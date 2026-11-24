package com.pixstop.mobile.domain.access

import com.pixstop.mobile.core.logging.AppLogger
import com.pixstop.mobile.domain.model.DomainError
import com.pixstop.mobile.domain.model.Outcome
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.NetworkExtension.NEHotspotConfiguration
import platform.NetworkExtension.NEHotspotConfigurationErrorAlreadyAssociated
import platform.NetworkExtension.NEHotspotConfigurationErrorDomain
import platform.NetworkExtension.NEHotspotConfigurationErrorUserDenied
import platform.NetworkExtension.NEHotspotConfigurationManager
import platform.NetworkExtension.NEHotspotNetwork
import kotlin.coroutines.resume

private const val TAG = "FridgeNetwork"

/**
 * Entra na rede da geladeira com `NEHotspotConfiguration`.
 *
 * Com `joinOnce` a rede vale só enquanto o app está na frente e nunca vira a
 * rede salva do iPhone: quando a pessoa sai do app ou a configuração é
 * removida em [leave], o celular volta sozinho à rede de casa. Como a
 * `Pixelstop-Setup` não tem internet, o iOS mantém a internet do resto pelo
 * 4G; o portal em `10.42.0.1` está na sub-rede da geladeira e sai pelo WiFi.
 *
 * O iOS pergunta "Entrar na rede?" na primeira vez — a tela avisa antes.
 *
 * Exige a capacidade **Hotspot Configuration** no `.entitlements`.
 */
class IosFridgeNetworkConnector : FridgeNetworkConnector {

    private val manager = NEHotspotConfigurationManager.sharedManager
    private var joinedSsid: String? = null

    override val canJoin: Boolean = true

    override suspend fun join(ssid: String, password: String): Outcome<Unit> {
        leave()

        val configuration = if (password.isEmpty()) {
            NEHotspotConfiguration(sSID = ssid)
        } else {
            NEHotspotConfiguration(sSID = ssid, passphrase = password, isWEP = false)
        }
        configuration.joinOnce = true

        val error = suspendCancellableCoroutine { continuation ->
            manager.applyConfiguration(configuration) { error: NSError? ->
                if (continuation.isActive) continuation.resume(error)
            }
        }

        if (error != null && !(error.domain == NEHotspotConfigurationErrorDomain && error.code == NEHotspotConfigurationErrorAlreadyAssociated)) {
            AppLogger.w("Rede $ssid recusada: ${error.domain} ${error.code} ${error.localizedDescription}", tag = TAG)

            val message = if (error.domain == NEHotspotConfigurationErrorDomain && error.code == NEHotspotConfigurationErrorUserDenied) {
                "Para configurar a geladeira, o celular precisa entrar na rede $ssid. Toque em Conectar de novo e escolha Entrar."
            } else {
                "Não consegui entrar na rede $ssid. A geladeira está ligada há mais de um minuto?"
            }

            return Outcome.Failure(DomainError.Offline(message))
        }

        joinedSsid = ssid

        // O `applyConfiguration` às vezes responde sem erro e o iPhone não
        // entra (rede fora de alcance). Conferir a rede atual separa os casos,
        // dando à associação o tempo que ela leva.
        var current = currentSsid()
        repeat(ASSOCIATION_CHECKS) {
            if (current == null || current == ssid) return@repeat
            delay(ASSOCIATION_INTERVAL_MS)
            current = currentSsid()
        }

        if (current != null && current != ssid) {
            AppLogger.w("Pedi $ssid e o iPhone está em $current", tag = TAG)
            leave()

            return Outcome.Failure(DomainError.Offline("Não encontrei a rede $ssid. A geladeira está ligada há mais de um minuto?"))
        }

        AppLogger.i("Na rede $ssid", tag = TAG)

        return Outcome.Success(Unit)
    }

    override fun leave() {
        joinedSsid?.let { ssid ->
            manager.removeConfigurationForSSID(ssid)
            AppLogger.i("Saí da rede $ssid", tag = TAG)
        }
        joinedSsid = null
    }

    /**
     * A rede em que o iPhone está. O app só a enxerga porque foi ele quem a
     * configurou; sem isso, ler o nome da rede pediria a localização.
     */
    private suspend fun currentSsid(): String? = suspendCancellableCoroutine { continuation ->
        NEHotspotNetwork.fetchCurrentWithCompletionHandler { network: NEHotspotNetwork? ->
            if (continuation.isActive) continuation.resume(network?.SSID)
        }
    }

    private companion object {
        const val ASSOCIATION_CHECKS = 8
        const val ASSOCIATION_INTERVAL_MS = 1_000L
    }
}
