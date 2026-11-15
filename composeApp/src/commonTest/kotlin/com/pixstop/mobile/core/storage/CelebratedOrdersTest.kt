package com.pixstop.mobile.core.storage

import com.pixstop.mobile.support.InMemorySettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A festa toca uma vez por pedido, e só quando ele acabou de ser pago.
 */
class CelebratedOrdersTest {

    @Test
    fun `o mesmo pedido so e comemorado uma vez, mesmo depois de reabrir o app`() {
        val settings = InMemorySettings()

        assertTrue(CelebratedOrders(settings).claim(20))
        assertFalse(CelebratedOrders(settings).claim(20))
        assertTrue(CelebratedOrders(settings).claim(21))
    }

    @Test
    fun `so os pedidos recentes ficam guardados`() {
        val store = CelebratedOrders(InMemorySettings())

        (1L..(CelebratedOrders.KEEP + 5L)).forEach { store.claim(it) }

        assertTrue(store.claim(1))
        assertFalse(store.claim(CelebratedOrders.KEEP + 5L))
    }

    @Test
    fun `pedido antigo aberto pelo historico nao e novidade`() {
        val now = 1_800_000_000_000L

        assertTrue(CelebratedOrders.isFresh(now - 30_000, now))
        assertTrue(CelebratedOrders.isFresh(now + 120_000, now))
        assertFalse(CelebratedOrders.isFresh(now - 3_600_000, now))
        assertFalse(CelebratedOrders.isFresh(null, now))
    }
}
