package com.pixstop.mobile.domain.model

/**
 * Onde a geladeira está no caminho até ficar operacional (Fase 9.3).
 */
enum class FridgePresence(val label: String) {
    /** Criada no painel; o código ainda não foi trocado. */
    Pending("Aguardando ativação"),
    Online("Online"),
    Offline("Offline"),
    Revoked("Revogada"),
    ;

    companion object {
        fun fromApi(value: String?): FridgePresence = when (value) {
            "online" -> Online
            "offline" -> Offline
            "revoked" -> Revoked
            else -> Pending
        }
    }
}

/**
 * Uma geladeira da empresa, como o servidor a vê.
 *
 * Nunca traz o token: o cliente conhece só o código de ativação, e o
 * servidor só o entrega enquanto vale.
 */
data class FridgeDevice(
    val id: Long,
    val name: String,
    val type: String,
    val presence: FridgePresence,
    val activationCode: String?,
    val activationExpiresAt: String?,
    val lastSeenAt: String?,
    val signalStrength: Int?,
    val firmwareVersion: String?,
    val doorOpen: Boolean?,
    val applianceName: String?,
) {
    val isOnline: Boolean get() = presence == FridgePresence.Online

    /** O código só serve enquanto o servidor ainda o mostra. */
    val hasCode: Boolean get() = !activationCode.isNullOrBlank()

    /** `483 921`, que é como se dita e como aparece no painel. */
    val formattedCode: String? get() = activationCode?.let { "${it.take(3)} ${it.drop(3)}" }
}

/** Uma rede WiFi vista pela geladeira, no modo de configuração. */
data class WifiNetwork(val ssid: String, val signal: Int, val secured: Boolean)

/**
 * O que o portal da geladeira responde em `/api/status`.
 */
data class PortalStatus(val mode: PortalMode, val message: String, val agentVersion: String?)

enum class PortalMode {
    Setup,
    Connecting,
    Provisioning,
    Done,
    Error,
    ;

    companion object {
        fun fromApi(value: String?): PortalMode = when (value) {
            "connecting" -> Connecting
            "provisioning" -> Provisioning
            "done" -> Done
            "error" -> Error
            else -> Setup
        }
    }
}
