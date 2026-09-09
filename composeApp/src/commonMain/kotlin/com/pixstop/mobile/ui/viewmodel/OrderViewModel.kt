package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.core.storage.currentTimeMillis
import com.pixstop.mobile.data.repository.AppConfigRepository
import com.pixstop.mobile.data.repository.OrderRepository
import com.pixstop.mobile.domain.access.FridgeUnlockConnector
import com.pixstop.mobile.domain.model.BluetoothUnlockResult
import com.pixstop.mobile.domain.model.Order
import com.pixstop.mobile.domain.model.OrderStatus
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.Pickup
import com.pixstop.mobile.domain.model.PickupReason
import com.pixstop.mobile.domain.model.PickupStatus
import com.pixstop.mobile.domain.model.UnlockTicket
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
    val now: Long = 0,
    val pixExpirationMinutes: Int = 30,
    /** A retirada (Fase 9.8), acompanhada à parte do pedido. */
    val pickup: Pickup = Pickup.None,
    /** O bilhete de abertura por Bluetooth (Fase 9.7), quando há. */
    val ticket: UnlockTicket? = null,
    /** Uma tentativa de abrir está em curso. */
    val isUnlocking: Boolean = false,
    /** O celular está falando com a geladeira pelo rádio. */
    val isTalkingToFridge: Boolean = false,
    /** Este aparelho tem rádio para abrir sem internet. */
    val bluetoothSupported: Boolean = false,
    /** O que dizer depois de uma tentativa: some no próximo toque. */
    val feedback: String? = null,
) {
    /**
     * Segundos que faltam para o PIX vencer.
     *
     * O teto existe pelo mesmo motivo do carrinho: o relógio do aparelho não é
     * o do servidor, e alguns minutos de adiantamento fariam a tela prometer
     * mais tempo do que o gateway vai aceitar.
     */
    val secondsLeft: Long?
        get() {
            val expiresAt = order?.pix?.expiresAt ?: return null
            if (now == 0L) {
                return null
            }

            return ((expiresAt - now) / 1_000L).coerceIn(0, pixExpirationMinutes * 60L)
        }

    val isExpired: Boolean get() = secondsLeft == 0L

    /** Segundos que faltam para a porta parar de responder a esta tentativa. */
    val unlockSecondsLeft: Long?
        get() {
            val until = pickup.windowUntil ?: return null
            if (now == 0L) {
                return null
            }

            return ((until - now) / 1_000L).coerceAtLeast(0)
        }

    /** O botão de abrir aparece, e nenhuma tentativa está em curso. */
    val canUnlock: Boolean get() = pickup.canUnlock && !isUnlocking && !isTalkingToFridge

    /**
     * O Bluetooth só é oferecido quando resolve alguma coisa: há bilhete, o
     * aparelho tem rádio, e a geladeira não está respondendo pela internet —
     * ou uma tentativa acabou de sair e pode não ter chegado. Fora disso, o
     * caminho normal é melhor, e é o que a pessoa espera.
     */
    val canUnlockByBluetooth: Boolean
        get() = bluetoothSupported &&
            ticket != null &&
            !isUnlocking &&
            !isTalkingToFridge &&
            !pickup.isDone &&
            (pickup.reason == PickupReason.Offline || pickup.status == PickupStatus.Unlocking)

    /**
     * A frase da retirada. Mora aqui, e não na tela, porque é ela que muda a
     * cada estado — e é o que se confere sem celular nenhum.
     */
    val pickupMessage: String
        get() = when (pickup.status) {
            PickupStatus.PickedUp -> "Produto retirado."
            PickupStatus.Opened -> "A porta abriu. Pode retirar o produto."
            PickupStatus.Manual -> "Retire o produto com o responsável."
            PickupStatus.None -> "Pode retirar o produto."
            PickupStatus.Unlocking -> {
                val left = unlockSecondsLeft

                if (left != null && left > 0) "Abrindo! Puxe a porta da geladeira ($left s)." else "A porta não abriu. Toque para tentar de novo."
            }

            PickupStatus.Awaiting -> when (pickup.reason) {
                PickupReason.Busy -> "Alguém está usando a geladeira agora. Aguarde a porta fechar."
                PickupReason.Offline -> if (pickup.hasTicket) {
                    "A geladeira está sem internet. Abra por Bluetooth, aqui do lado dela."
                } else {
                    "A geladeira está sem internet. Procure o responsável pela empresa."
                }

                PickupReason.Exhausted -> "Você já tentou abrir o máximo de vezes. Procure o responsável."
                null -> "Quando estiver na frente da geladeira, toque para abrir."
            }
        }
}

/**
 * Um pedido e, quando ele espera PIX, a consulta periódica do desfecho.
 *
 * A consulta existe porque quem está diante da geladeira com o celular na mão
 * não pode depender de um webhook que talvez demore — o servidor pergunta ao
 * gateway e confirma na hora.
 */
class OrderViewModel(
    private val orders: OrderRepository,
    private val config: AppConfigRepository,
    private val fridge: FridgeUnlockConnector,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var tickingJob: Job? = null
    private var pickupJob: Job? = null

    fun load(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = orders.order(id)) {
                is Outcome.Success -> {
                    _uiState.value = OrderUiState(
                        order = result.value,
                        isLoading = false,
                        now = currentTimeMillis(),
                        pixExpirationMinutes = config.config.value.pixExpirationMinutes,
                        pickup = result.value.pickup,
                        ticket = result.value.ticket,
                        bluetoothSupported = fridge.isSupported,
                    )
                    startPollingIfWaiting(result.value)
                    startFollowingPickup(result.value)
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
        tickingJob?.cancel()

        if (!order.isPending || order.pix == null) {
            return
        }

        startTicking()

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

    /**
     * "Abrir a geladeira" (Fase 9.8): a pessoa chegou na frente da porta.
     *
     * A recusa também traz o estado, então tanto faz o desfecho: a tela
     * termina sabendo o que mostrar e por quê.
     */
    fun unlock() {
        val id = _uiState.value.order?.id ?: return

        if (!_uiState.value.canUnlock) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUnlocking = true, feedback = null)

            when (val result = orders.unlock(id)) {
                is Outcome.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isUnlocking = false,
                        pickup = result.value.pickup,
                        ticket = result.value.ticket ?: _uiState.value.ticket,
                        now = currentTimeMillis(),
                    )
                    startTicking()
                    startFollowingPickup(_uiState.value.order)
                }

                is Outcome.Failure -> _uiState.value = _uiState.value.copy(isUnlocking = false, feedback = result.error.message)
            }
        }
    }

    /**
     * A porta pelo rádio (Fase 9.7): a geladeira está sem internet e o
     * celular carrega o bilhete até ela.
     *
     * Um bilhete vencido não é um erro da pessoa — é a fila andando devagar.
     * Nesse caso o app pede outro ao servidor e tenta de novo, uma vez, sem
     * fazer ninguém tocar em nada.
     */
    fun unlockByBluetooth(retrying: Boolean = false) {
        val state = _uiState.value
        val ticket = state.ticket ?: return
        val id = state.order?.id ?: return

        if (!state.canUnlockByBluetooth) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTalkingToFridge = true, feedback = null)

            when (val result = fridge.unlock(ticket)) {
                is Outcome.Success -> when (result.value) {
                    BluetoothUnlockResult.Opened -> {
                        _uiState.value = _uiState.value.copy(isTalkingToFridge = false, feedback = "Abriu! Puxe a porta da geladeira.")
                        // O servidor só saberá quando a geladeira voltar à
                        // internet; até lá, quem viu a porta abrir foi o rádio.
                        refreshPickup(id)
                    }

                    BluetoothUnlockResult.Expired -> if (retrying) {
                        _uiState.value = _uiState.value.copy(isTalkingToFridge = false, feedback = "O bilhete venceu. Toque de novo para pedir outro.")
                    } else {
                        reissueAndRetry(id)
                    }

                    BluetoothUnlockResult.AlreadyUsed -> {
                        _uiState.value = _uiState.value.copy(isTalkingToFridge = false, feedback = "Esta compra já abriu a geladeira.")
                        refreshPickup(id)
                    }

                    BluetoothUnlockResult.Invalid ->
                        _uiState.value = _uiState.value.copy(isTalkingToFridge = false, feedback = "A geladeira não aceitou o bilhete. Procure o responsável.")

                    BluetoothUnlockResult.Unsupported ->
                        _uiState.value = _uiState.value.copy(isTalkingToFridge = false, feedback = "Esta geladeira não abre por Bluetooth.")
                }

                is Outcome.Failure -> _uiState.value = _uiState.value.copy(isTalkingToFridge = false, feedback = result.error.message)
            }
        }
    }

    /** O bilhete venceu esperando na fila: pede outro e tenta uma vez mais. */
    private suspend fun reissueAndRetry(id: Long) {
        when (val fresh = orders.reissueTicket(id)) {
            is Outcome.Success -> {
                _uiState.value = _uiState.value.copy(isTalkingToFridge = false, ticket = fresh.value ?: _uiState.value.ticket)
                unlockByBluetooth(retrying = true)
            }

            is Outcome.Failure -> _uiState.value = _uiState.value.copy(isTalkingToFridge = false, feedback = fresh.error.message)
        }
    }

    /**
     * Acompanha a retirada enquanto ela ainda pode mudar sozinha: é assim que
     * "puxe a porta" vira "retirado" sem ninguém recarregar a tela.
     */
    private fun startFollowingPickup(order: Order?) {
        pickupJob?.cancel()

        if (order == null || !order.isPaid || !_uiState.value.pickup.isFollowing) {
            return
        }

        startTicking()

        pickupJob = viewModelScope.launch {
            while (true) {
                delay(PICKUP_POLL_INTERVAL_MILLIS)
                refreshPickup(order.id)

                if (!_uiState.value.pickup.isFollowing) {
                    return@launch
                }
            }
        }
    }

    private suspend fun refreshPickup(id: Long) {
        val result = orders.pickupStatus(id)

        if (result is Outcome.Success) {
            _uiState.value = _uiState.value.copy(
                pickup = result.value.pickup,
                ticket = result.value.ticket ?: _uiState.value.ticket,
                now = currentTimeMillis(),
            )
        }
    }

    /**
     * Move o relógio do contador de segundo em segundo, só enquanto há PIX
     * esperando pagamento.
     */
    private fun startTicking() {
        tickingJob?.cancel()

        tickingJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                _uiState.value = _uiState.value.copy(now = currentTimeMillis())
            }
        }
    }

    override fun onCleared() {
        pollingJob?.cancel()
        tickingJob?.cancel()
        pickupJob?.cancel()
        super.onCleared()
    }

    companion object {
        /** Cinco segundos: rápido para quem espera, leve para o gateway. */
        const val POLL_INTERVAL_MILLIS = 5_000L

        /**
         * Três segundos: quem está com a mão na porta precisa ver a tela
         * virar quase junto com o ímã encostando.
         */
        const val PICKUP_POLL_INTERVAL_MILLIS = 3_000L
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
