package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.core.storage.currentTimeMillis
import com.pixstop.mobile.core.text.IsoInstant
import com.pixstop.mobile.data.payment.CardTokenizer
import com.pixstop.mobile.data.repository.ShopRepository
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.TopupMethod
import com.pixstop.mobile.domain.model.TopupOffer
import com.pixstop.mobile.domain.model.WalletTopup
import com.pixstop.mobile.domain.payment.CardInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Em que etapa da compra a pessoa está. */
enum class BuyPixelsStep { Choose, Card, Pix, Done }

data class BuyPixelsUiState(
    val offer: TopupOffer = TopupOffer.Off,
    val isLoading: Boolean = true,
    val step: BuyPixelsStep = BuyPixelsStep.Choose,
    /** Um dos valores prontos, em reais. */
    val preset: Int? = null,
    /** O campo livre; vence o valor pronto quando preenchido. */
    val customText: String = "",
    val method: TopupMethod = TopupMethod.Pix,
    val card: CardInput = CardInput("", "", "", "", ""),
    val cardErrors: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    val topup: WalletTopup? = null,
    val error: String? = null,
    val now: Long = 0L,
) {
    /** O valor escolhido, em reais, ou nulo se o campo livre não é um número. */
    val reais: Int? get() = if (customText.isNotBlank()) customText.trim().toIntOrNull() else preset

    val isValid: Boolean get() = offer.accepts(reais)

    val pixels: Int get() = if (isValid) (reais ?: 0) * offer.pixelsPerReal else 0

    /** O que sai do bolso: o valor, mais a taxa do cartão quando é cartão. */
    val charged: Double
        get() {
            val value = reais ?: return 0.0
            return if (method == TopupMethod.Card) value + offer.cardFeeFor(value) else value.toDouble()
        }

    /** Segundos até o Pix vencer. */
    val pixSecondsLeft: Long?
        get() {
            val expiresAt = topup?.pixExpiresAt ?: return null
            if (now == 0L) return null
            return ((expiresAt - now) / 1_000L).coerceAtLeast(0)
        }
}

/**
 * Comprar pixels para a carteira (docs/plans/CARTEIRA_PIXELS.md no servidor, P2).
 *
 * R$ 1 compra 100 pixels; os comprados pagam a compra inteira e não vencem.
 * O cartão é tokenizado aqui, como no checkout: o número não passa pelo
 * servidor. O Pix fica perguntando o status até cair; quem credita é o
 * servidor, uma vez só.
 */
class BuyPixelsViewModel(
    private val shop: ShopRepository,
    private val cards: CardTokenizer,
    private val now: () -> Long = ::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BuyPixelsUiState())
    val uiState: StateFlow<BuyPixelsUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var tickingJob: Job? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = shop.pixelWallet()) {
                is Outcome.Success -> {
                    val offer = result.value.topup
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        offer = offer,
                        // O do meio dos prontos é o mais escolhido; sem prontos, o mínimo.
                        preset = offer.presets.getOrNull(1) ?: offer.presets.firstOrNull() ?: offer.min,
                    )
                }

                is Outcome.Failure -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.error.message)
            }
        }
    }

    fun pick(reais: Int) {
        _uiState.value = _uiState.value.copy(preset = reais, customText = "", error = null)
    }

    fun onCustomChange(text: String) {
        _uiState.value = _uiState.value.copy(customText = text.filter(Char::isDigit).take(4), error = null)
    }

    fun onMethodChange(method: TopupMethod) {
        _uiState.value = _uiState.value.copy(method = method, error = null)
    }

    fun onCardChange(card: CardInput) {
        val state = _uiState.value
        _uiState.value = state.copy(
            card = card,
            cardErrors = state.cardErrors.filterKeys { field -> field !in card.changedFieldsFrom(state.card) },
            error = null,
        )
    }

    /** Da escolha para o pagamento: o cartão abre o formulário, o Pix já gera o código. */
    fun next() {
        val state = _uiState.value
        if (!state.isValid || state.isSubmitting) return

        if (state.method == TopupMethod.Card) {
            _uiState.value = state.copy(step = BuyPixelsStep.Card, error = null)
            return
        }

        submit()
    }

    fun back() {
        stopPolling()
        _uiState.value = _uiState.value.copy(step = BuyPixelsStep.Choose, error = null, topup = null)
    }

    fun payWithCard() {
        val state = _uiState.value
        if (state.isSubmitting) return

        val (year, month) = IsoInstant.yearMonthOf(now())
        val errors = state.card.errors(month, year)
        if (errors.isNotEmpty()) {
            _uiState.value = state.copy(cardErrors = errors)
            return
        }

        submit()
    }

    private fun submit() {
        val state = _uiState.value
        val reais = state.reais ?: return

        viewModelScope.launch {
            _uiState.value = state.copy(isSubmitting = true, error = null, cardErrors = emptyMap())

            var token: String? = null
            if (state.method == TopupMethod.Card) {
                when (val tokenized = cards.tokenize(state.card)) {
                    is Outcome.Success -> token = tokenized.value
                    is Outcome.Failure -> {
                        _uiState.value = _uiState.value.copy(isSubmitting = false, error = tokenized.error.message)
                        return@launch
                    }
                }
            }

            val result = shop.buyPixels(
                reais = reais,
                method = state.method,
                cardToken = token,
                documentType = token?.let { state.card.documentType },
                documentNumber = token?.let { state.card.document },
            )

            when (result) {
                is Outcome.Success -> {
                    _uiState.value = _uiState.value.copy(isSubmitting = false)
                    follow(result.value)
                }

                is Outcome.Failure -> _uiState.value = _uiState.value.copy(isSubmitting = false, error = result.error.message)
            }
        }
    }

    /** O que o servidor disse sobre a compra decide a etapa. */
    private fun follow(topup: WalletTopup) {
        val state = _uiState.value

        when {
            topup.credited -> {
                stopPolling()
                _uiState.value = state.copy(step = BuyPixelsStep.Done, topup = topup)
            }

            topup.refused -> {
                stopPolling()
                _uiState.value = state.copy(
                    step = if (topup.method == TopupMethod.Card) BuyPixelsStep.Card else BuyPixelsStep.Choose,
                    topup = null,
                    error = if (topup.method == TopupMethod.Card) {
                        "O cartão foi recusado. Confira os dados ou tente outro cartão."
                    } else {
                        "O Pix não foi concluído. Gere um novo código."
                    },
                )
            }

            topup.method == TopupMethod.Pix -> {
                _uiState.value = state.copy(step = BuyPixelsStep.Pix, topup = topup, now = now())
                startPolling(topup.id)
            }
        }
    }

    private fun startPolling(id: Long) {
        if (pollingJob?.isActive == true) return

        pollingJob = viewModelScope.launch {
            while (true) {
                delay(POLL_INTERVAL_MILLIS)
                val result = shop.pixelTopup(id)
                if (result is Outcome.Success) follow(result.value)
            }
        }

        tickingJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                _uiState.value = _uiState.value.copy(now = now())
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        tickingJob?.cancel()
        pollingJob = null
        tickingJob = null
    }

    override fun onCleared() {
        stopPolling()
        super.onCleared()
    }

    private companion object {
        const val POLL_INTERVAL_MILLIS = 4_000L
    }
}
