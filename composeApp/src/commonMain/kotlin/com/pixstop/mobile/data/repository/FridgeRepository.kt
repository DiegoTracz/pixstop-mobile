package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.data.remote.dto.FridgeDeviceDto
import com.pixstop.mobile.data.remote.dto.IrProfileDto
import com.pixstop.mobile.data.remote.dto.LedSettingsDto
import com.pixstop.mobile.data.remote.dto.PressKeyRequest
import com.pixstop.mobile.data.remote.dto.SaveLedRequest
import com.pixstop.mobile.data.remote.dto.StoreFridgeRequest
import com.pixstop.mobile.domain.model.FridgeDevice
import com.pixstop.mobile.domain.model.FridgePresence
import com.pixstop.mobile.domain.model.FridgeStock
import com.pixstop.mobile.domain.model.IrProfile
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
}

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
