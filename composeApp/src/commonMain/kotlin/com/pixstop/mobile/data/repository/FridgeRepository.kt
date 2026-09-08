package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.data.remote.dto.FridgeDeviceDto
import com.pixstop.mobile.data.remote.dto.StoreFridgeRequest
import com.pixstop.mobile.domain.model.FridgeDevice
import com.pixstop.mobile.domain.model.FridgePresence
import com.pixstop.mobile.domain.model.FridgeStock
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.map
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
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
}

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
    stock = stock?.let {
        FridgeStock(
            applianceName = it.appliance?.name ?: appliance?.name,
            inAppliance = it.inAppliance,
            inCompany = it.inCompany,
        )
    },
)
