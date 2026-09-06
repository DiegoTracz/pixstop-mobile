package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.remote.dto.RewardDto
import com.pixstop.mobile.data.remote.dto.VoucherDto
import com.pixstop.mobile.data.repository.RewardRepository
import com.pixstop.mobile.domain.model.Outcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RewardsUiState(
    val available: Int = 0,
    val rewards: List<RewardDto> = emptyList(),
    val vouchers: List<VoucherDto> = emptyList(),
    val isLoading: Boolean = true,
    /** A recompensa que a pessoa tocou e ainda não confirmou. */
    val confirming: RewardDto? = null,
    val redeemingId: Long? = null,
    /** O voucher aberto na tela, com o código grande para o balcão. */
    val openVoucher: VoucherDto? = null,
    val error: String? = null,
) {
    /** Os válidos primeiro: é o que a pessoa vai mostrar. */
    val sortedVouchers: List<VoucherDto> get() = vouchers.sortedBy { if (it.isOpen) 0 else 1 }
}

/**
 * "Recompensas": o que os pixels compram onde não há loja. O resgate tira os
 * pixels na hora — por isso pede confirmação — e o voucher abre em seguida.
 */
class RewardsViewModel(private val repository: RewardRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RewardsUiState())
    val uiState: StateFlow<RewardsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            _uiState.value = when (val result = repository.page()) {
                is Outcome.Success -> _uiState.value.copy(
                    available = result.value.available,
                    rewards = result.value.rewards,
                    vouchers = result.value.vouchers,
                    isLoading = false,
                )

                is Outcome.Failure -> _uiState.value.copy(isLoading = false, error = result.error.message)
            }
        }
    }

    fun askRedeem(reward: RewardDto) {
        if (reward.affordable && reward.available) {
            _uiState.value = _uiState.value.copy(confirming = reward, error = null)
        }
    }

    fun dismissRedeem() {
        _uiState.value = _uiState.value.copy(confirming = null)
    }

    fun confirmRedeem() {
        val reward = _uiState.value.confirming ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(confirming = null, redeemingId = reward.id, error = null)

            when (val result = repository.redeem(reward.id)) {
                is Outcome.Success -> {
                    _uiState.value = _uiState.value.copy(redeemingId = null, openVoucher = result.value)
                    refresh()
                }

                is Outcome.Failure -> _uiState.value = _uiState.value.copy(redeemingId = null, error = result.error.message)
            }
        }
    }

    fun openVoucher(voucher: VoucherDto?) {
        _uiState.value = _uiState.value.copy(openVoucher = voucher)
    }
}
