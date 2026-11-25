package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.model.BatchItem
import com.pixstop.mobile.domain.model.ProductCategory
import com.pixstop.mobile.domain.model.SimilarProduct
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Vários produtos numa foto (APP_OPERADOR_PRODUTOS.md, G4.1). */
class BatchProductsUiStateTest {

    private fun item(name: String, price: Double? = 5.5, similar: List<SimilarProduct> = emptyList()) = BatchItem(
        name = name,
        description = null,
        category = ProductCategory(3, "Bebidas"),
        estimatedPrice = price,
        quantity = 6,
        photo = null,
        similar = similar,
    )

    @Test
    fun `a linha nasce com o preco da foto e sem custo`() {
        val row = BatchRow(item("Guaraná lata"))

        assertEquals("5,50", row.price)
        assertEquals("", row.cost)
        assertFalse(row.isReady)
        assertTrue(row.copy(cost = "2,80").isReady)
    }

    @Test
    fun `com parecido ja cadastrado a linha comeca de fora`() {
        val row = BatchRow(item("Guaraná lata", similar = listOf(SimilarProduct(1, "Guaraná Antarctica 350ml", 5.0, 12))))

        assertFalse(row.included)
        assertTrue(BatchRow(item("Bis")).included)
    }

    @Test
    fun `so salva quando toda linha marcada tem custo`() {
        val prontas = BatchProductsUiState(rows = listOf(BatchRow(item("Guaraná"), cost = "2,80"), BatchRow(item("Bis"))))

        assertFalse(prontas.canSave)
        assertEquals(1, prontas.missingCost)
        // Tirar a linha sem custo libera o resto.
        assertTrue(prontas.copy(rows = listOf(prontas.rows[0], prontas.rows[1].copy(included = false))).canSave)
    }

    @Test
    fun `a linha salva sai da conta e o fim chega quando todas foram salvas`() {
        val state = BatchProductsUiState(
            rows = listOf(
                BatchRow(item("Guaraná"), cost = "2,80", status = BatchStatus.Saved),
                BatchRow(item("Bis"), cost = "1,20", status = BatchStatus.Failed("Código já usado")),
            ),
        )

        assertEquals(1, state.toSave.size)
        assertEquals(1, state.failed)
        assertFalse(state.isDone)
        assertTrue(state.copy(rows = state.rows.map { it.copy(status = BatchStatus.Saved) }).isDone)
    }

    @Test
    fun `o produto da foto vai sem estoque inicial`() {
        val product = BatchRow(item("  Guaraná  "), cost = "2,80").toNewProduct()

        assertEquals("Guaraná", product.name)
        assertEquals(0, product.initialStock)
        assertEquals(3L, product.categoryId)
        assertNull(product.catalogProductId)
    }
}
