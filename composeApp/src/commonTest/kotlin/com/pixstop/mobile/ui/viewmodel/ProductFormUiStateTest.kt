package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.model.CatalogSuggestion
import com.pixstop.mobile.domain.model.PhotoReading
import com.pixstop.mobile.domain.model.ProductCategory
import com.pixstop.mobile.domain.model.ProductSuggestions
import com.pixstop.mobile.domain.model.SimilarProduct
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** O cadastro de produto no app (APP_OPERADOR_PRODUTOS.md, G4). */
class ProductFormUiStateTest {

    private fun leitura(similar: List<SimilarProduct> = emptyList(), catalog: List<CatalogSuggestion> = emptyList()) = PhotoReading(
        name = "Guaraná Antarctica Lata",
        description = "Refrigerante 350 ml",
        category = ProductCategory(3, "Bebidas"),
        suggestedCategory = "Bebidas",
        estimatedPrice = 5.5,
        similar = similar,
        catalog = catalog,
    )

    @Test
    fun `a foto preenche o formulario vazio`() {
        val state = ProductFormUiState().withReading(leitura())

        assertEquals("Guaraná Antarctica Lata", state.name)
        assertEquals("Refrigerante 350 ml", state.description)
        assertEquals(3L, state.categoryId)
        assertEquals("5,50", state.price)
        assertEquals("", state.cost)
    }

    @Test
    fun `o que a pessoa digitou vale mais que a foto`() {
        val state = ProductFormUiState(name = "Guaraná lata", price = "6,00", categoryId = 9).withReading(leitura())

        assertEquals("Guaraná lata", state.name)
        assertEquals("6,00", state.price)
        assertEquals(9L, state.categoryId)
    }

    @Test
    fun `salvar pede nome e preco e custo`() {
        val base = ProductFormUiState(name = "Coxinha", price = "7,00")

        assertFalse(base.canSave)
        assertTrue(base.copy(cost = "3,20").canSave)
        assertFalse(base.copy(cost = "3,20", price = "0").canSave)
        assertFalse(base.copy(cost = "3,20", name = "C").canSave)
        // Custo zero é custo informado: brinde que a empresa ganhou.
        assertTrue(base.copy(cost = "0").canSave)
    }

    @Test
    fun `parecido cadastrado segura o salvar ate a pessoa responder`() {
        val state = ProductFormUiState(cost = "2,80")
            .withReading(leitura(similar = listOf(SimilarProduct(1, "Guaraná Antarctica 350ml", 5.0, 12))))

        assertEquals(1, state.similar.size)
        assertFalse(state.canSave)

        val answered = state.copy(confirmedNew = true)
        assertTrue(answered.similar.isEmpty())
        assertTrue(answered.canSave)
    }

    @Test
    fun `a margem e calculada sobre o preco`() {
        assertEquals(50, ProductFormUiState(price = "6,00", cost = "3,00").marginPercent)
        assertNull(ProductFormUiState(price = "6,00").marginPercent)
        assertTrue(ProductFormUiState(price = "3,00", cost = "30,00").sellsAtLoss)
    }

    @Test
    fun `o envio leva os numeros e o item do catalogo escolhido`() {
        val item = CatalogSuggestion(44, "Guaraná Antarctica 350 ml", "Bebidas", hasPhoto = true)
        val product = ProductFormUiState(
            name = "  Guaraná  ",
            price = "5,50",
            cost = "2,80",
            initialStock = "12",
            barcode = " ",
            catalogChoice = item,
        ).toNewProduct()

        assertEquals("Guaraná", product.name)
        assertEquals(5.5, product.price)
        assertEquals(2.8, product.cost)
        assertEquals(12, product.initialStock)
        assertNull(product.barcode)
        assertEquals(44L, product.catalogProductId)
    }

    @Test
    fun `sem foto o item da nota ja mostra o catalogo achado pelo nome`() {
        val item = CatalogSuggestion(44, "Coca-Cola Lata 350ml", "Bebidas", hasPhoto = true, imageUrl = "https://x/coca.png")
        val state = ProductFormUiState(noteSuggestions = ProductSuggestions(similar = emptyList(), catalog = listOf(item)))

        assertEquals(listOf(item), state.catalog)
        // Com a foto do catálogo dá para a IA criar o cenário, mesmo sem foto do celular.
        assertTrue(state.copy(catalogChoice = item).canEnhance)
        assertFalse(state.canEnhance)
    }

    @Test
    fun `a foto que vai no cadastro segue o que a pessoa aprovou`() {
        val original = byteArrayOf(1)
        val ia = byteArrayOf(2)
        val item = CatalogSuggestion(44, "Coca", null, hasPhoto = true)

        assertTrue(ProductFormUiState(photo = original).photoToSend.contentEquals(original))
        // A prévia sem o "usar esta" não vale.
        assertTrue(ProductFormUiState(photo = original, enhanced = ia).photoToSend.contentEquals(original))
        assertTrue(ProductFormUiState(photo = original, enhanced = ia, useEnhanced = true).photoToSend.contentEquals(ia))
        // Catálogo escolhido sem melhoria: o servidor copia a foto de lá.
        assertNull(ProductFormUiState(photo = original, catalogChoice = item).photoToSend)
        assertTrue(ProductFormUiState(catalogChoice = item, enhanced = ia, useEnhanced = true).photoToSend.contentEquals(ia))
    }

    @Test
    fun `o catalogo do cadastro tambem respeita o tamanho do nome`() {
        val lata = CatalogSuggestion(1, "Coca-Cola Lata 350ml", "Bebidas", hasPhoto = true)
        val umLitro = CatalogSuggestion(3, "Coca-Cola 1L", "Bebidas", hasPhoto = true)
        val state = ProductFormUiState(name = "COCA COLA 1LT", noteSuggestions = ProductSuggestions(emptyList(), listOf(lata, umLitro)))

        assertEquals(listOf(3L), state.catalog.map { it.id })
    }

    @Test
    fun `escolhido o catalogo as sugestoes dele somem`() {
        val item = CatalogSuggestion(44, "Guaraná", null, hasPhoto = true)
        val state = ProductFormUiState().withReading(leitura(catalog = listOf(item)))

        assertEquals(1, state.catalog.size)
        assertTrue(state.copy(catalogChoice = item).catalog.isEmpty())
    }
}
