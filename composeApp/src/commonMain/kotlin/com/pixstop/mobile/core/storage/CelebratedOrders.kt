package com.pixstop.mobile.core.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

/**
 * Os pedidos que já foram comemorados neste aparelho.
 *
 * A animação de recompensa toca uma vez por pedido: reabrir o pedido pelo
 * histórico, ou a tela recarregar, não repete a festa. Só os mais recentes
 * ficam guardados — um pedido antigo já não é comemorado de qualquer jeito.
 */
class CelebratedOrders(private val settings: Settings) {

    /** Marca o pedido e diz se esta é a primeira vez. */
    fun claim(orderId: Long): Boolean {
        val seen = seen()

        if (orderId in seen) {
            return false
        }

        settings[KEY] = (seen + orderId).takeLast(KEEP).joinToString(",")

        return true
    }

    private fun seen(): List<Long> =
        settings.getStringOrNull(KEY).orEmpty().split(',').mapNotNull { it.trim().toLongOrNull() }

    companion object {
        private const val KEY = "celebrated_orders"
        const val KEEP = 40

        /** Quanto tempo depois de pago um pedido ainda é novidade. */
        const val FRESH_MILLIS = 10 * 60 * 1000L

        /**
         * O pedido acabou de ser pago? A janela é larga de propósito: o
         * relógio do aparelho não é o do servidor.
         */
        fun isFresh(paidAtMillis: Long?, nowMillis: Long): Boolean {
            val paidAt = paidAtMillis ?: return false
            val age = nowMillis - paidAt

            return age > -FRESH_MILLIS && age < FRESH_MILLIS
        }
    }
}
