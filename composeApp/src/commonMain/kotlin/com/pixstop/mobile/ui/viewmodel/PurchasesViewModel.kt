package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.repository.PurchaseRepository
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.PurchaseSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** As compras da empresa, o rascunho primeiro (docs/plans/COMPRAS_MOBILE.md, passo 5). */
data class PurchasesUiState(
    val purchases: List<PurchaseSummary> = emptyList(),
    val canReadReceipts: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class PurchasesViewModel(private val repository: PurchaseRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchasesUiState())
    val uiState: StateFlow<PurchasesUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.update { it.copy(isLoading = it.purchases.isEmpty(), error = null) }
        viewModelScope.launch {
            when (val outcome = repository.index()) {
                is Outcome.Success -> _uiState.update {
                    it.copy(purchases = outcome.value.purchases, canReadReceipts = outcome.value.canReadReceipts, isLoading = false)
                }

                is Outcome.Failure -> _uiState.update { it.copy(isLoading = false, error = outcome.error.message) }
            }
        }
    }
}
