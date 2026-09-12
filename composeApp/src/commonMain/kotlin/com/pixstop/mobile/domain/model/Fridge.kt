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
    /** O endereço dela na rede da empresa: serve ao suporte, não ao app. */
    val localIp: String? = null,
    /** `picamera`, `usb`, `none` — o que ela tem de olho. */
    val cameraKind: String? = null,
    /** "Ativa", "Aguardando ativação", "Revogada": o texto do servidor. */
    val provisioningLabel: String? = null,
    val stock: FridgeStock? = null,
) {
    val isOnline: Boolean get() = presence == FridgePresence.Online

    /** O código só serve enquanto o servidor ainda o mostra. */
    val hasCode: Boolean get() = !activationCode.isNullOrBlank()

    /** `483 921`, que é como se dita e como aparece no painel. */
    val formattedCode: String? get() = activationCode?.let { "${it.take(3)} ${it.drop(3)}" }

    /** O que a geladeira está fazendo com a porta, em uma palavra. */
    val doorLabel: String get() = when (doorOpen) {
        true -> "Aberta"
        false -> "Fechada"
        null -> "Sem sensor"
    }

    /**
     * A força do sinal em palavras. Abaixo de -75 dBm a geladeira ainda
     * conecta, mas é ali que as quedas começam — e é a informação que faz
     * alguém mover o roteador antes de abrir um chamado.
     */
    val signalLabel: String? get() = signalStrength?.let { rssi ->
        val quality = when {
            rssi >= -60 -> "bom"
            rssi >= -75 -> "razoável"
            else -> "fraco"
        }

        "$rssi dBm ($quality)"
    }

    val cameraLabel: String get() = when (cameraKind) {
        "picamera" -> "Módulo oficial"
        "usb" -> "Webcam USB"
        "fake" -> "Simulada"
        else -> "Sem câmera"
    }
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
