package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.repository.NotificationRepository
import com.pixstop.mobile.domain.model.AppNotification
import com.pixstop.mobile.domain.model.Outcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val items: List<AppNotification> = emptyList(),
    val unread: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val page: Int = 1,
    val hasNextPage: Boolean = false,
    val error: String? = null,
) {
    /**
     * Marca um aviso como lido e desconta da contagem.
     *
     * É função pura para o desfazer ser trivial: guarda-se o estado anterior e
     * devolve-se ele inteiro se o servidor recusar.
     */
    fun withRead(id: String): NotificationsUiState {
        if (items.none { it.id == id && !it.read }) {
            return this
        }

        return copy(
            items = items.map { if (it.id == id) it.copy(read = true) else it },
            unread = (unread - 1).coerceAtLeast(0),
        )
    }

    fun withAllRead(): NotificationsUiState =
        if (unread == 0) this else copy(items = items.map { it.copy(read = true) }, unread = 0)
}

/**
 * Caixa de avisos e o badge da barra inferior.
 *
 * A contagem sai do rodapé da listagem, que o servidor manda junto — o badge
 * não custa uma chamada própria.
 */
class NotificationsViewModel(private val notifications: NotificationRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = notifications.list(page = 1)) {
                is Outcome.Success -> _uiState.value = NotificationsUiState(
                    items = result.value.items,
                    unread = result.value.meta.unread ?: 0,
                    page = result.value.meta.currentPage,
                    hasNextPage = result.value.meta.hasNextPage,
                )

                is Outcome.Failure -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.error.message,
                )
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value

        if (state.isLoading || state.isLoadingMore || !state.hasNextPage) {
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)

            when (val result = notifications.list(page = state.page + 1)) {
                is Outcome.Success -> _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    items = _uiState.value.items + result.value.items,
                    unread = result.value.meta.unread ?: _uiState.value.unread,
                    page = result.value.meta.currentPage,
                    hasNextPage = result.value.meta.hasNextPage,
                )

                is Outcome.Failure -> _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    error = result.error.message,
                )
            }
        }
    }

    /**
     * Marca como lida na tela antes de o servidor confirmar e desfaz se ele
     * recusar: esperar a resposta para apagar um ponto deixaria a lista
     * parecendo travada.
     */
    fun markRead(id: String) {
        val before = _uiState.value
        val after = before.withRead(id)

        if (after === before) {
            return
        }

        _uiState.value = after

        viewModelScope.launch {
            if (notifications.markRead(id) is Outcome.Failure) {
                rollback(before)
            }
        }
    }

    fun markAllRead() {
        val before = _uiState.value
        val after = before.withAllRead()

        if (after === before) {
            return
        }

        _uiState.value = after

        viewModelScope.launch {
            if (notifications.markAllRead() is Outcome.Failure) {
                rollback(before)
            }
        }
    }

    /**
     * Devolve só o que a marcação otimista mexeu. Copiar o estado inteiro
     * apagaria uma página que tivesse chegado enquanto a chamada corria.
     */
    private fun rollback(before: NotificationsUiState) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.map { current ->
                before.items.firstOrNull { it.id == current.id }?.let { current.copy(read = it.read) } ?: current
            },
            unread = before.unread,
        )
    }
}
