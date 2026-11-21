package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.model.HourSales
import com.pixstop.mobile.domain.model.OperatorAttention
import com.pixstop.mobile.domain.model.OperatorInsights
import com.pixstop.mobile.domain.model.OperatorTotals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** As regras da visão geral do operador, sem rede (PAINEL_ESTOQUE.md, D1). */
class OperatorHomeUiStateTest {

    private fun totals(money: Boolean) = OperatorTotals(
        companies = 2, appliances = 3, offline = 1, attention = 7, soldToday = 12,
        revenueToday = if (money) 120.0 else null,
        revenueMonth = if (money) 2000.0 else null,
        profitMonth = if (money) 600.0 else null,
        stockValue = if (money) 900.0 else null,
    )

    private fun attention(count: Int) = List(count) {
        OperatorAttention("zeroed", "Produto $it zerado", null, "Empresa", "t1", 5.0)
    }

    private fun insights(money: Boolean, attention: Int = 7, hours: List<HourSales> = emptyList()) = OperatorInsights(
        operatorName = "Diego",
        seesMoney = money,
        totals = totals(money),
        attention = attention(attention),
        companies = emptyList(),
        salesByHour = hours,
    )

    @Test
    fun `o repositor nao recebe nenhum cartao em reais`() {
        val cards = OperatorHomeUiState(insights = insights(money = false)).headline

        assertTrue(cards.none { "R$" in it.value })
        assertEquals(listOf("Unidades hoje", "Geladeiras"), cards.map { it.label })
    }

    @Test
    fun `o dono ve o dinheiro e a porta fora do ar acende`() {
        val cards = OperatorHomeUiState(insights = insights(money = true)).headline

        assertEquals(listOf("Vendido hoje", "Lucro no mês", "Unidades hoje", "Geladeiras", "Em estoque"), cards.map { it.label })
        assertTrue(cards.single { it.label == "Geladeiras" }.isAlert)
    }

    @Test
    fun `mostra cinco avisos na ordem do servidor e esconde o resto`() {
        val state = OperatorHomeUiState(insights = insights(money = true, attention = 7))

        assertEquals((0 until 5).map { "Produto $it zerado" }, state.visibleAttention.map { it.title })
        assertEquals(2, state.hiddenAttention)
        assertTrue(state.canToggleAttention)
        assertEquals(7, state.copy(showAllAttention = true).visibleAttention.size)
    }

    @Test
    fun `sem aviso diz que esta tudo em ordem`() {
        val state = OperatorHomeUiState(insights = insights(money = true, attention = 0))

        assertTrue(state.allGood)
        assertFalse(state.canToggleAttention)
        assertFalse(OperatorHomeUiState().allGood)
    }

    @Test
    fun `o grafico corta a madrugada vazia`() {
        val hours = (0..23).map { HourSales(it, today = if (it in 9..11) 2 else 0, average = if (it == 15) 1.5 else 0.0) }
        val state = OperatorHomeUiState(insights = insights(money = true, hours = hours))

        assertEquals((9..15).toList(), state.chartHours.map { it.hour })
        assertTrue(OperatorHomeUiState(insights = insights(money = true, hours = hours.map { it.copy(today = 0, average = 0.0) })).chartHours.isEmpty())
    }
}
