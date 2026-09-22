package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.model.Cart
import com.pixstop.mobile.domain.model.CartLine
import com.pixstop.mobile.domain.model.Product
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * O contador tem de olhar a reserva mais curta: é ela que decide quando o
 * carrinho começa a encolher. Contar pela mais longa daria uma folga que não
 * existe, e a pessoa perderia o item achando que ainda tinha tempo.
 */
class CartUiStateTest {

    private val produto = Product(
        id = 1,
        name = "Água Mineral 500ml",
        description = null,
        imageUrl = null,
        categoryName = "Bebidas",
        priceMoney = 3.0,
        priceMoneyDiscounted = 3.0,
        pricePixels = 300,
        pricePixelsDiscounted = 300,
        discountPercentage = 0,
        available = 49,
    )

    private fun linha(id: Long, reservedUntil: Long?) = CartLine(
        id = id,
        quantity = 1,
        unitPrice = 3.0,
        subtotal = 3.0,
        reservedUntil = reservedUntil,
        product = produto,
    )

    private fun estado(vararg reservas: Long?, now: Long) = CartUiState(
        cart = Cart(
            items = reservas.mapIndexed { index, until -> linha(index.toLong(), until) },
            totalItems = reservas.size,
            total = 3.0 * reservas.size,
            reservationMinutes = 5,
        ),
        now = now,
    )

    @Test
    fun `conta pela reserva mais curta`() {
        val state = estado(200_000L, 100_000L, 300_000L, now = 40_000L)

        assertEquals(60L, state.secondsLeft)
    }

    @Test
    fun `carrinho vazio nao tem contador`() {
        assertNull(CartUiState().secondsLeft)
        assertFalse(CartUiState().isExpired)
    }

    @Test
    fun `reserva vencida para em zero em vez de ficar negativa`() {
        val state = estado(100_000L, now = 160_000L)

        assertEquals(0L, state.secondsLeft)
        assertTrue(state.isExpired)
    }

    @Test
    fun `item sem prazo nao encurta o contador`() {
        val state = estado(null, 120_000L, now = 60_000L)

        assertEquals(60L, state.secondsLeft)
    }

    @Test
    fun `relogio adiantado nao promete mais tempo do que a reserva dura`() {
        // Cinco minutos de reserva, mas o aparelho acha que faltam seis.
        val state = estado(360_000L, now = 0L)

        assertEquals(300L, state.secondsLeft)
    }

    @Test
    fun `a contagem de itens vem do servidor nao do tamanho da lista`() {
        val state = CartUiState(
            cart = Cart(items = listOf(linha(1, null)), totalItems = 3, total = 9.0, reservationMinutes = 5),
        )

        assertEquals(3, state.itemCount)
    }

    @Test
    fun `o aviso de adicionado diz quantas unidades daquele produto ja estao no carrinho`() {
        val cart = Cart(
            items = listOf(linha(7, null).copy(quantity = 2)),
            totalItems = 2,
            total = 6.0,
            reservationMinutes = 5,
        )

        assertEquals("2 × Água Mineral 500ml", addedNotice(cart, productId = 1))
    }

    @Test
    fun `sem o item no carrinho o aviso volta a frase de sempre`() {
        assertEquals("Adicionado ao carrinho.", addedNotice(Cart.Empty, productId = 1))
    }
}
