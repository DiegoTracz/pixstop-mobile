package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.core.image.downscaleJpeg
import com.pixstop.mobile.data.repository.AssistantRepository
import com.pixstop.mobile.domain.model.ChatConversation
import com.pixstop.mobile.domain.model.ChatMessage
import com.pixstop.mobile.domain.model.ChatRole
import com.pixstop.mobile.domain.model.ChatThread
import com.pixstop.mobile.domain.model.DomainError
import com.pixstop.mobile.domain.model.MascotState
import com.pixstop.mobile.domain.model.Outcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Uma foto já enviada ao servidor, esperando a mensagem; a prévia é só para a tela. */
data class PendingPhoto(val id: Long, val preview: ByteArray) {
    override fun equals(other: Any?): Boolean = other is PendingPhoto && other.id == id

    override fun hashCode(): Int = id.hashCode()
}

data class AssistantUiState(
    val conversationId: Long? = null,
    val conversations: List<ChatConversation> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val photos: List<PendingPhoto> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val isUploading: Boolean = false,
    val showHistory: Boolean = false,
    val error: String? = null,
) {
    val canSend: Boolean get() = draft.isNotBlank() && !isSending && !isUploading

    val canAddPhoto: Boolean get() = photos.size < MAX_PHOTOS && !isUploading && !isSending

    /** O mascote pensa enquanto a IA responde; é o único sinal de espera do chat. */
    val mascot: MascotState get() = if (isSending || isUploading) MascotState.Loading else MascotState.Idle

    val isEmpty: Boolean get() = messages.isEmpty() && !isLoading

    companion object {
        const val MAX_PHOTOS = 4

        val SUGGESTIONS = listOf(
            "Quem não compra há duas semanas?",
            "Como foram as vendas esta semana?",
            "O que está zerado?",
            "Qual geladeira vende menos?",
        )
    }
}

/**
 * O chat do assistente de IA no celular (ASSISTENTE_IA.md, A8): o mesmo do
 * painel, com a câmera para fotografar o produto na prateleira.
 */
class AssistantViewModel(
    private val repository: AssistantRepository,
    private val shrink: (ByteArray) -> ByteArray = { downscaleJpeg(it) },
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val outcome = repository.conversations()) {
                is Outcome.Success -> {
                    _uiState.update { it.copy(conversations = outcome.value, isLoading = false) }
                    outcome.value.firstOrNull()?.let { open(it.id) }
                }
                is Outcome.Failure -> _uiState.update { it.copy(isLoading = false, error = message(outcome.error)) }
            }
        }
    }

    fun onDraft(text: String) = _uiState.update { it.copy(draft = text, error = null) }

    fun toggleHistory() = _uiState.update { it.copy(showHistory = !it.showHistory) }

    fun newConversation() = _uiState.update {
        it.copy(conversationId = null, messages = emptyList(), photos = emptyList(), showHistory = false, error = null)
    }

    fun open(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showHistory = false, error = null) }
            when (val outcome = repository.conversation(id)) {
                is Outcome.Success -> _uiState.update { it.copy(conversationId = id, messages = outcome.value.messages, isLoading = false) }
                is Outcome.Failure -> _uiState.update { it.copy(isLoading = false, error = message(outcome.error)) }
            }
        }
    }

    fun send(text: String? = null) {
        val state = _uiState.value
        val content = (text ?: state.draft).trim()

        if (content.isEmpty() || state.isSending || state.isUploading) return

        // A fala aparece na hora; a resposta do servidor a substitui.
        val optimistic = ChatMessage(id = -1, role = ChatRole.User, text = content, photos = state.photos.size)
        val photoIds = state.photos.map { it.id }
        _uiState.update { it.copy(messages = it.messages + optimistic, draft = "", photos = emptyList(), isSending = true, error = null) }

        viewModelScope.launch {
            when (val outcome = repository.send(state.conversationId, content, photoIds)) {
                is Outcome.Success -> _uiState.update { current -> current.received(outcome.value, appended = true) }
                is Outcome.Failure -> _uiState.update {
                    it.copy(messages = it.messages - optimistic, draft = content, isSending = false, error = message(outcome.error))
                }
            }
        }
    }

    fun decide(toolName: String, token: String, confirm: Boolean) {
        val conversationId = _uiState.value.conversationId ?: return

        if (_uiState.value.isSending) return

        _uiState.update { it.copy(isSending = true, error = null) }
        viewModelScope.launch {
            when (val outcome = repository.decide(conversationId, toolName, token, confirm)) {
                is Outcome.Success -> _uiState.update { it.received(outcome.value, appended = false) }
                is Outcome.Failure -> _uiState.update { it.copy(isSending = false, error = message(outcome.error)) }
            }
        }
    }

    /** A foto sobe reduzida, antes da mensagem; a mensagem leva só o id. */
    fun onPhoto(photo: ByteArray) {
        if (!_uiState.value.canAddPhoto) return

        _uiState.update { it.copy(isUploading = true, error = null) }
        viewModelScope.launch {
            val jpeg = withContext(Dispatchers.Default) { shrink(photo) }
            when (val outcome = repository.uploadPhoto(jpeg)) {
                is Outcome.Success -> _uiState.update {
                    it.copy(
                        photos = it.photos + PendingPhoto(outcome.value.id, jpeg),
                        draft = it.draft.ifBlank { "Cadastre este produto." },
                        isUploading = false,
                    )
                }
                is Outcome.Failure -> _uiState.update { it.copy(isUploading = false, error = message(outcome.error)) }
            }
        }
    }

    fun removePhoto(id: Long) = _uiState.update { it.copy(photos = it.photos.filterNot { photo -> photo.id == id }) }

    private fun AssistantUiState.received(thread: ChatThread, appended: Boolean): AssistantUiState {
        val conversation = thread.conversation
        val known = conversations.any { it.id == conversation?.id }

        return copy(
            conversationId = conversation?.id ?: conversationId,
            conversations = if (conversation != null && !known) listOf(conversation) + conversations else conversations,
            // O envio devolve só as mensagens novas; o botão do card, a conversa inteira.
            messages = if (appended) messages.filter { it.id > 0 } + thread.messages else thread.messages,
            isSending = false,
        )
    }

    private fun message(error: DomainError): String = when (error) {
        is DomainError.Offline -> "Sem conexão. Tente de novo quando a internet voltar."
        else -> error.message ?: "Não deu certo. Tente de novo."
    }
}
