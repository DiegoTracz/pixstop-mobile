package com.pixstop.mobile.domain.model

/**
 * A cor que a fita LED da geladeira está mostrando (Fase 9.6).
 *
 * A geladeira fala por cor a três metros de distância: vermelho é sem rede,
 * verde é liberada, branco é porta aberta, ciano é esperando o celular. Trazer
 * a mesma cor para a tela faz as duas falarem a mesma língua — quem olha o
 * celular e levanta os olhos para a geladeira vê a mesma coisa.
 *
 * Os nomes são os do `LedColor` do servidor; a fita é quem sabe qual número
 * infravermelho corresponde a cada um.
 */
enum class FridgeLight {
    Off,
    White,
    Red,
    Green,
    Blue,
    Yellow,
    Cyan,
    Magenta,

    /** Os dois arco-íris do controle: cor de descanso que muda sozinha. */
    Fade,
    Smooth;

    companion object {
        fun from(value: String?): FridgeLight = when (value) {
            "white" -> White
            "red" -> Red
            "green" -> Green
            "blue" -> Blue
            "yellow" -> Yellow
            "cyan" -> Cyan
            "magenta" -> Magenta
            "fade" -> Fade
            "smooth" -> Smooth
            "off" -> Off
            else -> Off
        }

        /**
         * A cor da fita para o que está acontecendo, na mesma ordem de
         * prioridade da máquina de estados do agente: sem rede vence tudo,
         * depois a espera do celular, depois a liberação, depois a porta
         * aberta. Nada acontecendo é a cor de repouso que a empresa escolheu.
         */
        fun of(
            pickup: Pickup,
            resting: FridgeLight,
            talkingByBluetooth: Boolean = false,
        ): FridgeLight = when {
            pickup.reason == PickupReason.Offline -> Red
            talkingByBluetooth -> Cyan
            pickup.status == PickupStatus.Unlocking -> Green
            pickup.status == PickupStatus.Opened -> White
            else -> resting
        }

        /** A cor da fita para o que se sabe da geladeira no pagamento. */
        fun ofPresence(presence: Presence, resting: FridgeLight): FridgeLight = when (presence) {
            Presence.Offline, Presence.Unreachable -> Red
            else -> resting
        }
    }
}

/**
 * O que se sabe da geladeira na hora de pagar (Fase 9.7).
 */
enum class Presence {
    Online,
    Unreachable,
    Offline,
    Unknown;

    companion object {
        fun from(value: String?): Presence = when (value) {
            "online" -> Online
            "unreachable" -> Unreachable
            "offline" -> Offline
            else -> Unknown
        }
    }
}

/**
 * A geladeira desta compra, do ponto de vista do pagamento (Fase 9.7).
 */
data class ApplianceStatus(
    val presence: Presence,
    val label: String,
    val applianceId: Long?,
    val bleAvailable: Boolean,
    val supportPhone: String?,
    val restingLight: FridgeLight,
) {
    /** A cor que a fita está mostrando agora, do que o app sabe. */
    val light: FridgeLight get() = FridgeLight.ofPresence(presence, restingLight)

    /** A geladeira não vai abrir pela internet, e não há outro caminho. */
    val blocksPayment: Boolean
        get() = (presence == Presence.Offline || presence == Presence.Unreachable) && !bleAvailable

    companion object {
        val Unknown = ApplianceStatus(Presence.Unknown, "", null, false, null, FridgeLight.Off)
    }
}
