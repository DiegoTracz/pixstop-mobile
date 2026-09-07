package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.remote.SetupPortalClient
import com.pixstop.mobile.data.repository.FridgeRepository
import com.pixstop.mobile.domain.access.FridgeNetworkConnector
import com.pixstop.mobile.domain.model.FridgeDevice
import com.pixstop.mobile.domain.model.FridgePresence
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.PortalMode
import com.pixstop.mobile.domain.model.PortalStatus
import com.pixstop.mobile.domain.model.WifiNetwork
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Os passos de conectar uma geladeira (Fase 9.3), na ordem em que acontecem.
 */
enum class ConnectStep {
    /** A lista: escolher uma geladeira aguardando ativação, ou criar. */
    Devices,
    /** O código em fonte grande e a instrução de entrar na rede da geladeira. */
    Code,
    /** Na rede da geladeira: escolher o WiFi da empresa e a senha. */
    Network,
    /** A geladeira está entrando no WiFi e trocando o código. */
    Configuring,
    /** Fora da rede da geladeira: esperando o servidor dizer "online". */
    WaitingOnline,
    Done,
}

/**
 * O estado da tela. As transições são puras — `withPortalStatus`,
 * `withPresence` — para poderem ser conferidas sem rede nem celular.
 */
data class ConnectFridgeUiState(
    val devices: List<FridgeDevice> = emptyList(),
    val chosen: FridgeDevice? = null,
    val step: ConnectStep = ConnectStep.Devices,
    val newName: String = "",
    val hotspotSsid: String = HOTSPOT_SSID,
    val hotspotPassword: String = HOTSPOT_PASSWORD,
    val portalHost: String = PORTAL_HOST,
    val canAutoJoin: Boolean = false,
    val joined: Boolean = false,
    val networks: List<WifiNetwork> = emptyList(),
    val ssid: String = "",
    val password: String = "",
    val portalMessage: String? = null,
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val message: String? = null,
    val error: String? = null,
) {
    /** Só quem ainda espera o código aparece para conectar. */
    val pendingDevices: List<FridgeDevice> get() = devices.filter { it.presence == FridgePresence.Pending }

    val code: String? get() = chosen?.activationCode

    val canCreate: Boolean get() = newName.trim().length >= 3 && !isWorking

    val canConfigure: Boolean get() = ssid.isNotBlank() && !code.isNullOrBlank() && !isWorking

    /**
     * O portal disse em que pé está. `done` avança para esperar o servidor;
     * `error` volta para a escolha da rede, com a mensagem, para tentar de
     * novo sem reiniciar nada.
     */
    fun withPortalStatus(status: PortalStatus): ConnectFridgeUiState = when (status.mode) {
        PortalMode.Done -> copy(step = ConnectStep.WaitingOnline, portalMessage = status.message, isWorking = false, error = null)
        PortalMode.Error -> copy(step = ConnectStep.Network, portalMessage = null, isWorking = false, error = status.message)
        PortalMode.Connecting, PortalMode.Provisioning -> copy(step = ConnectStep.Configuring, portalMessage = status.message, error = null)
        PortalMode.Setup -> if (step == ConnectStep.Configuring) copy(portalMessage = status.message) else this
    }

    /**
     * O servidor disse como a geladeira está. Online fecha o fluxo; o resto
     * só atualiza a linha na lista.
     */
    fun withPresence(device: FridgeDevice): ConnectFridgeUiState {
        val refreshed = copy(
            chosen = if (chosen?.id == device.id) device else chosen,
            devices = devices.map { if (it.id == device.id) device else it },
        )

        // Só a geladeira escolhida fecha o fluxo: outra ficando online é só uma linha da lista mudando.
        return if (device.isOnline && device.id == chosen?.id && (step == ConnectStep.WaitingOnline || step == ConnectStep.Code)) {
            refreshed.copy(step = ConnectStep.Done, isWorking = false, message = "${device.name} está online.")
        } else {
            refreshed
        }
    }

    companion object {
        const val HOTSPOT_SSID = "Pixstop-Setup"
        const val HOTSPOT_PASSWORD = "pixstop123"
        const val PORTAL_HOST = "10.42.0.1"
    }
}

/**
 * Conectar uma geladeira (Fase 9.3).
 *
 * O celular fala com dois lados que nunca se enxergam: o servidor Pixstop,
 * pela internet, e a geladeira, pela rede que ela mesma cria. Enquanto o app
 * está preso à rede da geladeira não há internet, então o servidor só volta a
 * ser consultado depois de `leave()`.
 */
class ConnectFridgeViewModel(
    private val fridges: FridgeRepository,
    private val portal: SetupPortalClient,
    private val network: FridgeNetworkConnector,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectFridgeUiState(canAutoJoin = network.canJoin))
    val uiState: StateFlow<ConnectFridgeUiState> = _uiState.asStateFlow()

    private var watcher: Job? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = fridges.devices()) {
                is Outcome.Success -> _uiState.update { it.copy(devices = result.value, isLoading = false) }
                is Outcome.Failure -> _uiState.update { it.copy(isLoading = false, error = result.error.message) }
            }
        }
    }

    fun updateName(value: String) = _uiState.update { it.copy(newName = value) }

    fun updateHotspotPassword(value: String) = _uiState.update { it.copy(hotspotPassword = value) }

    /** Para a bancada: o agente simulado num PC não está em 10.42.0.1. */
    fun updatePortalHost(value: String) = _uiState.update { it.copy(portalHost = value.trim()) }

    fun updateSsid(value: String) = _uiState.update { it.copy(ssid = value) }

    fun updatePassword(value: String) = _uiState.update { it.copy(password = value) }

    fun dismissMessage() = _uiState.update { it.copy(message = null, error = null) }

    /** Cria a geladeira e já cai no passo do código. */
    fun create() {
        val name = _uiState.value.newName.trim()

        if (!_uiState.value.canCreate) return

        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, error = null) }

            when (val result = fridges.create(name)) {
                is Outcome.Success -> _uiState.update {
                    it.copy(devices = it.devices + result.value, chosen = result.value, newName = "", step = ConnectStep.Code, isWorking = false)
                }

                is Outcome.Failure -> _uiState.update { it.copy(isWorking = false, error = result.error.message) }
            }
        }
    }

    fun choose(device: FridgeDevice) {
        _uiState.update { it.copy(chosen = device, step = ConnectStep.Code, error = null) }

        if (!device.hasCode) issueCode(device)
    }

    fun issueCode(target: FridgeDevice? = null) {
        val device = target ?: _uiState.value.chosen ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, error = null) }

            when (val result = fridges.issueCode(device.id)) {
                is Outcome.Success -> _uiState.update { it.withPresence(result.value).copy(isWorking = false) }
                is Outcome.Failure -> _uiState.update { it.copy(isWorking = false, error = result.error.message) }
            }
        }
    }

    /**
     * Entra na rede da geladeira (ou pede que a pessoa entre) e lê as redes
     * que ela vê. A partir daqui não há internet até `leaveHotspot()`.
     */
    fun joinHotspot() {
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, error = null) }

            if (network.canJoin) {
                when (val joined = network.join(state.hotspotSsid, state.hotspotPassword)) {
                    is Outcome.Success -> Unit
                    is Outcome.Failure -> {
                        _uiState.update { it.copy(isWorking = false, error = joined.error.message) }
                        return@launch
                    }
                }
            }

            _uiState.update { it.copy(joined = true) }
            scanNetworks()
        }
    }

    fun scanNetworks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true) }

            when (val result = portal.networks(_uiState.value.portalHost)) {
                is Outcome.Success -> _uiState.update {
                    it.copy(networks = result.value, step = ConnectStep.Network, isWorking = false, error = null)
                }

                is Outcome.Failure -> _uiState.update { it.copy(isWorking = false, error = result.error.message) }
            }
        }
    }

    fun pickNetwork(network: WifiNetwork) = _uiState.update { it.copy(ssid = network.ssid) }

    /**
     * Manda WiFi e código para a geladeira e acompanha o portal até ela
     * dizer "done" ou "error".
     */
    fun configure() {
        val state = _uiState.value
        val code = state.code ?: return

        if (!state.canConfigure) return

        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, error = null, step = ConnectStep.Configuring, portalMessage = "Enviando…") }

            when (val sent = portal.configure(state.portalHost, state.ssid, state.password, code)) {
                is Outcome.Success -> watchPortal()
                is Outcome.Failure -> _uiState.update { it.copy(isWorking = false, step = ConnectStep.Network, error = sent.error.message) }
            }
        }
    }

    private fun watchPortal() {
        watcher?.cancel()
        watcher = viewModelScope.launch {
            var silent = 0

            while (true) {
                delay(PORTAL_POLL_MS)

                when (val status = portal.status(_uiState.value.portalHost)) {
                    is Outcome.Success -> {
                        silent = 0
                        _uiState.update { it.withPortalStatus(status.value) }

                        when (_uiState.value.step) {
                            ConnectStep.WaitingOnline -> {
                                leaveHotspot()
                                watchServer()
                                return@launch
                            }

                            ConnectStep.Network -> return@launch
                            else -> Unit
                        }
                    }

                    // A geladeira some da rede quando entra no WiFi do
                    // cliente: se ela já tinha dito que estava provisionando,
                    // sumir é sinal de sucesso, não de falha.
                    is Outcome.Failure -> if (++silent >= SILENT_TOLERANCE) {
                        _uiState.update { it.copy(step = ConnectStep.WaitingOnline, portalMessage = "A geladeira saiu da rede de configuração.") }
                        leaveHotspot()
                        watchServer()
                        return@launch
                    }
                }
            }
        }
    }

    /** Solta a rede da geladeira e volta à internet. */
    fun leaveHotspot() {
        network.leave()
        _uiState.update { it.copy(joined = false) }
    }

    /**
     * Pergunta ao servidor até a geladeira aparecer online. Dois minutos
     * bastam: o primeiro sync sai segundos depois de ela entrar no WiFi.
     */
    private fun watchServer() {
        val device = _uiState.value.chosen ?: return

        watcher?.cancel()
        watcher = viewModelScope.launch {
            repeat(SERVER_POLLS) {
                delay(SERVER_POLL_MS)

                val result = fridges.status(device.id)

                if (result is Outcome.Success) {
                    _uiState.update { it.withPresence(result.value) }

                    if (_uiState.value.step == ConnectStep.Done) return@launch
                }
            }

            _uiState.update {
                it.copy(
                    isWorking = false,
                    error = "A geladeira ainda não apareceu online. Confira se ela está na rede da empresa; esta lista atualiza sozinha.",
                )
            }
        }
    }

    /** Volta para a lista, soltando a rede da geladeira se ainda estiver nela. */
    fun restart() {
        watcher?.cancel()
        leaveHotspot()
        _uiState.update {
            it.copy(step = ConnectStep.Devices, chosen = null, networks = emptyList(), ssid = "", password = "", portalMessage = null, isWorking = false, error = null)
        }
        load()
    }

    override fun onCleared() {
        network.leave()
        super.onCleared()
    }

    private companion object {
        const val PORTAL_POLL_MS = 2_000L
        const val SILENT_TOLERANCE = 8
        const val SERVER_POLL_MS = 5_000L
        const val SERVER_POLLS = 24
    }
}
