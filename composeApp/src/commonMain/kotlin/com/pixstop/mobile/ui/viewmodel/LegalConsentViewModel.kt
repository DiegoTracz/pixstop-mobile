package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.repository.LegalRepository
import com.pixstop.mobile.domain.model.LegalDocument
import com.pixstop.mobile.domain.model.Outcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LegalConsentUiState(
    val documents: List<LegalDocument> = emptyList(),
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val accepted: Boolean = false,
    val isAccepted: Boolean = false,
    val error: String? = null,
) {
    /** Só o que ainda falta aceitar precisa ser enviado. */
    val pending: List<LegalDocument> get() = documents.filterNot { it.accepted }

    val canSubmit: Boolean get() = accepted && !isSubmitting && pending.isNotEmpty()
}

/**
 * Aceite dos documentos legais.
 *
 * A caixa de marcar é uma só, como no cadastro, mas o envio nomeia cada
 * documento pendente: o registro precisa dizer qual versão foi aceita, e não
 * apenas que houve um aceite.
 */
class LegalConsentViewModel(private val legal: LegalRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LegalConsentUiState())
    val uiState: StateFlow<LegalConsentUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = legal.documents()) {
                is Outcome.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    documents = result.value,
                )

                is Outcome.Failure -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.error.message,
                )
            }
        }
    }

    fun toggleAccepted(value: Boolean) {
        _uiState.value = _uiState.value.copy(accepted = value)
    }

    fun submit() {
        val state = _uiState.value

        if (!state.canSubmit) {
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSubmitting = true, error = null)

            when (val result = legal.accept(state.pending.map { it.id })) {
                is Outcome.Success -> _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    isAccepted = true,
                )

                is Outcome.Failure -> _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = result.error.message,
                )
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
