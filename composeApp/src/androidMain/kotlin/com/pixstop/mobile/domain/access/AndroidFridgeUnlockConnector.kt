package com.pixstop.mobile.domain.access

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.pixstop.mobile.core.logging.AppLogger
import com.pixstop.mobile.domain.model.BluetoothUnlockResult
import com.pixstop.mobile.domain.model.DomainError
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.UnlockTicket
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

private const val TAG = "FridgeUnlock"

/**
 * A porta pelo rádio (Fase 9.7, B2).
 *
 * Três passos, cada um com prazo próprio: achar a geladeira pelo nome que ela
 * anuncia, conectar e descobrir o serviço, escrever o bilhete em pedaços e
 * esperar a resposta. A conexão morre logo depois — é a geladeira que a
 * encerra, e o app não segura nada.
 *
 * Os identificadores do serviço são os mesmos do `ble.py` do agente. Mudar um
 * lado sem o outro deixa a geladeira invisível para o app.
 */
class AndroidFridgeUnlockConnector(private val context: Context) : FridgeUnlockConnector {

    private val manager: BluetoothManager? =
        context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    private val adapter: BluetoothAdapter? get() = manager?.adapter

    override val isSupported: Boolean
        get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) && adapter != null

    /**
     * Perguntar se o rádio está ligado é uma chamada ao sistema, e ela lança
     * quando falta permissão — inclusive a antiga `BLUETOOTH`, que é de
     * instalação e não se pede em tempo de execução. Um botão de loja não
     * pode derrubar o aplicativo por causa disso.
     */
    private fun radioIsOn(): Boolean = runCatching { adapter?.isEnabled == true }.getOrDefault(false)

    /**
     * Do Android 12 em diante, escanear e conectar são permissões próprias;
     * antes disso, escanear exigia localização, porque um rádio por perto diz
     * onde a pessoa está.
     */
    override val missingPermissions: List<String>
        get() = requiredPermissions().filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

    private fun requiredPermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    override suspend fun unlock(ticket: UnlockTicket): Outcome<BluetoothUnlockResult> {
        if (!isSupported) {
            return Outcome.Failure(DomainError.Rule("ble_unsupported", "Este celular não tem Bluetooth para abrir a geladeira."))
        }

        if (missingPermissions.isNotEmpty()) {
            return Outcome.Failure(DomainError.Rule("ble_permission", "Permita o Bluetooth para abrir a geladeira."))
        }

        if (!radioIsOn()) {
            return Outcome.Failure(DomainError.Rule("ble_disabled", "Ligue o Bluetooth do celular para abrir a geladeira."))
        }

        val device = try {
            withTimeout(SCAN_TIMEOUT_MS) { scanFor(ticket.advertisedName) }
        } catch (_: TimeoutCancellationException) {
            return Outcome.Failure(
                DomainError.Offline("Não encontrei a geladeira por perto. Chegue mais perto dela e tente de novo."),
            )
        } catch (error: Throwable) {
            AppLogger.w("Busca falhou: ${error.message}", tag = TAG)

            return Outcome.Failure(DomainError.Rule("ble_scan", "Não consegui usar o Bluetooth do celular. Confira as permissões do aplicativo."))
        }

        return try {
            withTimeout(EXCHANGE_TIMEOUT_MS) { exchange(device, ticket) }
        } catch (_: TimeoutCancellationException) {
            Outcome.Failure(DomainError.Offline("A geladeira não respondeu. Tente de novo."))
        } catch (error: Throwable) {
            AppLogger.w("Troca falhou: ${error.message}", tag = TAG)

            Outcome.Failure(DomainError.Rule("ble_exchange", "A conversa com a geladeira falhou. Tente de novo."))
        }
    }

    /**
     * O primeiro anúncio que bate com o nome da geladeira do bilhete. Filtrar
     * pelo serviço no próprio sistema é o que evita acordar o app a cada
     * fone de ouvido da sala.
     */
    @SuppressLint("MissingPermission")
    private suspend fun scanFor(name: String): BluetoothDevice {
        val scanner = adapter?.bluetoothLeScanner ?: throw IllegalStateException("sem scanner")
        val found = CompletableDeferred<BluetoothDevice>()

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val advertised = result.scanRecord?.deviceName ?: result.device?.name

                if (advertised.equals(name, ignoreCase = true) && !found.isCompleted) {
                    AppLogger.i("Geladeira $name encontrada", tag = TAG)
                    found.complete(result.device)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                AppLogger.w("Busca falhou: $errorCode", tag = TAG)

                if (!found.isCompleted) {
                    found.completeExceptionally(IllegalStateException("scan $errorCode"))
                }
            }
        }

        scanner.startScan(listOf(filter), settings, callback)

        return try {
            found.await()
        } finally {
            runCatching { scanner.stopScan(callback) }
        }
    }

    /**
     * Conecta, escreve o bilhete e espera a resposta. A geladeira encerra a
     * conexão logo depois de responder; aqui a gente só solta o que sobrou.
     */
    @SuppressLint("MissingPermission")
    private suspend fun exchange(device: BluetoothDevice, ticket: UnlockTicket): Outcome<BluetoothUnlockResult> {
        val answer = CompletableDeferred<Outcome<BluetoothUnlockResult>>()
        var gatt: BluetoothGatt? = null

        // Os pedaços do bilhete, na ordem. O GATT do Android aceita uma
        // operação por vez: mandar todos de uma vez faz o sistema descartar
        // tudo depois do primeiro, e a geladeira fica esperando um bilhete
        // que nunca chega inteiro. Cada pedaço sai quando o anterior é
        // confirmado. Os pedaços saem do tamanho que o MTU combinado deixa:
        // com 18 bytes o bilhete leva ~20 idas e voltas (2,4 s medidos na
        // ESP32-S3); com o MTU maior, uma ou duas.
        val packets = ArrayDeque<ByteArray>()
        var chunkBytes = CHUNK_BYTES
        var discovering = false

        fun discover(client: BluetoothGatt) {
            if (!discovering) {
                discovering = true
                client.discoverServices()
            }
        }

        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(client: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    // Pede MTU maior antes de descobrir os serviços; se o pedido
                    // nem sai, segue com os pedaços pequenos de sempre.
                    BluetoothProfile.STATE_CONNECTED -> if (!client.requestMtu(REQUESTED_MTU)) discover(client)

                    BluetoothProfile.STATE_DISCONNECTED -> if (!answer.isCompleted) {
                        answer.complete(Outcome.Failure(DomainError.Offline("A geladeira encerrou a conexão antes de responder.")))
                    }
                }
            }

            override fun onMtuChanged(client: BluetoothGatt, mtu: Int, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // 3 bytes do cabeçalho do ATT e 2 do nosso (sequência e total).
                    chunkBytes = (mtu - 5).coerceIn(CHUNK_BYTES, MAX_CHUNK_BYTES)
                }

                discover(client)
            }

            override fun onServicesDiscovered(client: BluetoothGatt, status: Int) {
                packets.clear()
                packets.addAll(fragmentsOf(ticket.raw, chunkBytes))

                val service = client.getService(SERVICE_UUID)
                val result = service?.getCharacteristic(RESULT_UUID)

                if (service == null || result == null) {
                    answer.complete(Outcome.Failure(DomainError.Rule("ble_service", "Esta geladeira não sabe abrir por Bluetooth.")))

                    return
                }

                // A resposta vem por notificação; ligá-la antes de escrever é
                // o que evita perder um "abriu" que chega rápido demais.
                client.setCharacteristicNotification(result, true)

                val descriptor = result.getDescriptor(CLIENT_CONFIG_UUID)

                if (descriptor == null) {
                    sendNext(client)
                } else {
                    writeDescriptor(client, descriptor)
                }
            }

            override fun onDescriptorWrite(client: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                sendNext(client)
            }

            override fun onCharacteristicWrite(client: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    AppLogger.w("Pedaço recusado pelo GATT: $status", tag = TAG)
                    answer.complete(Outcome.Failure(DomainError.Offline("A geladeira recusou o bilhete pela metade. Tente de novo.")))

                    return
                }

                sendNext(client)
            }

            /** O próximo pedaço, ou nada quando o bilhete inteiro já saiu. */
            private fun sendNext(client: BluetoothGatt) {
                val characteristic = client.getService(SERVICE_UUID)?.getCharacteristic(TICKET_UUID)

                if (characteristic == null) {
                    answer.complete(Outcome.Failure(DomainError.Rule("ble_service", "Esta geladeira não sabe abrir por Bluetooth.")))

                    return
                }

                val packet = packets.removeFirstOrNull() ?: return

                writePacket(client, characteristic, packet)
            }

            override fun onCharacteristicChanged(client: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                complete(value)
            }

            @Deprecated("Android 12 e anteriores entregam o valor na própria característica")
            @Suppress("DEPRECATION")
            override fun onCharacteristicChanged(client: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                complete(characteristic.value ?: ByteArray(0))
            }

            private fun complete(value: ByteArray) {
                if (answer.isCompleted) {
                    return
                }

                answer.complete(Outcome.Success(readAnswer(value)))
            }
        }

        return try {
            gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
            answer.await()
        } finally {
            runCatching {
                gatt?.disconnect()
                gatt?.close()
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun writeDescriptor(client: BluetoothGatt, descriptor: BluetoothGattDescriptor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            client.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        } else {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            client.writeDescriptor(descriptor)
        }
    }

    /**
     * O bilhete em pedaços de `[sequência, total, bytes…]`.
     *
     * Fragmentar sempre continua: o pedaço só cresce até o MTU combinado, e
     * um celular que negocia mal cai nos 18 bytes que cabem no padrão de 23
     * em vez de virar um "não abriu" sem explicação.
     */
    private fun fragmentsOf(ticket: String, chunkBytes: Int): List<ByteArray> {
        val chunks = ticket.encodeToByteArray().toList().chunked(chunkBytes)

        return chunks.mapIndexed { index, chunk ->
            byteArrayOf(index.toByte(), chunks.size.toByte()) + chunk.toByteArray()
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun writePacket(client: BluetoothGatt, characteristic: BluetoothGattCharacteristic, packet: ByteArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            client.writeCharacteristic(characteristic, packet, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else {
            characteristic.value = packet
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            client.writeCharacteristic(characteristic)
        }
    }

    /** `{"ok":true}` ou `{"ok":false,"reason":"expired"}`. */
    private fun readAnswer(value: ByteArray): BluetoothUnlockResult {
        val body = runCatching { json.parseToJsonElement(value.decodeToString()) as? JsonObject }.getOrNull()
            ?: return BluetoothUnlockResult.Invalid

        val opened = runCatching { body["ok"]?.jsonPrimitive?.booleanOrNull }.getOrNull() == true

        if (opened) {
            return BluetoothUnlockResult.Opened
        }

        val reason = runCatching { body["reason"]?.jsonPrimitive?.content }.getOrNull()

        return BluetoothUnlockResult.from(reason ?: "invalid")
    }

    private companion object {
        /** Os mesmos identificadores do `ble.py` do agente. */
        val SERVICE_UUID: UUID = UUID.fromString("6f7a0001-5069-7873-746f-700000000001")
        val TICKET_UUID: UUID = UUID.fromString("6f7a0003-5069-7873-746f-700000000001")
        val RESULT_UUID: UUID = UUID.fromString("6f7a0004-5069-7873-746f-700000000001")

        /** O descritor padrão que liga as notificações de uma característica. */
        val CLIENT_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        /** Cabe no MTU padrão de 23 bytes, com os dois do cabeçalho. */
        const val CHUNK_BYTES = 18

        /** O MTU que o app pede; a geladeira aceita até 256. */
        const val REQUESTED_MTU = 247

        /** Um pedaço cabe num byte de tamanho no lado da geladeira. */
        const val MAX_CHUNK_BYTES = 240

        /** Achar a geladeira: perto da porta, é quase imediato. */
        const val SCAN_TIMEOUT_MS = 12_000L

        /** Conectar, escrever e ouvir a resposta. */
        const val EXCHANGE_TIMEOUT_MS = 15_000L

        val json = Json { ignoreUnknownKeys = true }
    }
}
