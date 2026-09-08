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
    val stock: FridgeStock? = null,
) {
    val isOnline: Boolean get() = presence == FridgePresence.Online

    /** O código só serve enquanto o servidor ainda o mostra. */
    val hasCode: Boolean get() = !activationCode.isNullOrBlank()

    /** `483 921`, que é como se dita e como aparece no painel. */
    val formattedCode: String? get() = activationCode?.let { "${it.take(3)} ${it.drop(3)}" }
}

/**
 * O estoque desta geladeira, para a tela dizer o que ainda falta.
 *
 * Ligar na tomada e entrar no WiFi não vende nada: sem produto cadastrado e
 * sem estoque aqui dentro, a vitrine continua vazia. Esta é a única coisa
 * que a pessoa precisa saber assim que a geladeira fica online.
 */
data class FridgeStock(
    val applianceName: String?,
    val inAppliance: Int,
    val inCompany: Int,
) {
    /** O que dizer, na ordem em que os problemas aparecem. */
    val nextStep: String
        get() = when {
            inCompany == 0 ->
                "Falta cadastrar os produtos da empresa. Depois é só informar o estoque desta geladeira."

            applianceName == null ->
                "Falta ligar esta geladeira a um equipamento no painel para ela receber estoque."

            inAppliance == 0 ->
                "Os produtos já estão cadastrados. Falta informar o estoque de $applianceName para a vitrine encher."

            inAppliance == 1 ->
                "1 produto já está em $applianceName. Pedidos pagos abrem a trava, e toda abertura fica gravada."

            else ->
                "$inAppliance produtos já estão em $applianceName. Pedidos pagos abrem a trava, e toda abertura fica gravada."
        }
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
