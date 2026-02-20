package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.pixstop.mobile.data.model.User
import com.pixstop.mobile.data.repository.AuthRepository
import com.pixstop.mobile.data.repository.Result

/**
 * Estado da tela de login
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOfflineMode: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: User? = null
)

/**
 * ViewModel para a tela de Login
 */
class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // Verifica se já está autenticado ao iniciar
        checkExistingSession()
    }

    private fun checkExistingSession() {
        if (authRepository.isAuthenticated()) {
            val cachedUser = authRepository.getCachedUser()
            if (cachedUser != null) {
                _uiState.value = _uiState.value.copy(
                    isLoggedIn = true,
                    user = cachedUser
                )
            } else {
                // Tem token mas não tem dados em cache, tenta buscar
                viewModelScope.launch {
                    when (val result = authRepository.fetchProfile()) {
                        is Result.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isLoggedIn = true,
                                user = result.data
                            )
                        }
                        is Result.Error -> {
                            // Token inválido ou sem conexão, mantém na tela de login
                            _uiState.value = _uiState.value.copy(
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(
            email = email,
            error = null
        )
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            error = null
        )
    }

    fun login() {
        val currentState = _uiState.value

        // Validação básica
        if (currentState.email.isBlank()) {
            _uiState.value = currentState.copy(error = "Digite seu email")
            return
        }
        if (currentState.password.isBlank()) {
            _uiState.value = currentState.copy(error = "Digite sua senha")
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)

            when (val result = authRepository.login(currentState.email, currentState.password)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        user = result.data,
                        error = null
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message,
                        isOfflineMode = result.isOffline
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Continua com dados offline (quando tem cache válido)
     */
    fun continueOffline() {
        val cachedUser = authRepository.getCachedUser()
        if (cachedUser != null) {
            _uiState.value = _uiState.value.copy(
                isLoggedIn = true,
                user = cachedUser,
                isOfflineMode = true,
                error = null
            )
        }
    }
}
