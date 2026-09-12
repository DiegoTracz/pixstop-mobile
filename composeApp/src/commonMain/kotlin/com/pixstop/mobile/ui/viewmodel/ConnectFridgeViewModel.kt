package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.remote.SetupPortalClient
import com.pixstop.mobile.data.repository.FridgeRepository
import com.pixstop.mobile.domain.access.FridgeNetworkConnector
import com.pixstop.mobile.domain.model.FridgeDevice
import com.pixstop.mobile.domain.model.FridgePresence
import com.pixstop.mobile.domain.model.LedSettings
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

    /**
     * Uma geladeira instalada, por dentro: o que ela está sentindo agora.
     *
     * É a tela que a web tem e o app não tinha. Quem está de pé na frente
     * dela quer saber se a porta fechou, se o sinal chega e se a câmera
     * existe — sem abrir o computador.
     */
    Detail,
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
    /** A fita desta geladeira: cor de agora, catálogo de controles, teclas. */
    val led: LedSettings = LedSettings.Empty,
    /** A cor que a pessoa acendeu no controle e ainda não salvou. */
    val pickedColor: String? = null,
    /** A tecla que está saindo agora. */
    val pressing: String? = null,
    /** A última tecla que a geladeira recebeu, serve de repouso ou não. */
    val lastSent: String? = null,
    /**
     * A gaveta da fita está aberta.
     *
     * O controle ocupa a tela inteira, e numa gaveta ele aparece quando se
     * quer mexer na fita e some quando acaba — sem tirar a pessoa de onde
     * ela estava, que é a tela da geladeira com o resto dos mostradores.
     */
    val ledSheetOpen: Boolean = false,
) {
    /** Só quem ainda espera o código aparece para conectar. */
    val pendingDevices: List<FridgeDevice> get() = devices.filter { it.presence == FridgePresence.Pending }

    val code: String? get() = chosen?.activationCode

    /**
     * O nome é opcional: em branco, o servidor batiza de "Pixstop 01". Um
     * nome de uma letra, porém, é engano de digitação — e a lista com vinte
     * geladeiras chamadas "a" não ajuda ninguém.
     */
    val canCreate: Boolean get() = (newName.isBlank() || newName.trim().length >= 3) && !isWorking

    val canConfigure: Boolean get() = ssid.isNotBlank() && !code.isNullOrBlank() && !isWorking

    /** Sem controle escolhido não há tecla para apertar, mesmo online. */
    val canPressKeys: Boolean get() = led.canPress && !isWorking

    /** Salvar só faz sentido depois de a pessoa acender alguma coisa. */
    val canSaveColor: Boolean get() = pickedColor != null && !isWorking

    /** O que a fita está mostrando agora, para a lâmpada da tela. */
    val currentColor: String get() = pickedColor ?: led.color

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

    /**
     * Abre uma geladeira instalada e busca o estado dela agora — o que a
     * lista não mostra, por ser lista.
     */
    fun openDetail(device: FridgeDevice) {
        _uiState.update { it.copy(chosen = device, step = ConnectStep.Detail, error = null, message = null, led = LedSettings.Empty) }
        refreshDetail()
    }

    /** O estado de agora, para quem está olhando a tela. */
    fun refreshDetail() {
        val device = _uiState.value.chosen ?: return

        _uiState.update { it.copy(isWorking = true) }

        viewModelScope.launch {
            // O estado do aparelho e a fita são duas perguntas, e a tela
            // mostra as duas: a segunda só serve para dizer qual controle
            // esta geladeira usa, sem obrigar a entrar no passo da cor.
            when (val result = fridges.status(device.id)) {
                is Outcome.Success -> _uiState.update { it.copy(chosen = result.value, isWorking = false) }
                is Outcome.Failure -> _uiState.update { it.copy(isWorking = false, error = result.error.message) }
            }

            (fridges.ledSettings(device.id) as? Outcome.Success)?.let { led ->
                _uiState.update { it.copy(led = led.value) }
            }
        }
    }

    /* ─────────────────────────────────────────────────────────────────
     * A fita LED (Fase 9.10, etapa A)
     *
     * Quem instala está de pé na frente da geladeira. Escolhe o controle,
     * aperta a tecla, a fita acende, e o que ficou aceso é o que se salva.
     * Nada disto pede tradução: é o controle de verdade, na tela.
     * ───────────────────────────────────────────────────────────────── */

    /**
     * Abre o passo da fita e busca o catálogo de controles.
     *
     * `device` vem preenchido quando a pessoa entra pela lista, numa
     * geladeira que já está instalada: a fita se troca muito depois da
     * instalação, e obrigar a reinstalar para mudar de cor seria absurdo.
     */
    fun openColorStep(device: FridgeDevice? = null) {
        device?.let { chosen -> _uiState.update { it.copy(chosen = chosen) } }

        val device = _uiState.value.chosen ?: return

        _uiState.update { it.copy(ledSheetOpen = true, isWorking = true, error = null, message = null) }

        viewModelScope.launch {
            when (val result = fridges.ledSettings(device.id)) {
                is Outcome.Success -> _uiState.update { it.copy(led = result.value, isWorking = false) }
                is Outcome.Failure -> _uiState.update { it.copy(isWorking = false, error = result.error.message) }
            }
        }
    }

    /** Escolhe qual controle esta fita entende. Sem ele não há tecla nenhuma. */
    fun chooseProfile(profileId: Long) {
        _uiState.update { it.copy(led = it.led.copy(profileId = profileId), pickedColor = null, error = null) }
    }

    /**
     * Aperta uma tecla. A geladeira transmite na hora; se a tecla for uma
     * cor de repouso, ela passa a ser a candidata a ser salva. Brilho, tons
     * e os programas piscantes funcionam igual, mas não viram escolha.
     */
    fun pressKey(slug: String) {
        val device = _uiState.value.chosen ?: return
        val led = _uiState.value.led

        if (!led.canPress) return

        _uiState.update { it.copy(pressing = slug, error = null) }

        viewModelScope.launch {
            val result = fridges.pressKey(device.id, slug)

            _uiState.update {
                when (result) {
                    is Outcome.Success -> {
                        val isColor = it.led.colors.any { color -> color.value == slug }

                        it.copy(
                            pressing = null,
                            lastSent = slug,
                            pickedColor = if (isColor) slug else it.pickedColor,
                            // Quem aperta FLASH vê a fita mudar e não vê a
                            // seleção mudar: sem uma palavra aqui, parece
                            // que o botão não funcionou.
                            message = if (isColor) null else "Enviado. Esta tecla não serve de cor de repouso.",
                        )
                    }
                    is Outcome.Failure -> it.copy(pressing = null, error = result.error.message)
                }
            }
        }
    }

    /** Salva o que está aceso: a fita já está mostrando, e é isso que se guarda. */
    fun saveColor() {
        val device = _uiState.value.chosen ?: return
        val color = _uiState.value.pickedColor ?: return
        val profileId = _uiState.value.led.profileId

        _uiState.update { it.copy(isWorking = true, error = null) }

        viewModelScope.launch {
            when (val result = fridges.saveLed(device.id, color, profileId)) {
                is Outcome.Success -> _uiState.update {
                    it.copy(
                        led = result.value,
                        pickedColor = null,
                        isWorking = false,
                        ledSheetOpen = false,
                        message = "A geladeira fica ${result.value.colorLabel.lowercase()} quando está em repouso.",
                    )
                }
                is Outcome.Failure -> _uiState.update { it.copy(isWorking = false, error = result.error.message) }
            }
        }
    }

    /** "Agora não": a geladeira segue no arco-íris, que já é uma cor que serve. */
    /** Fecha a gaveta sem salvar: a fita fica na cor que a geladeira já tinha. */
    fun closeColorSheet() {
        _uiState.update { it.copy(ledSheetOpen = false, pickedColor = null, error = null) }
    }

    /** Volta para a lista, soltando a rede da geladeira se ainda estiver nela. */
    fun restart() {
        watcher?.cancel()
        leaveHotspot()
        _uiState.update {
            it.copy(
                step = ConnectStep.Devices,
                chosen = null,
                networks = emptyList(),
                ssid = "",
                password = "",
                portalMessage = null,
                isWorking = false,
                error = null,
                led = LedSettings.Empty,
                pickedColor = null,
                ledSheetOpen = false,
            )
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
