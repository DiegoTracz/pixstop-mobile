package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.repository.OperatorRepository
import com.pixstop.mobile.domain.model.HourSales
import com.pixstop.mobile.domain.model.OperatorAttention
import com.pixstop.mobile.domain.model.OperatorInsights
import com.pixstop.mobile.ui.components.formatMoney
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Um número do topo da visão geral. */
data class HeadlineCard(val label: String, val value: String, val isAlert: Boolean = false)

data class OperatorHomeUiState(
    val insights: OperatorInsights? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAllAttention: Boolean = false,
) {
    /** Os cinco primeiros, que é o que cabe antes da pessoa rolar; o resto sob "ver todos". */
    val visibleAttention: List<OperatorAttention>
        get() = insights?.attention.orEmpty().let { if (showAllAttention) it else it.take(ATTENTION_PREVIEW) }

    val hiddenAttention: Int
        get() = (insights?.attention?.size ?: 0) - visibleAttention.size

    /** Só há o que esconder quando a lista passa do que cabe. */
    val canToggleAttention: Boolean get() = (insights?.attention?.size ?: 0) > ATTENTION_PREVIEW

    val allGood: Boolean get() = insights != null && insights.attention.isEmpty()

    /**
     * Os números do topo. Os de dinheiro só entram para quem o vê e quando o
     * servidor mandou: o repositor nunca recebe um cartão com R$.
     */
    val headline: List<HeadlineCard>
        get() {
            val data = insights ?: return emptyList()
            val totals = data.totals
            val cards = mutableListOf<HeadlineCard>()

            if (data.seesMoney) {
                totals.revenueToday?.let { cards += HeadlineCard("Vendido hoje", formatMoney(it)) }
                totals.profitMonth?.let { cards += HeadlineCard("Lucro no mês", formatMoney(it)) }
            }

            cards += HeadlineCard("Unidades hoje", totals.soldToday.toString())
            cards += HeadlineCard(
                label = "Geladeiras",
                value = if (totals.offline > 0) "${totals.offline} de ${totals.appliances} fora" else "${totals.appliances} no ar",
                isAlert = totals.offline > 0,
            )

            if (data.seesMoney) {
                totals.stockValue?.let { cards += HeadlineCard("Em estoque", formatMoney(it)) }
            }

            return cards
        }

    /**
     * As horas que o gráfico mostra: da primeira à última com venda, hoje ou
     * na média, para a madrugada vazia não espremer as barras.
     */
    val chartHours: List<HourSales>
        get() {
            val hours = insights?.salesByHour.orEmpty()
            val active = hours.filter { it.today > 0 || it.average > 0 }
            if (active.isEmpty()) return emptyList()

            val first = active.minOf { it.hour }
            val last = active.maxOf { it.hour }

            return hours.filter { it.hour in first..last }
        }

    companion object {
        const val ATTENTION_PREVIEW = 5
    }
}

/**
 * A tela inicial de quem opera (PAINEL_ESTOQUE.md, D1): todas as empresas de
 * uma vez, o que precisa de alguém hoje e as vendas por hora.
 */
class OperatorHomeViewModel(private val operators: OperatorRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(OperatorHomeUiState())
    val uiState: StateFlow<OperatorHomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val result = operators.insights()

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    insights = result.successOrNull ?: state.insights,
                    error = result.errorOrNull?.message,
                )
            }
        }
    }

    fun toggleAllAttention() {
        _uiState.update { it.copy(showAllAttention = !it.showAllAttention) }
    }
}
