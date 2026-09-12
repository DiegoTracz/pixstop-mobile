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
    @SerialName("local_ip") val localIp: String? = null,
    @SerialName("camera_kind") val cameraKind: String? = null,
    @SerialName("provisioning_label") val provisioningLabel: String? = null,
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

/** A fita LED desta geladeira, como o servidor a apresenta (Fase 9.10). */
@Serializable
data class LedSettingsDto(
    val color: String = "fade",
    @SerialName("ir_profile_id") val irProfileId: Long? = null,
    val online: Boolean = false,
    val colors: List<LedColorOptionDto> = emptyList(),
    val profiles: List<IrProfileDto> = emptyList(),
)

@Serializable
data class LedColorOptionDto(val value: String, val label: String, val swatch: String)

/** Um modelo de controle do catálogo, com as teclas que alguém mapeou. */
@Serializable
data class IrProfileDto(val id: Long, val name: String, val keys: List<String> = emptyList())

/** O que o app manda para salvar a fita: o nome da cor, nunca o código. */
@Serializable
data class SaveLedRequest(
    val color: String,
    @SerialName("ir_profile_id") val irProfileId: Long?,
)

/** Uma tecla apertada no celular. */
@Serializable
data class PressKeyRequest(val command: String)


/** A janela da câmera, aberta pelo app (Fase 9.10, etapa D). */
@Serializable
data class LiveWindowDto(
    @SerialName("live_until") val liveUntil: String? = null,
    @SerialName("window_seconds") val windowSeconds: Int = 30,
    @SerialName("has_frame") val hasFrame: Boolean = false,
    @SerialName("frame_age_seconds") val frameAgeSeconds: Long? = null,
)

/** Uma abertura da porta, resumida (Fase 9.10, etapa C). */
@Serializable
data class DoorSessionDto(
    val id: String,
    @SerialName("opened_at") val openedAt: String,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    @SerialName("is_open") val isOpen: Boolean = false,
    @SerialName("has_order") val hasOrder: Boolean = false,
    @SerialName("order_transaction_id") val orderTransactionId: String? = null,
    @SerialName("has_cover") val hasCover: Boolean = false,
    val verdict: String? = null,
    @SerialName("verdict_label") val verdictLabel: String? = null,
    @SerialName("is_flag") val isFlag: Boolean = false,
    val observations: String? = null,
)

/** O que o pulso devolve: o número do comando que a geladeira vai executar. */
@Serializable
data class UnlockResultDto(
    @SerialName("command_id") val commandId: Int = 0,
    @SerialName("pulse_ms") val pulseMs: Int = 400,
)
