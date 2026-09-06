package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.model.SavedCard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * A tela muda antes de o servidor confirmar — o padrão troca no toque, o
 * cartão removido some na hora — e as transições são puras para poderem ser
 * conferidas sem rede.
 */
class CardsUiStateTest {

    private fun cartao(id: Long, isDefault: Boolean) = SavedCard(
        id = id,
        lastFour = "70$id$id",
        brand = "Visa",
        isDefault = isDefault,
        expires = "12/2030",
    )

    private val estado = CardsUiState(
        cards = listOf(cartao(1, isDefault = true), cartao(2, isDefault = false)),
        isLoading = false,
    )

    @Test
    fun `escolher outro padrao desmarca o anterior`() {
        val depois = estado.withDefault(2)

        assertTrue(depois.cards.first { it.id == 2L }.isDefault)
        assertFalse(depois.cards.first { it.id == 1L }.isDefault)
        assertEquals(1, depois.cards.count { it.isDefault })
    }

    @Test
    fun `escolher o que ja e padrao, ou um cartao que nao existe, nao mexe em nada`() {
        assertSame(estado, estado.withDefault(1))
        assertSame(estado, estado.withDefault(99))
    }

    @Test
    fun `remover tira o cartao da lista e nao promove ninguem`() {
        val depois = estado.without(1)

        assertEquals(listOf(2L), depois.cards.map { it.id })
        assertFalse(depois.cards.single().isDefault)
    }

    @Test
    fun `a lista vazia so e vazia depois de carregar e sem erro`() {
        assertFalse(CardsUiState(isLoading = true).isEmpty)
        assertFalse(CardsUiState(isLoading = false, error = "Sem rede").isEmpty)
        assertTrue(CardsUiState(isLoading = false).isEmpty)
        assertFalse(estado.isEmpty)
    }
}
