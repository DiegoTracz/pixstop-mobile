package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.domain.model.DomainError
import com.pixstop.mobile.domain.model.Outcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * "Esqueci a senha": o servidor manda por e-mail o link da página do site
 * onde a pessoa cria a senha nova; o app só pede o link e explica o caminho.
 */
data class ForgotPasswordUiState(
    val email: String = "",
    val isSending: Boolean = false,
    /** O e-mail para onde o link foi; enquanto nulo, a folha mostra o formulário. */
    val sentTo: String? = null,
    val error: String? = null,
) {
    val canSend: Boolean get() = !isSending && looksLikeEmail(email)

    /** O que a folha faz com a resposta do servidor. */
    fun withResult(outcome: Outcome<Unit>): ForgotPasswordUiState = when (outcome) {
        is Outcome.Success -> copy(isSending = false, sentTo = email.trim(), error = null)
        is Outcome.Failure -> copy(isSending = false, error = message(outcome.error))
    }

    companion object {
        fun looksLikeEmail(text: String): Boolean = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""").matches(text.trim())

        private fun message(error: DomainError): String = when (error) {
            is DomainError.Offline -> "Sem conexão. Tente de novo quando a internet voltar."
            is DomainError.RateLimited -> "Você já pediu há pouco. Espere um minuto e tente de novo."
            else -> error.message
        }
    }
}

class ForgotPasswordViewModel(
    private val requestLink: suspend (String) -> Outcome<Unit>,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    /** Abre a folha já com o e-mail que a pessoa digitou no login. */
    fun start(email: String) = _uiState.update { ForgotPasswordUiState(email = email.trim()) }

    fun onEmail(text: String) = _uiState.update { it.copy(email = text, error = null) }

    fun send() {
        val state = _uiState.value
        if (!state.canSend) return

        _uiState.update { it.copy(isSending = true, error = null) }
        viewModelScope.launch {
            val outcome = requestLink(state.email.trim())
            _uiState.update { it.withResult(outcome) }
        }
    }
}
