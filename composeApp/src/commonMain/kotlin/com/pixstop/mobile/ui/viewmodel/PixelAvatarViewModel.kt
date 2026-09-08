package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.repository.AvatarRepository
import com.pixstop.mobile.domain.model.Outcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * O caminho do avatar em pixel art: câmera → prévia → aplicar.
 *
 * A prévia fica aqui e no servidor até alguém decidir. Tirar outra foto
 * substitui a prévia e não encosta na foto do perfil — é o que permite
 * insistir até sair um retrato de que a pessoa goste.
 */
data class PixelAvatarUiState(
    val isCameraOpen: Boolean = false,
    val isWorking: Boolean = false,
    /** A prévia em data URI, esperando "usar esta". */
    val preview: String? = null,
    val message: String? = null,
    val error: String? = null,
) {
    val hasPreview: Boolean get() = preview != null
}

class PixelAvatarViewModel(private val avatars: AvatarRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PixelAvatarUiState())
    val uiState: StateFlow<PixelAvatarUiState> = _uiState.asStateFlow()

    fun openCamera() = _uiState.update { it.copy(isCameraOpen = true, error = null, message = null) }

    fun closeCamera() = _uiState.update { it.copy(isCameraOpen = false) }

    fun discardPreview() = _uiState.update { it.copy(preview = null, error = null, message = null) }

    fun dismissMessage() = _uiState.update { it.copy(message = null, error = null) }

    /** A câmera devolveu o JPEG: a partir daqui quem trabalha é o servidor. */
    fun onPhoto(photo: ByteArray) {
        _uiState.update { it.copy(isCameraOpen = false, isWorking = true, error = null, message = null) }

        viewModelScope.launch {
            when (val result = avatars.generatePixel(photo)) {
                is Outcome.Success -> _uiState.update {
                    it.copy(isWorking = false, preview = result.value)
                }

                is Outcome.Failure -> _uiState.update {
                    it.copy(isWorking = false, error = result.error.message)
                }
            }
        }
    }

    /**
     * A foto escolhida na galeria vai direto: quem já tem a foto de que gosta
     * não precisa passar pela IA nem aprovar prévia nenhuma.
     *
     * @param onApplied a conta precisa recarregar: o avatar mudou em toda a tela.
     */
    fun uploadPhoto(photo: ByteArray, onApplied: () -> Unit = {}) {
        _uiState.update { it.copy(isWorking = true, preview = null, error = null, message = null) }

        viewModelScope.launch {
            when (val result = avatars.upload(photo)) {
                is Outcome.Success -> {
                    _uiState.update { it.copy(isWorking = false, message = "Foto atualizada.") }
                    onApplied()
                }

                is Outcome.Failure -> _uiState.update {
                    it.copy(isWorking = false, error = result.error.message)
                }
            }
        }
    }

    /** @param onApplied a conta precisa recarregar: o avatar mudou em toda a tela. */
    fun apply(onApplied: () -> Unit = {}) {
        if (!_uiState.value.hasPreview || _uiState.value.isWorking) return

        _uiState.update { it.copy(isWorking = true, error = null) }

        viewModelScope.launch {
            when (val result = avatars.applyPixel()) {
                is Outcome.Success -> {
                    _uiState.update {
                        it.copy(isWorking = false, preview = null, message = "Avatar atualizado.")
                    }
                    onApplied()
                }

                is Outcome.Failure -> _uiState.update {
                    it.copy(isWorking = false, error = result.error.message)
                }
            }
        }
    }
}
