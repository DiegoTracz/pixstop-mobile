package com.pixstop.mobile.domain.model

/**
 * O desfecho de uma tentativa de abrir (Fase 9.8), com o estado que veio
 * junto: recusado ou não, o servidor sempre diz em que pé a retirada ficou.
 */
data class UnlockAttempt(
    val pickup: Pickup,
    val ticket: UnlockTicket?,
)

/**
 * O que a geladeira respondeu ao bilhete entregue por Bluetooth (Fase 9.7).
 *
 * O motivo vem da própria geladeira, e cada um pede uma conversa diferente
 * com quem está na frente da porta.
 */
enum class BluetoothUnlockResult {
    /** A porta abriu. */
    Opened,

    /** O bilhete venceu: dá para pedir outro ao servidor. */
    Expired,

    /** Esta compra já abriu a porta. */
    AlreadyUsed,

    /** É de outra geladeira, ou a assinatura não confere. */
    Invalid,

    /** Esta geladeira não confere bilhetes (sem chave ou sem a biblioteca). */
    Unsupported;

    companion object {
        /**
         * Os motivos que o `ble.py` devolve. Nonce e repetição são a mesma
         * conversa: aquela compra já foi.
         */
        fun from(reason: String?): BluetoothUnlockResult = when (reason) {
            null -> Opened
            "expired" -> Expired
            "nonce", "replayed" -> AlreadyUsed
            "unavailable" -> Unsupported
            else -> Invalid
        }
    }
}
