package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeBytes
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.data.remote.dto.FridgeDeviceDto
import com.pixstop.mobile.data.remote.dto.DoorSessionDto
import com.pixstop.mobile.data.remote.dto.IrProfileDto
import com.pixstop.mobile.data.remote.dto.LiveWindowDto
import com.pixstop.mobile.data.remote.dto.LedSettingsDto
import com.pixstop.mobile.data.remote.dto.PressKeyRequest
import com.pixstop.mobile.data.remote.dto.SaveLedRequest
import com.pixstop.mobile.data.remote.dto.UnlockResultDto
import com.pixstop.mobile.data.remote.dto.StoreFridgeRequest
import com.pixstop.mobile.domain.model.FridgeDevice
import com.pixstop.mobile.domain.model.FridgePresence
import com.pixstop.mobile.domain.model.FridgeStock
import com.pixstop.mobile.domain.model.DoorSessionSummary
import com.pixstop.mobile.domain.model.IrProfile
import com.pixstop.mobile.domain.model.LiveWindow
import com.pixstop.mobile.domain.model.LedColorOption
import com.pixstop.mobile.domain.model.LedSettings
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.map
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

private const val TAG = "Fridge"

/**
 * As geladeiras da empresa no servidor (Fase 9.3).
 *
 * Só o administrador chega aqui; quem decide é o middleware. O token do
 * aparelho nunca vem nestas respostas: o app conhece o código de ativação,
 * e é a geladeira quem o troca pelas credenciais, direto com o servidor.
 */
class FridgeRepository(private val client: HttpClient) {

    suspend fun devices(): Outcome<List<FridgeDevice>> =
        safeCall<List<FridgeDeviceDto>>(TAG) {
            client.get(ApiConfig.Endpoints.COMPANY_DEVICES)
        }.map { list -> list.map { it.toDomain() } }

    /** Cria a geladeira e já volta com o código de ativação. */
    suspend fun create(name: String): Outcome<FridgeDevice> =
        safeCall<FridgeDeviceDto>(TAG) {
            client.post(ApiConfig.Endpoints.COMPANY_DEVICES) {
                setBody(StoreFridgeRequest(name = name.trim()))
            }
        }.map { it.toDomain() }

    /** Outro código; o anterior morre junto. */
    suspend fun issueCode(deviceId: Long): Outcome<FridgeDevice> =
        safeCall<FridgeDeviceDto>(TAG) {
            client.post(ApiConfig.Endpoints.companyDeviceCode(deviceId))
        }.map { it.toDomain() }

    /** O estado ao vivo, para esperar a geladeira ficar online. */
    suspend fun status(deviceId: Long): Outcome<FridgeDevice> =
        safeCall<FridgeDeviceDto>(TAG) {
            client.get(ApiConfig.Endpoints.companyDeviceStatus(deviceId))
        }.map { it.toDomain() }

    /** A fita: a cor de agora, o catálogo de controles e as teclas de cada um. */
    suspend fun ledSettings(deviceId: Long): Outcome<LedSettings> =
        safeCall<LedSettingsDto>(TAG) {
            client.get(ApiConfig.Endpoints.companyDeviceLed(deviceId))
        }.map { it.toDomain() }

    /**
     * Aperta uma tecla do controle. O app manda o nome; quem sabe o código
     * infravermelho é o servidor, com o perfil desta geladeira.
     */
    suspend fun pressKey(deviceId: Long, command: String): Outcome<Unit> =
        safeCall<Unit>(TAG) {
            client.post(ApiConfig.Endpoints.companyDeviceIrSend(deviceId)) {
                setBody(PressKeyRequest(command))
            }
        }

    /** Salva o que ficou aceso como a cor de repouso desta geladeira. */
    suspend fun saveLed(deviceId: Long, color: String, profileId: Long?): Outcome<LedSettings> =
        safeCall<LedSettingsDto>(TAG) {
            client.put(ApiConfig.Endpoints.companyDeviceLed(deviceId)) {
                setBody(SaveLedRequest(color = color, irProfileId = profileId))
            }
        }.map { it.toDomain() }

    /**
     * Abre a geladeira como admin (Fase 9.10, etapa B).
     *
     * Não é a abertura de uma compra: esta serve para conferir se a trava
     * responde, e fica registrada com o nome de quem apertou.
     */
    suspend fun unlock(deviceId: Long): Outcome<Unit> =
        safeCall<UnlockResultDto>(TAG) {
            client.post(ApiConfig.Endpoints.companyDeviceUnlock(deviceId))
        }.map { }

    /** Abre a janela da câmera: enquanto ela durar, a geladeira manda quadros. */
    suspend fun openLive(deviceId: Long): Outcome<LiveWindow> =
        safeCall<LiveWindowDto>(TAG) {
            client.post(ApiConfig.Endpoints.companyDeviceLive(deviceId))
        }.map { LiveWindow(windowSeconds = it.windowSeconds, hasFrame = it.hasFrame, frameAgeSeconds = it.frameAgeSeconds) }

    /**
     * O último quadro que a geladeira mandou, em bytes.
     *
     * Vem pelo cliente autenticado, e não por uma URL solta: a imagem da
     * geladeira de uma empresa não pode ser aberta por quem tem o endereço.
     */
    suspend fun frame(deviceId: Long): Outcome<ByteArray> =
        safeBytes(TAG) {
            client.get(ApiConfig.Endpoints.companyDeviceFrame(deviceId))
        }

    /** As últimas aberturas da porta desta geladeira. */
    suspend fun sessions(applianceId: Long): Outcome<List<DoorSessionSummary>> =
        safeCall<List<DoorSessionDto>>(TAG) {
            client.get(ApiConfig.Endpoints.companyApplianceSessions(applianceId))
        }.map { list -> list.map { it.toDomain() } }
}

private fun DoorSessionDto.toDomain() = DoorSessionSummary(
    id = id,
    openedAt = openedAt,
    durationSeconds = durationSeconds,
    isOpen = isOpen,
    hasOrder = hasOrder,
    orderTransactionId = orderTransactionId,
    hasCover = hasCover,
    verdictLabel = verdictLabel,
    isFlag = isFlag,
    observations = observations,
)

private fun LedSettingsDto.toDomain() = LedSettings(
    color = color,
    profileId = irProfileId,
    online = online,
    colors = colors.map { LedColorOption(value = it.value, label = it.label, swatch = it.swatch) },
    profiles = profiles.map(IrProfileDto::toDomain),
)

private fun IrProfileDto.toDomain() = IrProfile(id = id, name = name, keys = keys)

private fun FridgeDeviceDto.toDomain() = FridgeDevice(
    id = id,
    name = name,
    type = type,
    presence = FridgePresence.fromApi(presence),
    activationCode = activationCode,
    activationExpiresAt = activationExpiresAt,
    lastSeenAt = lastSeenAt,
    signalStrength = signalStrength,
    firmwareVersion = firmwareVersion,
    doorOpen = doorOpen,
    applianceName = appliance?.name,
    applianceId = appliance?.id,
    localIp = localIp,
    cameraKind = cameraKind,
    provisioningLabel = provisioningLabel,
    stock = stock?.let {
        FridgeStock(
            applianceName = it.appliance?.name ?: appliance?.name,
            inAppliance = it.inAppliance,
            inCompany = it.inCompany,
        )
    },
)
