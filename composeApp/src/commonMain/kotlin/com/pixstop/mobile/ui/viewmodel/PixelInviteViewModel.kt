package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.repository.PixelInviteRepository
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.PixelInvite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PixelInviteUiState(
    val invite: PixelInvite? = null,
    val phone: String = "",
    val isLoading: Boolean = true,
    val isAccepting: Boolean = false,
    /** O nome da empresa em que a pessoa acabou de entrar, quando o aceite deu certo. */
    val acceptedCompany: String? = null,
    val acceptedPixels: Int = 0,
    val error: String? = null,
) {
    val canAccept: Boolean
        get() = invite?.isOpen == true && !isAccepting && (invite.needsPhone.not() || phone.filter(Char::isDigit).length >= 10)
}

/**
 * A tela do convite com pixels: mostra o presente, pede o telefone quando o
 * convite foi por telefone, e aceita uma vez.
 */
class PixelInviteViewModel(
    private val code: String,
    private val repository: PixelInviteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PixelInviteUiState())
    val uiState: StateFlow<PixelInviteUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            _uiState.value = when (val result = repository.show(code)) {
                is Outcome.Success -> _uiState.value.copy(invite = result.value, isLoading = false)
                is Outcome.Failure -> _uiState.value.copy(isLoading = false, error = result.error.message)
            }
        }
    }

    fun onPhoneChange(value: String) {
        _uiState.value = _uiState.value.copy(phone = value.filter { it.isDigit() || it in " ()-+" }.take(20), error = null)
    }

    fun accept() {
        val state = _uiState.value
        if (!state.canAccept) return

        viewModelScope.launch {
            _uiState.value = state.copy(isAccepting = true, error = null)

            _uiState.value = when (val result = repository.accept(code, state.phone.takeIf { state.invite?.needsPhone == true })) {
                is Outcome.Success -> _uiState.value.copy(
                    isAccepting = false,
                    acceptedCompany = result.value.name,
                    acceptedPixels = result.value.pixels,
                )

                is Outcome.Failure -> _uiState.value.copy(isAccepting = false, error = result.error.message)
            }
        }
    }
}
