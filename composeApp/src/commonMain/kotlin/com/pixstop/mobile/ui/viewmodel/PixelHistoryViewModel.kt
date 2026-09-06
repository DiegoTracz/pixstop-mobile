package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.remote.dto.ReferralDto
import com.pixstop.mobile.data.repository.RewardRepository
import com.pixstop.mobile.data.repository.ShopRepository
import com.pixstop.mobile.data.repository.TeamRepository
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.PixelEntry
import com.pixstop.mobile.domain.model.PixelWallet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PixelHistoryUiState(
    val wallet: PixelWallet = PixelWallet.Empty,
    val entries: List<PixelEntry> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val page: Int = 1,
    val hasNextPage: Boolean = false,
    /** O meu link de indicação; nulo onde a empresa não usa indicação. */
    val referral: ReferralDto? = null,
    val error: String? = null,
)

/**
 * Carteira e extrato de pixels.
 *
 * A carteira vem de uma chamada própria porque o extrato é paginado: somar as
 * linhas da primeira página daria um saldo errado.
 */
class PixelHistoryViewModel(
    private val team: TeamRepository,
    private val shop: ShopRepository,
    private val rewards: RewardRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PixelHistoryUiState())
    val uiState: StateFlow<PixelHistoryUiState> = _uiState.asStateFlow()

    init {
        loadReferral()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val wallet = shop.pixelWallet()

            when (val result = team.pixelHistory(page = 1)) {
                is Outcome.Success -> _uiState.value = PixelHistoryUiState(
                    wallet = (wallet as? Outcome.Success)?.value ?: PixelWallet.Empty,
                    entries = result.value.items,
                    isLoading = false,
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

            when (val result = team.pixelHistory(state.page + 1)) {
                is Outcome.Success -> _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    entries = _uiState.value.entries + result.value.items,
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
     * O link de indicação, à parte do extrato: uma empresa sem indicação
     * simplesmente não mostra o cartão, e isso nunca segura a carteira.
     */
    private fun loadReferral() {
        viewModelScope.launch {
            when (val result = rewards.referral()) {
                is Outcome.Success -> _uiState.value = _uiState.value.copy(referral = result.value)
                is Outcome.Failure -> Unit
            }
        }
    }
}
