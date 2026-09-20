package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.model.CountProduct
import com.pixstop.mobile.domain.model.CountResult
import com.pixstop.mobile.domain.model.CountResultItem
import com.pixstop.mobile.domain.model.LossSuggestion
import com.pixstop.mobile.domain.model.WarehouseProduct
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A regra da visita, sem rede e sem Compose (CONCILIACAO_MOBILE.md).
 *
 * O que estes testes protegem é o que decide a conciliação: "não contado" não
 * é zero, a busca não apaga o que já foi digitado, e enviar exige ter contado
 * alguma coisa.
 */
class VisitUiStateTest {

    private fun produtos() = listOf(
        CountProduct(1, "Monster Energy", "Energéticos", "7891991010856", null),
        CountProduct(2, "Coca-Cola Lata", "Refrigerantes", null, null),
        CountProduct(3, "Água 500ml", "Águas", null, null),
    )

    @Test
    fun `nao contado e zero sao coisas diferentes`() {
        val state = VisitUiState(products = produtos(), counts = mapOf(1L to 0))

        assertEquals(0, state.countOf(1))
        assertNull(state.countOf(2))
        assertEquals(1, state.countedCount)
        assertEquals(listOf(2L, 3L), state.skipped)
        assertTrue(state.hasSkipped)
    }

    @Test
    fun `contar tudo tira a pendencia e completa o progresso`() {
        val state = VisitUiState(products = produtos(), counts = mapOf(1L to 6, 2L to 0, 3L to 12))

        assertFalse(state.hasSkipped)
        assertEquals(1f, state.progress)
        assertEquals(3, state.countedCount)
    }

    @Test
    fun `enviar exige ao menos uma linha contada`() {
        val vazio = VisitUiState(products = produtos())
        val comUma = vazio.copy(counts = mapOf(2L to 4))

        assertFalse(vazio.canReview)
        assertTrue(comUma.canReview)
        // Enquanto o envio acontece, o botão para de aceitar toque.
        assertFalse(comUma.copy(isWorking = true).canReview)
    }

    @Test
    fun `a busca filtra a lista sem apagar o que ja foi digitado`() {
        val state = VisitUiState(products = produtos(), counts = mapOf(1L to 6), query = "coca")

        assertEquals(listOf(2L), state.visibleProducts.map { it.id })
        assertEquals(6, state.countOf(1))
        // O que está fora da busca continua contando para o total.
        assertEquals(1, state.countedCount)
    }

    @Test
    fun `a busca tambem acha pela categoria`() {
        val state = VisitUiState(products = produtos(), query = "refrigerante")

        assertEquals(listOf(2L), state.visibleProducts.map { it.id })
    }

    @Test
    fun `a sugestao do abastecimento respeita o deposito`() {
        // Grade cheia é o dobro do mínimo: 12 para um mínimo de 6.
        val comFolga = WarehouseProduct(1, "Monster", null, inWarehouse = 24, lowStockThreshold = 6)
        val noLimite = WarehouseProduct(2, "Coca", null, inWarehouse = 3, lowStockThreshold = 6)

        assertEquals(6, comFolga.suggestionFor(inAppliance = 6))
        assertEquals(0, comFolga.suggestionFor(inAppliance = 20))
        // Não se sugere colocar o que não existe no carro.
        assertEquals(3, noLimite.suggestionFor(inAppliance = 0))
    }

    @Test
    fun `o resultado separa o que faltou do que sobrou`() {
        val result = CountResult(
            id = 7,
            countedItems = 3,
            skippedItems = 1,
            missingValue = 25.5,
            alert = true,
            items = listOf(
                CountResultItem(1, "Monster", expected = 9, counted = 6, difference = -3, differenceValue = -25.5, suggestedReason = null),
                CountResultItem(2, "Coca", expected = 4, counted = 5, difference = 1, differenceValue = 3.0, suggestedReason = null),
                CountResultItem(3, "Água", expected = 6, counted = 6, difference = 0, differenceValue = 0.0, suggestedReason = null),
            ),
        )

        assertEquals(listOf(1L), result.missing.map { it.productId })
        assertEquals(listOf(2L), result.extra.map { it.productId })
        assertEquals(1, result.matched)
        assertTrue(result.hasDifferences)
    }

    @Test
    fun `a sugestao aparece na hora e o mesmo toque a desfaz`() {
        val result = CountResult(
            id = 7,
            countedItems = 1,
            skippedItems = 0,
            missingValue = 25.5,
            alert = false,
            items = listOf(
                CountResultItem(1, "Monster", expected = 9, counted = 6, difference = -3, differenceValue = -25.5, suggestedReason = null),
            ),
        )

        val comSugestao = result.withSuggestion(1, LossSuggestion.Expiry)
        val semSugestao = comSugestao.withSuggestion(1, null)

        assertEquals(LossSuggestion.Expiry, comSugestao.items.first().suggestedReason)
        assertNull(semSugestao.items.first().suggestedReason)
    }
}
