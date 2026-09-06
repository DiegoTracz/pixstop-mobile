package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.repository.ProgressRepository
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.Progression
import com.pixstop.mobile.domain.model.XpEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProgressUiState(
    val progression: Progression? = null,
    val entries: List<XpEntry> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val page: Int = 1,
    val hasNextPage: Boolean = false,
    val error: String? = null,
)

/**
 * A tela "Meu progresso": a barra por dentro e o histórico de como chegou lá.
 */
class ProgressViewModel(private val repository: ProgressRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val progression = repository.progress()
            val history = repository.history(page = 1)

            _uiState.value = when {
                progression is Outcome.Failure -> ProgressUiState(isLoading = false, error = progression.error.message)
                history is Outcome.Failure -> ProgressUiState(
                    progression = (progression as Outcome.Success).value,
                    isLoading = false,
                    error = history.error.message,
                )

                else -> ProgressUiState(
                    progression = (progression as Outcome.Success).value,
                    entries = (history as Outcome.Success).value.items,
                    isLoading = false,
                    page = history.value.meta.currentPage,
                    hasNextPage = history.value.meta.hasNextPage,
                )
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value

        if (state.isLoadingMore || !state.hasNextPage) {
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)

            when (val result = repository.history(page = state.page + 1)) {
                is Outcome.Success -> _uiState.value = _uiState.value.copy(
                    entries = _uiState.value.entries + result.value.items,
                    isLoadingMore = false,
                    page = result.value.meta.currentPage,
                    hasNextPage = result.value.meta.hasNextPage,
                )

                is Outcome.Failure -> _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }
}
