package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Uma geladeira como o servidor a apresenta (Fase 9.3). Sem token, nunca.
 */
@Serializable
data class FridgeDeviceDto(
    val id: Long,
    val name: String,
    val type: String = "gateway",
    val presence: String? = null,
    @SerialName("provisioning_status") val provisioningStatus: String? = null,
    @SerialName("activation_code") val activationCode: String? = null,
    @SerialName("activation_expires_at") val activationExpiresAt: String? = null,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("signal_strength") val signalStrength: Int? = null,
    @SerialName("firmware_version") val firmwareVersion: String? = null,
    @SerialName("door_open") val doorOpen: Boolean? = null,
    val appliance: NamedRefDto? = null,
    val stock: FridgeStockDto? = null,
)

/** O que ainda falta para a geladeira vender (Fase 9.3). */
@Serializable
data class FridgeStockDto(
    val appliance: NamedRefDto? = null,
    @SerialName("in_appliance") val inAppliance: Int = 0,
    @SerialName("in_company") val inCompany: Int = 0,
)

@Serializable
data class NamedRefDto(val id: Long, val name: String)

@Serializable
data class StoreFridgeRequest(
    val name: String,
    val type: String = "gateway",
    @SerialName("pulse_duration_ms") val pulseDurationMs: Int = 500,
)

/** O que o portal da geladeira responde em `/api/status`. */
@Serializable
data class PortalStatusDto(
    val mode: String = "setup",
    val message: String = "",
    @SerialName("agent_version") val agentVersion: String? = null,
)

@Serializable
data class PortalNetworkDto(val ssid: String, val signal: Int = 0, val secured: Boolean = true)

@Serializable
data class PortalConfigureRequest(val ssid: String, val password: String, val code: String)

@Serializable
data class PortalMessageDto(val message: String = "")
