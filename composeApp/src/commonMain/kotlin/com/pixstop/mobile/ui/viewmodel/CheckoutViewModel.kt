package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.repository.AppConfigRepository
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
    /** Cartão guardado escolhido; o cartão novo espera o SDK do gateway. */
    val savedCardId: Long? = null,
    val installments: Int = 1,
    val maxInstallments: Int = 12,
    val isLoading: Boolean = true,
    val isPlacing: Boolean = false,
    val placedOrder: Order? = null,
    val error: String? = null,
) {
    /**
     * Formas que a empresa aceita agora.
     *
     * Cartão exige gateway conectado e pelo menos um cartão guardado: o app
     * ainda não tokeniza cartão novo, e oferecer a opção sem ter o que enviar
     * daria um erro depois de a pessoa já ter escolhido.
     */
    val availableMethods: List<PaymentMethod>
        get() {
            val checkout = checkout ?: return listOf(PaymentMethod.Balance)

            return buildList {
                add(PaymentMethod.Balance)
                if (checkout.gatewayAvailable) add(PaymentMethod.Money)
                if (checkout.gatewayAvailable && checkout.savedCards.isNotEmpty()) add(PaymentMethod.Card)
            }
        }

    val savedCards: List<com.pixstop.mobile.domain.model.SavedCard>
        get() = checkout?.savedCards.orEmpty()

    /** Parcelar só faz sentido no cartão e com valor a cobrar. */
    val canChooseInstallments: Boolean
        get() = method == PaymentMethod.Card && breakdown.charged > 0

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
        get() = !isPlacing && checkout != null && checkout.itemCount > 0 && !missingMoney &&
            (method != PaymentMethod.Card || savedCardId != null)
}

/**
 * Fechamento do pedido.
 *
 * A conta mostrada é a mesma do servidor, refeita a cada mudança de método ou
 * de pixels. O envio manda só o que a pessoa escolheu — quem decide o valor
 * final é o servidor.
 */
class CheckoutViewModel(
    private val orders: OrderRepository,
    private val config: AppConfigRepository,
) : ViewModel() {

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
                        // O cartão padrão já vem escolhido, como na web: quem
                        // tem um só não precisa escolher nada.
                        savedCardId = (checkout.savedCards.firstOrNull { it.isDefault }
                            ?: checkout.savedCards.firstOrNull())?.id,
                        maxInstallments = config.config.value.maxInstallments,
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
        // Trocar de forma zera o parcelamento: 6× num PIX não quer dizer nada,
        // e voltar ao cartão com a escolha antiga surpreenderia.
        _uiState.value = _uiState.value.copy(method = method, installments = 1, error = null)
    }

    fun onSavedCardChange(id: Long) {
        _uiState.value = _uiState.value.copy(savedCardId = id, error = null)
    }

    fun onInstallmentsChange(value: Int) {
        val state = _uiState.value

        _uiState.value = state.copy(installments = value.coerceIn(1, state.maxInstallments), error = null)
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
                savedCardId = state.savedCardId,
                installments = state.installments,
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
