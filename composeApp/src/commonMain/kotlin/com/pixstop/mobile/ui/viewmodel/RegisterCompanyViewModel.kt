package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.model.RegisterCompanyRequest
import com.pixstop.mobile.data.repository.AuthRepository
import com.pixstop.mobile.domain.auth.PasswordRule
import com.pixstop.mobile.domain.model.DomainError
import com.pixstop.mobile.domain.model.Outcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CompanyDocumentType(val apiValue: String, val label: String, val digits: Int) {
    Cnpj("cnpj", "CNPJ", 14),
    Cpf("cpf", "CPF", 11),
}

/**
 * Os campos guardam só os dígitos; a máscara é desenhada na tela. Assim o
 * cursor não pula quando a pessoa digita no meio do número.
 */
data class RegisterCompanyUiState(
    val companyName: String = "",
    val documentType: CompanyDocumentType = CompanyDocumentType.Cnpj,
    val documentDigits: String = "",
    val phoneDigits: String = "",
    val adminName: String = "",
    val adminEmail: String = "",
    val password: String = "",
    val passwordConfirmation: String = "",
    val isLoading: Boolean = false,
    val fieldErrors: Map<String, String> = emptyMap(),
    val generalError: String? = null,
    val isRegistered: Boolean = false,
)

/**
 * Cadastro de empresa pelo app, com os mesmos campos e regras da web.
 */
class RegisterCompanyViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterCompanyUiState())
    val uiState: StateFlow<RegisterCompanyUiState> = _uiState.asStateFlow()

    fun onCompanyNameChange(value: String) = edit("company_name") { copy(companyName = value) }

    fun onDocumentTypeChange(type: CompanyDocumentType) = edit("company_document") {
        copy(documentType = type, documentDigits = documentDigits.take(type.digits))
    }

    fun onDocumentChange(value: String) = edit("company_document") {
        copy(documentDigits = value.filter(Char::isDigit).take(documentType.digits))
    }

    fun onPhoneChange(value: String) = edit("company_phone") {
        copy(phoneDigits = value.filter(Char::isDigit).take(11))
    }

    fun onAdminNameChange(value: String) = edit("admin_name") { copy(adminName = value) }

    fun onAdminEmailChange(value: String) = edit("admin_email") { copy(adminEmail = value) }

    fun onPasswordChange(value: String) = edit("password") { copy(password = value) }

    fun onPasswordConfirmationChange(value: String) = edit("password_confirmation") { copy(passwordConfirmation = value) }

    fun register() {
        if (!validateLocally()) return

        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null, fieldErrors = emptyMap()) }

            val request = RegisterCompanyRequest(
                companyName = state.companyName.trim(),
                companyDocumentType = state.documentType.apiValue,
                companyDocument = state.documentDigits,
                companyPhone = state.phoneDigits,
                adminName = state.adminName.trim(),
                adminEmail = state.adminEmail.trim().lowercase(),
                password = state.password,
                passwordConfirmation = state.passwordConfirmation,
            )

            when (val result = authRepository.registerCompany(request)) {
                is Outcome.Success -> _uiState.update { it.copy(isLoading = false, isRegistered = true) }
                is Outcome.Failure -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        generalError = result.error.message,
                        fieldErrors = (result.error as? DomainError.Validation)?.fieldErrors ?: emptyMap(),
                    )
                }
            }
        }
    }

    /**
     * O que dá para conferir sem a rede. O dígito verificador do documento
     * fica com o servidor, que é quem decide.
     */
    private fun validateLocally(): Boolean {
        val state = _uiState.value
        val errors = mutableMapOf<String, String>()

        if (state.companyName.isBlank()) {
            errors["company_name"] = "O nome da empresa é obrigatório."
        }

        if (state.documentDigits.length != state.documentType.digits) {
            errors["company_document"] = "O ${state.documentType.label} deve ter ${state.documentType.digits} dígitos."
        }

        if (state.phoneDigits.length != 11 || state.phoneDigits[2] != '9') {
            errors["company_phone"] = "Digite um WhatsApp com DDD, como (11) 99999-0000."
        }

        if (state.adminName.isBlank()) {
            errors["admin_name"] = "Seu nome é obrigatório."
        }

        val email = state.adminEmail.trim()
        if (email.isBlank()) {
            errors["admin_email"] = "Seu e-mail é obrigatório."
        } else if (!email.contains("@") || !email.substringAfter("@").contains(".")) {
            errors["admin_email"] = "Digite um e-mail válido."
        }

        PasswordRule.firstFailure(state.password)?.let { errors["password"] = it.error }

        if (state.passwordConfirmation != state.password) {
            errors["password_confirmation"] = "As senhas não conferem."
        }

        _uiState.update { it.copy(fieldErrors = errors) }

        return errors.isEmpty()
    }

    private fun edit(field: String, change: RegisterCompanyUiState.() -> RegisterCompanyUiState) {
        _uiState.update { it.change().copy(fieldErrors = it.fieldErrors - field, generalError = null) }
    }
}

/** Máscaras do cadastro, usadas com o `DigitMask`: o valor segue só com dígitos. */
object CompanyFormat {

    /** `11.444.777/0001-61` */
    fun cnpj(value: String): String = mask(value.take(14), mapOf(2 to '.', 5 to '.', 8 to '/', 12 to '-'))

    /** `(11) 98765-4321` */
    fun phone(value: String): String {
        val digits = value.take(11)

        if (digits.isEmpty()) return ""

        return buildString {
            digits.forEachIndexed { index, digit ->
                when (index) {
                    0 -> append('(')
                    2 -> append(") ")
                    7 -> append('-')
                }
                append(digit)
            }
        }
    }

    private fun mask(digits: String, separators: Map<Int, Char>): String = buildString {
        digits.forEachIndexed { index, digit ->
            separators[index]?.let(::append)
            append(digit)
        }
    }
}
