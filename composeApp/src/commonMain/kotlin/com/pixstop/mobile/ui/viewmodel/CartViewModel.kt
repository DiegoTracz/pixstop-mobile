package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.core.storage.SessionStore
import com.pixstop.mobile.core.storage.currentTimeMillis
import com.pixstop.mobile.data.repository.CartRepository
import com.pixstop.mobile.domain.model.Cart
import com.pixstop.mobile.domain.model.Outcome
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CartUiState(
    val cart: Cart = Cart.Empty,
    val isLoading: Boolean = false,
    /** Item em que uma alteração está em curso, para travar só os botões dele. */
    val busyItemId: Long? = null,
    val error: String? = null,
    val message: String? = null,
    /** Instante do relógio do aparelho, para o contador da reserva. */
    val now: Long = 0L,
) {
    val itemCount: Int get() = cart.totalItems

    /**
     * Segundos que faltam para a reserva mais curta vencer.
     *
     * É a mais curta porque é ela que decide quando o carrinho começa a
     * encolher — avisar pela mais longa daria uma falsa sensação de folga.
     */
    val secondsLeft: Long?
        get() {
            val earliest = cart.items.mapNotNull { it.reservedUntil }.minOrNull() ?: return null
            val remaining = (earliest - now) / 1_000L

            // O relógio do aparelho não é o do servidor. Sem o teto, alguns
            // segundos de adiantamento fariam a tela prometer mais tempo do
            // que a reserva pode durar.
            return remaining.coerceIn(0, cart.reservationMinutes * 60L)
        }

    val isExpired: Boolean get() = secondsLeft == 0L && cart.items.isNotEmpty()
}

/**
 * Carrinho.
 *
 * Toda alteração recarrega o carrinho inteiro em vez de emendar o estado
 * local: a reserva vence sozinha no servidor, e um carrinho remendado ficaria
 * mostrando item que já não existe.
 */
class CartViewModel(
    private val cart: CartRepository,
    private val session: SessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    init {
        // Nasce com a navegação, que existe antes do login: buscar sem token
        // daria um 401, que o cliente HTTP trata como sessão vencida.
        if (session.isLoggedIn()) {
            refresh()
        }

        startTicking()
    }

    /**
     * Esquece o carrinho carregado.
     *
     * O carrinho é da empresa ativa. Ao trocar de empresa, manter o anterior
     * na tela ofereceria itens que não existem mais para quem está olhando.
     */
    fun clear() {
        _uiState.value = CartUiState()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = cart.cart()) {
                is Outcome.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    cart = result.value,
                    now = currentTimeMillis(),
                )

                is Outcome.Failure -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.error.message,
                )
            }
        }
    }

    fun add(productId: Long, quantity: Int = 1, onAdded: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = null, message = null)

            when (val result = cart.add(productId, quantity)) {
                is Outcome.Success -> {
                    reload()
                    _uiState.value = _uiState.value.copy(message = "Adicionado ao carrinho.")
                    onAdded()
                }

                is Outcome.Failure -> _uiState.value = _uiState.value.copy(error = result.error.message)
            }
        }
    }

    fun changeQuantity(itemId: Long, quantity: Int) {
        if (quantity < 1) {
            remove(itemId)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busyItemId = itemId, error = null, message = null)

            val result = cart.updateQuantity(itemId, quantity)

            if (result is Outcome.Failure) {
                _uiState.value = _uiState.value.copy(error = result.error.message)
            }

            reload()
            _uiState.value = _uiState.value.copy(busyItemId = null)
        }
    }

    fun remove(itemId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busyItemId = itemId, error = null, message = null)

            val result = cart.remove(itemId)

            if (result is Outcome.Failure) {
                _uiState.value = _uiState.value.copy(error = result.error.message)
            }

            reload()
            _uiState.value = _uiState.value.copy(busyItemId = null)
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    private suspend fun reload() {
        when (val result = cart.cart()) {
            is Outcome.Success -> _uiState.value = _uiState.value.copy(cart = result.value, now = currentTimeMillis())
            is Outcome.Failure -> _uiState.value = _uiState.value.copy(error = result.error.message)
        }
    }

    /**
     * Move o relógio do contador de segundo em segundo.
     *
     * Ao chegar a zero recarrega uma vez: o servidor já devolveu o estoque, e
     * a tela precisa deixar de oferecer o que não está mais reservado.
     */
    private fun startTicking() {
        viewModelScope.launch {
            while (true) {
                delay(1_000)

                val before = _uiState.value.secondsLeft
                _uiState.value = _uiState.value.copy(now = currentTimeMillis())

                if (before != null && before > 0 && _uiState.value.secondsLeft == 0L) {
                    reload()
                }
            }
        }
    }
}
