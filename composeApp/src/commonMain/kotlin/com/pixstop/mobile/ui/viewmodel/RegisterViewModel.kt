package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.pixstop.mobile.core.config.CompanyCodeParser
import com.pixstop.mobile.data.model.RegisterUserRequest
import com.pixstop.mobile.data.model.User
import com.pixstop.mobile.data.repository.AuthRepository
import com.pixstop.mobile.domain.model.DomainError
import com.pixstop.mobile.domain.model.Outcome

/**
 * Estado da tela de registro.
 */
data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",
    val companyCode: String = "",
    val isCompanyCodeExpanded: Boolean = false,
    val isQrScannerOpen: Boolean = false,
    val isLoading: Boolean = false,
    val fieldErrors: Map<String, String> = emptyMap(),
    val generalError: String? = null,
    val isRegistered: Boolean = false,
    val user: User? = null
)

/**
 * ViewModel para a tela de Registro de Usuário.
 */
class RegisterViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(
            name = name,
            fieldErrors = _uiState.value.fieldErrors - "name",
            generalError = null
        )
    }

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(
            email = email,
            fieldErrors = _uiState.value.fieldErrors - "email",
            generalError = null
        )
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            fieldErrors = _uiState.value.fieldErrors - "password",
            generalError = null
        )
    }

    fun onPasswordConfirmationChange(confirmation: String) {
        _uiState.value = _uiState.value.copy(
            passwordConfirmation = confirmation,
            fieldErrors = _uiState.value.fieldErrors - "password_confirmation",
            generalError = null
        )
    }

    fun onCompanyCodeChange(code: String) {
        _uiState.value = _uiState.value.copy(
            companyCode = code,
            fieldErrors = _uiState.value.fieldErrors - "company_code",
            generalError = null
        )
    }

    fun toggleCompanyCodeSection() {
        _uiState.value = _uiState.value.copy(
            isCompanyCodeExpanded = !_uiState.value.isCompanyCodeExpanded
        )
    }

    fun openQrScanner() {
        _uiState.value = _uiState.value.copy(isQrScannerOpen = true)
    }

    fun closeQrScanner() {
        _uiState.value = _uiState.value.copy(isQrScannerOpen = false)
    }

    /**
     * Chamado quando o QR code é escaneado.
     * Extrai o company_code da URL e preenche o campo.
     */
    fun onQrCodeScanned(rawValue: String) {
        val code = CompanyCodeParser.parse(rawValue)
        _uiState.value = _uiState.value.copy(
            companyCode = code,
            isCompanyCodeExpanded = true,
            isQrScannerOpen = false,
            fieldErrors = _uiState.value.fieldErrors - "company_code",
            generalError = null
        )
    }

    /**
     * Validação local dos campos.
     * @return true se tudo ok
     */
    private fun validateLocally(): Boolean {
        val errors = mutableMapOf<String, String>()
        val state = _uiState.value

        if (state.name.isBlank()) {
            errors["name"] = "Nome é obrigatório"
        }

        if (state.email.isBlank()) {
            errors["email"] = "E-mail é obrigatório"
        } else if (!isValidEmail(state.email)) {
            errors["email"] = "E-mail inválido"
        }

        if (state.password.isBlank()) {
            errors["password"] = "Senha é obrigatória"
        } else if (state.password.length < 8) {
            errors["password"] = "Senha deve ter no mínimo 8 caracteres"
        }

        if (state.passwordConfirmation.isBlank()) {
            errors["password_confirmation"] = "Confirmação é obrigatória"
        } else if (state.password != state.passwordConfirmation) {
            errors["password_confirmation"] = "Senhas não coincidem"
        }

        if (errors.isNotEmpty()) {
            _uiState.value = state.copy(fieldErrors = errors)
            return false
        }
        return true
    }

    /**
     * Registra o usuário.
     */
    fun register() {
        if (!validateLocally()) return

        val state = _uiState.value

        viewModelScope.launch {
            _uiState.value = state.copy(
                isLoading = true,
                generalError = null,
                fieldErrors = emptyMap()
            )

            val request = RegisterUserRequest(
                name = state.name.trim(),
                email = state.email.trim(),
                password = state.password,
                passwordConfirmation = state.passwordConfirmation,
                companyCode = state.companyCode.trim().ifBlank { null }
            )

            when (val result = authRepository.registerUser(request)) {
                is Outcome.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRegistered = true,
                        user = result.value
                    )
                }
                is Outcome.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        generalError = result.error.message,
                        fieldErrors = (result.error as? DomainError.Validation)?.fieldErrors ?: emptyMap()
                    )
                }
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }
}

