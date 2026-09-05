package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.repository.OrderRepository
import com.pixstop.mobile.domain.checkout.CheckoutBreakdown
import com.pixstop.mobile.domain.checkout.CheckoutTotals
import com.pixstop.mobile.domain.checkout.PaymentMethod
import com.pixstop.mobile.domain.model.Checkout
import com.pixstop.mobile.domain.model.Order
import com.pixstop.mobile.domain.model.Outcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.min

data class CheckoutUiState(
    val checkout: Checkout? = null,
    val method: PaymentMethod = PaymentMethod.Balance,
    val pixels: Int = 0,
    val useBalance: Boolean = true,
    val isLoading: Boolean = true,
    val isPlacing: Boolean = false,
    val placedOrder: Order? = null,
    val error: String? = null,
) {
    /**
     * Formas que a empresa aceita agora.
     *
     * Cartão só aparece com o gateway conectado e a chave pública no ar;
     * oferecê-lo sem isso levaria a um erro no envio.
     */
    val availableMethods: List<PaymentMethod>
        get() {
            val checkout = checkout ?: return listOf(PaymentMethod.Balance)

            return buildList {
                add(PaymentMethod.Balance)
                if (checkout.gatewayAvailable) add(PaymentMethod.Money)
                if (checkout.cardTokenizationAvailable) add(PaymentMethod.Card)
            }
        }

    val breakdown: CheckoutBreakdown
        get() {
            val checkout = checkout ?: return CheckoutBreakdown(0.0, 0, 0.0, 0.0, 0.0, 0.0, 0.0)

            return CheckoutTotals.calculate(
                products = checkout.productsTotal,
                method = method,
                pixelsToRedeem = pixels,
                balanceToUse = if (useBalance) checkout.walletBalance else 0.0,
                pixelsPerReal = checkout.pixelsPerReal,
                cardFeePercentage = checkout.cardFeePercentage,
                cardFeeFixed = checkout.cardFeeFixed,
            )
        }

    /** Pixels que a pessoa pode usar nesta compra. */
    val maxPixels: Int
        get() {
            val checkout = checkout ?: return 0

            return CheckoutTotals.maxPixelsFor(
                productsTotal = checkout.productsTotal,
                availablePixels = checkout.walletPixels,
                pixelsPerReal = checkout.pixelsPerReal,
                maxDiscountPercentage = checkout.maxDiscountPercentage,
                minRedeem = checkout.minPixelsRedeem,
                enabled = checkout.pixelsEnabled,
            )
        }

    /** Cashback estimado sobre o que sai de saldo e dinheiro, nunca sobre pixels. */
    val estimatedCashback: Int
        get() {
            val checkout = checkout ?: return 0
            val base = breakdown.balance + breakdown.money

            return (base * checkout.cashbackPercentage / 100 * checkout.pixelsPerReal).toInt()
        }

    /**
     * Falta dinheiro quando o método é saldo e o saldo não cobre tudo.
     */
    val missingMoney: Boolean
        get() = method == PaymentMethod.Balance &&
            checkout != null &&
            breakdown.charged > 0

    val canPlace: Boolean
        get() = !isPlacing && checkout != null && checkout.itemCount > 0 && !missingMoney
}

/**
 * Fechamento do pedido.
 *
 * A conta mostrada é a mesma do servidor, refeita a cada mudança de método ou
 * de pixels. O envio manda só o que a pessoa escolheu — quem decide o valor
 * final é o servidor.
 */
class CheckoutViewModel(private val orders: OrderRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = orders.checkout()) {
                is Outcome.Success -> {
                    val checkout = result.value

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        checkout = checkout,
                        // Começa no que a empresa aceita e a pessoa tem.
                        method = if (checkout.walletBalance > 0 || !checkout.gatewayAvailable) {
                            PaymentMethod.Balance
                        } else {
                            PaymentMethod.Money
                        },
                    )
                }

                is Outcome.Failure -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.error.message,
                )
            }
        }
    }

    fun onMethodChange(method: PaymentMethod) {
        _uiState.value = _uiState.value.copy(method = method, error = null)
    }

    fun onPixelsChange(value: Int) {
        val state = _uiState.value

        _uiState.value = state.copy(pixels = min(value, state.maxPixels).coerceAtLeast(0), error = null)
    }

    fun onUseBalanceChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(useBalance = value, error = null)
    }

    fun place() {
        val state = _uiState.value

        if (!state.canPlace) {
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isPlacing = true, error = null)

            val result = orders.place(
                method = state.method,
                pixels = state.pixels,
                balance = state.breakdown.balance,
            )

            _uiState.value = when (result) {
                is Outcome.Success -> _uiState.value.copy(isPlacing = false, placedOrder = result.value)
                is Outcome.Failure -> _uiState.value.copy(isPlacing = false, error = result.error.message)
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
