package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.core.storage.currentTimeMillis
import com.pixstop.mobile.core.text.IsoInstant
import com.pixstop.mobile.data.payment.CardTokenizer
import com.pixstop.mobile.data.repository.AppConfigRepository
import com.pixstop.mobile.data.repository.OrderRepository
import com.pixstop.mobile.domain.checkout.CheckoutBreakdown
import com.pixstop.mobile.domain.checkout.CheckoutTotals
import com.pixstop.mobile.domain.checkout.PaymentMethod
import com.pixstop.mobile.domain.model.Checkout
import com.pixstop.mobile.domain.model.Order
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.payment.CardInput
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
    val cardMode: CardMode = CardMode.Saved,
    val savedCardId: Long? = null,
    val card: CardInput = CardInput("", "", "", "", ""),
    val cardErrors: Map<String, String> = emptyMap(),
    /** Guardar o cartão novo para as próximas compras, como na web. */
    val saveCard: Boolean = true,
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
     * Cartão exige gateway conectado, e ou um cartão guardado ou a chave
     * pública que permite tokenizar um novo. Oferecer sem nenhum dos dois daria
     * erro depois de a pessoa já ter escolhido.
     */
    val availableMethods: List<PaymentMethod>
        get() {
            val checkout = checkout ?: return listOf(PaymentMethod.Balance)

            return buildList {
                add(PaymentMethod.Balance)
                if (checkout.gatewayAvailable) add(PaymentMethod.Money)
                if (canPayWithCard) add(PaymentMethod.Card)
            }
        }

    private val canPayWithCard: Boolean
        get() {
            val checkout = checkout ?: return false

            return checkout.gatewayAvailable &&
                (checkout.savedCards.isNotEmpty() || checkout.cardTokenizationAvailable)
        }

    /** Sem cartão guardado não há o que escolher: só resta cadastrar um. */
    val cardModes: List<CardMode>
        get() = buildList {
            if (savedCards.isNotEmpty()) add(CardMode.Saved)
            if (checkout?.cardTokenizationAvailable == true) add(CardMode.New)
        }

    val usingNewCard: Boolean get() = method == PaymentMethod.Card && cardMode == CardMode.New

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
        get() {
            if (isPlacing || checkout == null || checkout.itemCount == 0 || missingMoney) {
                return false
            }

            return when {
                method != PaymentMethod.Card -> true
                cardMode == CardMode.Saved -> savedCardId != null
                // O cartão novo é conferido no envio; segurar o botão até o
                // último dígito esconderia da pessoa o que falta preencher.
                else -> true
            }
        }
}

/**
 * Fechamento do pedido.
 *
 * A conta mostrada é a mesma do servidor, refeita a cada mudança de método ou
 * de pixels. O envio manda só o que a pessoa escolheu — quem decide o valor
 * final é o servidor.
 */
enum class CardMode(val label: String) {
    Saved("Cartão salvo"),
    New("Novo cartão"),
}

class CheckoutViewModel(
    private val orders: OrderRepository,
    private val config: AppConfigRepository,
    private val cards: CardTokenizer,
    private val now: () -> Long = ::currentTimeMillis,
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
                        // Quem já tem cartão guardado começa nele; quem não tem
                        // já cai no formulário, sem um passo a mais.
                        cardMode = if (checkout.savedCards.isEmpty()) CardMode.New else CardMode.Saved,
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

    fun onCardModeChange(mode: CardMode) {
        _uiState.value = _uiState.value.copy(cardMode = mode, error = null, cardErrors = emptyMap())
    }

    fun onCardChange(card: CardInput) {
        val state = _uiState.value

        // Some o erro do campo que está sendo corrigido, e só dele: apagar
        // todos faria a lista piscar a cada tecla.
        _uiState.value = state.copy(
            card = card,
            cardErrors = state.cardErrors.filterKeys { field -> field !in card.changedFieldsFrom(state.card) },
            error = null,
        )
    }

    fun onSaveCardChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(saveCard = value)
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

    /**
     * Fecha o pedido.
     *
     * O cartão novo passa antes pelo gateway: o número sai do aparelho, volta
     * um token de uso único, e é o token que o nosso servidor recebe. Falhar
     * aqui não cria pedido nenhum.
     */
    fun place() {
        val state = _uiState.value

        if (!state.canPlace) {
            return
        }

        viewModelScope.launch {
            if (state.usingNewCard) {
                val errors = state.card.errors(currentMonth(), currentYear())

                if (errors.isNotEmpty()) {
                    _uiState.value = state.copy(cardErrors = errors)

                    return@launch
                }
            }

            _uiState.value = state.copy(isPlacing = true, error = null, cardErrors = emptyMap())

            val token = if (state.usingNewCard) {
                when (val tokenized = cards.tokenize(state.card)) {
                    is Outcome.Success -> tokenized.value
                    is Outcome.Failure -> {
                        _uiState.value = _uiState.value.copy(isPlacing = false, error = tokenized.error.message)

                        return@launch
                    }
                }
            } else {
                null
            }

            val result = orders.place(
                method = state.method,
                pixels = state.pixels,
                balance = state.breakdown.balance,
                savedCardId = state.savedCardId.takeIf { state.cardMode == CardMode.Saved },
                installments = state.installments,
                cardToken = token,
                documentType = state.card.documentType.takeIf { token != null },
                documentNumber = state.card.document.takeIf { token != null },
                saveCard = state.saveCard.takeIf { token != null },
            )

            _uiState.value = when (result) {
                is Outcome.Success -> _uiState.value.copy(isPlacing = false, placedOrder = result.value)
                is Outcome.Failure -> _uiState.value.copy(isPlacing = false, error = result.error.message)
            }
        }
    }

    /**
     * Mês e ano de hoje pelo relógio do aparelho.
     *
     * Serve só para recusar um cartão obviamente vencido antes de gastar uma
     * ida à rede; quem decide de verdade é o emissor, então um relógio
     * adiantado nunca custa uma venda.
     */
    private fun currentMonth(): Int = IsoInstant.yearMonthOf(now()).second

    private fun currentYear(): Int = IsoInstant.yearMonthOf(now()).first

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
