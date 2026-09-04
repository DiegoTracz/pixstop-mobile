package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.core.logging.AppLogger
import com.pixstop.mobile.core.storage.SessionStore
import com.pixstop.mobile.data.repository.AccountRepository
import com.pixstop.mobile.domain.model.Account
import com.pixstop.mobile.domain.model.DomainError
import com.pixstop.mobile.domain.model.Outcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado da conta e da empresa ativa, vivo enquanto o app estiver aberto.
 *
 * Fica acima da navegação porque quase toda tela depende dele: papel na
 * empresa, carteira, funcionalidades do plano e se falta aceitar os termos.
 * Trocar de empresa muda o que a API inteira devolve, então este é o único
 * lugar que sabe recarregar o `/me` depois da troca.
 */
data class SessionUiState(
    val account: Account? = null,
    val isLoading: Boolean = false,
    val isSwitching: Boolean = false,
    val error: String? = null,
    val loggedOut: Boolean = false,
) {
    val company get() = account?.activeCompany
    val needsCompany: Boolean get() = account != null && account.needsCompany
    val needsConsent: Boolean get() = account?.hasPendingConsent == true
}

class SessionViewModel(
    private val accounts: AccountRepository,
    private val session: SessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        // O cliente HTTP avisa quando o servidor recusa o token; a partir daqui
        // a navegação leva ao login sem que nenhuma tela precise saber disso.
        viewModelScope.launch {
            session.sessionExpired.collect {
                _uiState.value = SessionUiState(loggedOut = true)
            }
        }

        if (session.isLoggedIn()) {
            refresh()
        }
    }

    /**
     * Recarrega a conta inteira.
     *
     * Chamado no início, depois de trocar de empresa e depois de aceitar os
     * termos — sempre por completo, porque atualizar em pedaços deixaria a
     * tela mostrando saldo de uma empresa e nome de outra.
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = accounts.me()) {
                is Outcome.Success -> _uiState.value = SessionUiState(account = result.value)
                is Outcome.Failure -> handleFailure(result.error)
            }
        }
    }

    fun switchCompany(companyId: String) {
        if (_uiState.value.isSwitching) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSwitching = true, error = null)

            when (val result = accounts.switchCompany(companyId)) {
                is Outcome.Success -> {
                    AppLogger.i("Empresa ativa: ${result.value.name}", tag = "Session")
                    reloadAfterCompanyChange()
                }
                is Outcome.Failure -> {
                    _uiState.value = _uiState.value.copy(isSwitching = false, error = result.error.message)
                }
            }
        }
    }

    /**
     * Entra numa empresa pelo código. O servidor já a deixa ativa.
     */
    fun joinCompany(companyCode: String, onJoined: (String) -> Unit = {}) {
        if (_uiState.value.isSwitching) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSwitching = true, error = null)

            when (val result = accounts.joinCompany(companyCode)) {
                is Outcome.Success -> {
                    reloadAfterCompanyChange()
                    onJoined(result.value.name)
                }
                is Outcome.Failure -> {
                    _uiState.value = _uiState.value.copy(isSwitching = false, error = result.error.message)
                }
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private suspend fun reloadAfterCompanyChange() {
        when (val result = accounts.me()) {
            is Outcome.Success -> _uiState.value = SessionUiState(account = result.value)
            is Outcome.Failure -> _uiState.value = _uiState.value.copy(isSwitching = false, error = result.error.message)
        }
    }

    /**
     * Uma falha ao carregar a conta não é toda igual.
     *
     * Sem empresa e sem consentimento não são erros: são estados que a
     * navegação resolve mandando a pessoa para a tela certa. Por isso o `/me`
     * fica fora do middleware que exige empresa ativa no backend.
     */
    private fun handleFailure(error: DomainError) {
        when (error) {
            is DomainError.Unauthorized -> _uiState.value = SessionUiState(loggedOut = true)
            else -> _uiState.value = _uiState.value.copy(isLoading = false, error = error.message)
        }
    }
}
