package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.repository.ProfileRepository
import com.pixstop.mobile.domain.model.Outcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val isSavingProfile: Boolean = false,
    val profileMessage: String? = null,
    val profileError: String? = null,

    val deletePassword: String = "",
    val isDeleteOpen: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteError: String? = null,
    val isDeleted: Boolean = false,

    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmation: String = "",
    val isSavingPassword: Boolean = false,
    val passwordMessage: String? = null,
    val passwordError: String? = null,
) {
    val canSaveProfile: Boolean
        get() = !isSavingProfile && name.isNotBlank() && email.isNotBlank()

    /**
     * A confirmação é conferida aqui só para avisar antes de gastar uma
     * chamada — quem decide é o servidor, que exige `confirmed`.
     */
    val passwordsMatch: Boolean get() = newPassword == confirmation

    val canSavePassword: Boolean
        get() = !isSavingPassword &&
            currentPassword.isNotBlank() &&
            newPassword.length >= MIN_PASSWORD &&
            passwordsMatch

    /** Excluir a conta exige a senha: é o que separa o pedido do acidente. */
    val canDelete: Boolean get() = !isDeleting && deletePassword.isNotBlank()

    companion object {
        const val MIN_PASSWORD = 8
    }
}

/**
 * Perfil e senha.
 *
 * As duas metades são independentes de propósito: falhar ao trocar a senha não
 * pode apagar o nome que a pessoa acabou de digitar.
 */
class ProfileViewModel(private val profiles: ProfileRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /** Preenche com o que o `/me` já trouxe, sem uma chamada a mais. */
    fun start(name: String, email: String) {
        if (_uiState.value.name.isBlank() && _uiState.value.email.isBlank()) {
            _uiState.value = _uiState.value.copy(name = name, email = email)
        }
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value, profileError = null, profileMessage = null)
    }

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value, profileError = null, profileMessage = null)
    }

    fun onCurrentPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(currentPassword = value, passwordError = null, passwordMessage = null)
    }

    fun onNewPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(newPassword = value, passwordError = null, passwordMessage = null)
    }

    fun onConfirmationChange(value: String) {
        _uiState.value = _uiState.value.copy(confirmation = value, passwordError = null, passwordMessage = null)
    }

    /**
     * @param onSaved chamado para o `/me` ser recarregado: o nome aparece em
     *   outras telas e ficaria velho se só esta soubesse da mudança.
     */
    fun saveProfile(onSaved: () -> Unit = {}) {
        val state = _uiState.value

        if (!state.canSaveProfile) {
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSavingProfile = true, profileError = null, profileMessage = null)

            when (val result = profiles.update(state.name, state.email)) {
                is Outcome.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSavingProfile = false,
                        name = result.value.name,
                        email = result.value.email,
                        profileMessage = "Perfil atualizado.",
                    )
                    onSaved()
                }

                is Outcome.Failure -> _uiState.value = _uiState.value.copy(
                    isSavingProfile = false,
                    profileError = result.error.message,
                )
            }
        }
    }

    fun openDelete() {
        _uiState.value = _uiState.value.copy(isDeleteOpen = true, deletePassword = "", deleteError = null)
    }

    fun dismissDelete() {
        _uiState.value = _uiState.value.copy(isDeleteOpen = false, deletePassword = "", deleteError = null)
    }

    fun onDeletePasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(deletePassword = value, deleteError = null)
    }

    /**
     * Exclui a conta.
     *
     * O servidor recusa quem é o último administrador de uma empresa, e a
     * recusa vem com o nome dela — a tela só precisa mostrar a frase.
     */
    fun deleteAccount() {
        val state = _uiState.value

        if (!state.canDelete) {
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isDeleting = true, deleteError = null)

            _uiState.value = when (val result = profiles.deleteAccount(state.deletePassword)) {
                is Outcome.Success -> _uiState.value.copy(isDeleting = false, isDeleted = true)
                is Outcome.Failure -> _uiState.value.copy(isDeleting = false, deleteError = result.error.message)
            }
        }
    }

    fun savePassword() {
        val state = _uiState.value

        if (!state.canSavePassword) {
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSavingPassword = true, passwordError = null, passwordMessage = null)

            val result = profiles.updatePassword(state.currentPassword, state.newPassword, state.confirmation)

            _uiState.value = when (result) {
                // Os campos são limpos no sucesso: deixar a senha nova na tela
                // não serve para nada e ainda a expõe a quem olhar por cima.
                is Outcome.Success -> _uiState.value.copy(
                    isSavingPassword = false,
                    currentPassword = "",
                    newPassword = "",
                    confirmation = "",
                    passwordMessage = "Senha alterada.",
                )

                is Outcome.Failure -> _uiState.value.copy(
                    isSavingPassword = false,
                    passwordError = result.error.message,
                )
            }
        }
    }
}
