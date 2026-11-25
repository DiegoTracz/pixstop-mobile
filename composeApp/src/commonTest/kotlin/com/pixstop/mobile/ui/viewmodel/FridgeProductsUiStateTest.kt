package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.model.FridgeProduct
import com.pixstop.mobile.domain.model.FridgeProducts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** "Produtos desta geladeira" (APP_OPERADOR_PRODUTOS.md, G2). */
class FridgeProductsUiStateTest {

    private fun produto(id: Long, name: String, quantity: Int = 0, warehouse: Int = 0, barcode: String? = null) = FridgeProduct(
        id = id,
        name = name,
        category = "Bebidas",
        barcode = barcode,
        imageUrl = null,
        price = 5.0,
        quantity = quantity,
        inWarehouse = warehouse,
        lowStockThreshold = null,
    )

    private fun estado(here: List<FridgeProduct> = emptyList(), others: List<FridgeProduct> = emptyList()) =
        FridgeProductsUiState(applianceId = 4, products = FridgeProducts(4, "Copa", here, others))

    @Test
    fun `nao da para por mais do que o deposito tem`() {
        val bis = produto(1, "Bis", warehouse = 8)
        val state = estado(others = listOf(bis)).withAdding(bis, 20)

        assertEquals(8, state.addingOf(1))
        assertEquals(8, state.units)
        assertTrue(state.canRestock)
    }

    @Test
    fun `sem nada no deposito o contador vira primeira carga`() {
        val agua = produto(2, "Água", warehouse = 0)
        val bis = produto(1, "Bis", warehouse = 8)
        val state = estado(others = listOf(agua, bis)).withAdding(agua, 10).withAdding(bis, 2)

        assertEquals(mapOf(2L to 10), state.firstLoad)
        assertEquals(mapOf(1L to 2), state.adding)
        assertEquals(10, state.addingOf(2))
        assertEquals(12, state.units)
        assertEquals(10, state.firstLoadUnits)
    }

    @Test
    fun `zerar tira o produto do abastecimento`() {
        val bis = produto(1, "Bis", warehouse = 8)
        val state = estado(others = listOf(bis)).withAdding(bis, 3).withAdding(bis, 0)

        assertTrue(state.adding.isEmpty())
        assertFalse(state.canRestock)
    }

    @Test
    fun `a busca acha pelo nome e pelo codigo de barras`() {
        val state = estado(
            here = listOf(produto(1, "Coca-Cola lata", quantity = 4, barcode = "7894900011517")),
            others = listOf(produto(2, "Bis", warehouse = 8)),
        )

        assertEquals(listOf(2L), state.copy(query = "bis").others.map { it.id })
        assertEquals(listOf(1L), state.copy(query = "7894900011517").here.map { it.id })
        assertTrue(state.copy(query = "7894900011517").others.isEmpty())
    }

    @Test
    fun `geladeira sem nada dentro e empresa sem produto sao avisos diferentes`() {
        assertTrue(estado(others = listOf(produto(2, "Bis", warehouse = 8))).isEmptyFridge)
        assertFalse(estado(others = listOf(produto(2, "Bis", warehouse = 8))).hasNoProducts)
        assertTrue(estado().hasNoProducts)
        assertFalse(estado(here = listOf(produto(1, "Coca", quantity = 3))).isEmptyFridge)
    }
}
