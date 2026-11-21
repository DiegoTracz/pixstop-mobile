package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.repository.PrizeWheelRepository
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.PrizeSpinResult
import com.pixstop.mobile.domain.model.PrizeWheel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PrizeWheelUiState(
    val wheel: PrizeWheel? = null,
    val isLoading: Boolean = true,
    /** O giro foi pedido e a roda ainda não parou. */
    val isSpinning: Boolean = false,
    /** O que o servidor sorteou; a tela anima até ele e só então mostra. */
    val pending: PrizeSpinResult? = null,
    val lastResult: PrizeSpinResult? = null,
    val error: String? = null,
)

/**
 * A roleta de prêmios (docs/plans/LANCAMENTO_LOCARMAIS.md no servidor).
 *
 * O giro tem dois tempos: o servidor sorteia (`pending`) e a tela anima até a
 * fatia; quando a roda para, `onSpinAnimated` mostra o resultado e troca o
 * estado pelo que o servidor devolveu junto.
 */
class PrizeWheelViewModel(private val repository: PrizeWheelRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PrizeWheelUiState())
    val uiState: StateFlow<PrizeWheelUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = _uiState.value.wheel == null, error = null)

            _uiState.value = when (val result = repository.current()) {
                is Outcome.Success -> _uiState.value.copy(wheel = result.value, isLoading = false)
                is Outcome.Failure -> _uiState.value.copy(isLoading = false, error = result.error.message)
            }
        }
    }

    fun spin() {
        val state = _uiState.value

        if (state.isSpinning || (state.wheel?.spinsLeft ?: 0) == 0) {
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSpinning = true, lastResult = null, error = null)

            _uiState.value = when (val result = repository.spin()) {
                is Outcome.Success -> _uiState.value.copy(pending = result.value)
                is Outcome.Failure -> _uiState.value.copy(isSpinning = false, error = result.error.message)
            }
        }
    }

    /** A roda parou na fatia sorteada. */
    fun onSpinAnimated() {
        val pending = _uiState.value.pending ?: return

        _uiState.value = _uiState.value.copy(
            isSpinning = false,
            pending = null,
            lastResult = pending,
            wheel = pending.wheel ?: _uiState.value.wheel,
        )
    }
}
