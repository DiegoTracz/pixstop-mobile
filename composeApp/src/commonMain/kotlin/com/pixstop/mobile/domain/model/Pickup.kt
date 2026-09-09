package com.pixstop.mobile.domain.model

/**
 * A retirada do pedido (Fase 9.8).
 *
 * O pagamento reserva; abrir a geladeira é uma tentativa, e o sensor da porta
 * diz se ela foi usada. Este bloco é o que a tela do pedido lê para saber o
 * que mostrar e se o botão "Abrir a geladeira" aparece.
 */
data class Pickup(
    val status: PickupStatus,
    val canUnlock: Boolean,
    val reason: PickupReason?,
    val attempts: Int,
    val maxAttempts: Int,
    /** Até quando a porta responde a esta tentativa. */
    val windowUntil: Long?,
    val doorOpenedAt: Long?,
    val pickedUpAt: Long?,
    /** Há bilhete válido: dá para abrir por Bluetooth mesmo sem internet. */
    val hasTicket: Boolean,
    /** A cor de repouso da fita LED desta geladeira (Fase 9.6). */
    val restingLight: FridgeLight = FridgeLight.Off,
) {
    /** A tentativa está de pé: é a hora de puxar a porta. */
    val isUnlocking: Boolean get() = status == PickupStatus.Unlocking

    /** Acabou: a porta abriu ou o produto já saiu. */
    val isDone: Boolean get() = status == PickupStatus.Opened || status == PickupStatus.PickedUp

    /** Vale acompanhar o servidor: algo ainda pode mudar sozinho. */
    val isFollowing: Boolean get() = status == PickupStatus.Awaiting || status == PickupStatus.Unlocking

    companion object {
        /** Um pedido que ainda não é do mundo físico: nada a mostrar. */
        val None = Pickup(PickupStatus.None, false, null, 0, 0, null, null, null, false)
    }
}

/**
 * Em que pé está a retirada.
 *
 * Um valor desconhecido cai em `None`, que é o único que não promete porta
 * nenhuma a quem está esperando.
 */
enum class PickupStatus {
    /** Pago, esperando a pessoa chegar na frente da geladeira e tocar. */
    Awaiting,

    /** Pulso enviado: a porta responde por alguns segundos. */
    Unlocking,

    /** A porta abriu para este pedido. */
    Opened,

    /** Abriu e fechou: o produto saiu. */
    PickedUp,

    /** A empresa não tem geladeira conectada; a retirada é com o responsável. */
    Manual,

    None;

    companion object {
        fun from(value: String?): PickupStatus = when (value) {
            "awaiting" -> Awaiting
            "unlocking" -> Unlocking
            "opened" -> Opened
            "picked_up" -> PickedUp
            "manual" -> Manual
            else -> None
        }
    }
}

/** Por que não dá para abrir agora. */
enum class PickupReason {
    /** Outra pessoa está usando a geladeira. */
    Busy,

    /** A geladeira está sem internet. */
    Offline,

    /** As tentativas acabaram. */
    Exhausted;

    companion object {
        fun from(value: String?): PickupReason? = when (value) {
            "busy" -> Busy
            "offline" -> Offline
            "exhausted" -> Exhausted
            else -> null
        }
    }
}
