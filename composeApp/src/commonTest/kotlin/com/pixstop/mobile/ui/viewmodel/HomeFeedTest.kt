package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.model.Order
import com.pixstop.mobile.domain.model.OrderLine
import com.pixstop.mobile.domain.model.OrderStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Numa despensa de empresa quase toda compra é repetição — a mesma água, o
 * mesmo café. A prateleira de "comprar de novo" é o atalho para isso, e o que
 * decide o que entra nela é esta regra.
 */
class BuyAgainTest {

    private fun pedido(vararg productIds: Long?) = Order(
        id = productIds.size.toLong(),
        transactionId = null,
        status = OrderStatus.Paid,
        statusLabel = "Pago",
        paymentMethodLabel = null,
        productsTotal = 0.0,
        pixels = 0,
        balance = 0.0,
        money = 0.0,
        cardFee = 0.0,
        items = productIds.mapIndexed { index, id ->
            OrderLine(
                id = index.toLong(),
                productId = id,
                productName = "Produto $id",
                quantity = 1,
                unitPrice = 0.0,
                subtotal = 0.0,
            )
        },
        pix = null,
        cancellationReason = null,
        isCancelable = false,
        createdAt = null,
    )

    @Test
    fun `mantem a ordem dos pedidos do mais recente ao mais antigo`() {
        val ids = buyAgainIdsFrom(listOf(pedido(3), pedido(1), pedido(2)))

        assertEquals(listOf(3L, 1L, 2L), ids)
    }

    @Test
    fun `produto repetido conta uma vez so`() {
        val ids = buyAgainIdsFrom(listOf(pedido(3, 3), pedido(3), pedido(1)))

        assertEquals(listOf(3L, 1L), ids)
    }

    @Test
    fun `item sem produto e ignorado`() {
        // Pedido antigo cujo produto já saiu do catálogo.
        val ids = buyAgainIdsFrom(listOf(pedido(null, 5)))

        assertEquals(listOf(5L), ids)
    }

    @Test
    fun `respeita o limite da prateleira`() {
        val ids = buyAgainIdsFrom(listOf(pedido(1, 2, 3, 4, 5, 6, 7, 8)), limit = 6)

        assertEquals(6, ids.size)
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L, 6L), ids)
    }

    @Test
    fun `sem pedidos nao ha o que repetir`() {
        assertTrue(buyAgainIdsFrom(emptyList()).isEmpty())
        assertTrue(buyAgainIdsFrom(listOf(pedido())).isEmpty())
    }

    @Test
    fun `a tela so se declara vazia quando nenhuma secao trouxe nada`() {
        assertFalse(HomeFeedUiState().hasContent)
        assertTrue(HomeFeedUiState(categories = listOf(com.pixstop.mobile.domain.model.Category(1, "Bebidas", 5))).hasContent)
    }
}
