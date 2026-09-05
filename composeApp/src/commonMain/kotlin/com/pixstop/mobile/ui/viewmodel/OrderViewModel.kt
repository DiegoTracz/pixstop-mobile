package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.repository.OrderRepository
import com.pixstop.mobile.domain.model.Order
import com.pixstop.mobile.domain.model.OrderStatus
import com.pixstop.mobile.domain.model.Outcome
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OrderUiState(
    val order: Order? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * Um pedido e, quando ele espera PIX, a consulta periódica do desfecho.
 *
 * A consulta existe porque quem está diante da geladeira com o celular na mão
 * não pode depender de um webhook que talvez demore — o servidor pergunta ao
 * gateway e confirma na hora.
 */
class OrderViewModel(private val orders: OrderRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    fun load(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = orders.order(id)) {
                is Outcome.Success -> {
                    _uiState.value = OrderUiState(order = result.value, isLoading = false)
                    startPollingIfWaiting(result.value)
                }

                is Outcome.Failure -> _uiState.value = OrderUiState(isLoading = false, error = result.error.message)
            }
        }
    }

    /**
     * Só vale consultar enquanto há PIX pendente. Um pedido já pago ou pago com
     * saldo não muda mais sozinho.
     */
    private fun startPollingIfWaiting(order: Order) {
        pollingJob?.cancel()

        if (!order.isPending || order.pix == null) {
            return
        }

        pollingJob = viewModelScope.launch {
            while (true) {
                delay(POLL_INTERVAL_MILLIS)

                val result = orders.status(order.id)

                if (result is Outcome.Success && result.value != OrderStatus.Pending) {
                    // Mudou de estado: recarrega o pedido inteiro, que traz o
                    // rótulo e os totais definitivos.
                    load(order.id)
                    return@launch
                }
            }
        }
    }

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }

    companion object {
        /** Cinco segundos: rápido para quem espera, leve para o gateway. */
        const val POLL_INTERVAL_MILLIS = 5_000L
    }
}

data class OrdersUiState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * Histórico de pedidos.
 */
class OrdersViewModel(private val orders: OrderRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = orders.orders()) {
                is Outcome.Success -> _uiState.value = OrdersUiState(orders = result.value.items, isLoading = false)
                is Outcome.Failure -> _uiState.value = OrdersUiState(isLoading = false, error = result.error.message)
            }
        }
    }
}
