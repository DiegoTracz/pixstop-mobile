package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.repository.OperatorRepository
import com.pixstop.mobile.domain.model.OperatorCompanies
import com.pixstop.mobile.domain.model.OperatorRound
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** As duas metades do painel: o que repor hoje e onde. */
enum class OperatorTab { Round, Companies }

data class OperatorUiState(
    val tab: OperatorTab = OperatorTab.Round,
    val round: OperatorRound? = null,
    val companies: OperatorCompanies? = null,
    val onlyLow: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * O painel de quem opera geladeiras, no celular (Fase 16, O8).
 *
 * Carrega as duas listas de uma vez: quem abre o painel na rua quer as duas —
 * o que falta e em qual empresa — e uma segunda ida à rede depois do primeiro
 * toque é justamente onde o sinal costuma faltar.
 */
class OperatorViewModel(private val operators: OperatorRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(OperatorUiState())
    val uiState: StateFlow<OperatorUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val round = operators.round(onlyLow = _uiState.value.onlyLow)
            val companies = operators.companies()

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    round = round.successOrNull ?: state.round,
                    companies = companies.successOrNull ?: state.companies,
                    // A falha de uma das duas listas é a que a tela conta; a
                    // outra continua mostrando o que já tinha.
                    error = (round.errorOrNull ?: companies.errorOrNull)?.message,
                )
            }
        }
    }

    fun selectTab(tab: OperatorTab) {
        _uiState.update { it.copy(tab = tab) }
    }

    /** O filtro que transforma a lista no roteiro do dia. */
    fun toggleOnlyLow() {
        _uiState.update { it.copy(onlyLow = !it.onlyLow) }
        load()
    }
}
