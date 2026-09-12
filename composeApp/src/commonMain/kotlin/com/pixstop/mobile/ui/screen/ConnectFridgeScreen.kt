package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixstop.mobile.domain.model.FridgeLight
import com.pixstop.mobile.domain.model.FridgeDevice
import com.pixstop.mobile.domain.model.FridgePresence
import com.pixstop.mobile.domain.model.WifiNetwork
import com.pixstop.mobile.ui.components.SensorCard
import com.pixstop.mobile.ui.components.LedSheet
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.ui.components.toSwatch
import com.pixstop.mobile.ui.components.CameraSheet
import com.pixstop.mobile.ui.components.SessionsSheet
import com.pixstop.mobile.ui.components.FridgeLamp
import com.pixstop.mobile.ui.components.IrRemote
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelInput
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.ConnectFridgeUiState
import com.pixstop.mobile.ui.viewmodel.ConnectFridgeViewModel
import com.pixstop.mobile.ui.viewmodel.ConnectStep
import org.koin.compose.viewmodel.koinViewModel

/**
 * Conectar uma geladeira (Fase 9.3).
 *
 * Um passo por tela, na ordem em que acontecem: escolher ou criar a
 * geladeira, ver o código, entrar na rede dela, escolher o WiFi da empresa,
 * esperar. O cliente nunca digita token nenhum.
 */
@Composable
fun ConnectFridgeScreen(
    onBack: () -> Unit,
    viewModel: ConnectFridgeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark)) {
        PixelScreenTopBar(
            title = "Geladeiras",
            onBack = {
                if (state.step == ConnectStep.Devices) onBack() else viewModel.restart()
            },
        )

        state.message?.let { Notice(text = it, color = PixColors.Green, onDismiss = viewModel::dismissMessage) }
        state.error?.let { Notice(text = it, color = PixColors.Pink, onDismiss = viewModel::dismissMessage) }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PixColors.Cyan)
                }

                // A barra de navegação do aparelho fica por cima do que a
                // tela desenha: sem reservar esse espaço, o botão de
                // conectar nascia atrás dos ícones do celular, sem jeito de
                // ser tocado. O `imePadding` faz o mesmo pelo teclado.
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (state.step) {
                        ConnectStep.Devices -> DevicesStep(state, viewModel)
                        ConnectStep.Code -> CodeStep(state, viewModel)
                        ConnectStep.Network -> NetworkStep(state, viewModel)
                        ConnectStep.Configuring -> WaitingStep(
                            title = "Configurando a geladeira…",
                            detail = state.portalMessage ?: "Ela está entrando no WiFi e trocando o código pelas credenciais.",
                        )

                        ConnectStep.WaitingOnline -> WaitingStep(
                            title = "Esperando a geladeira aparecer online…",
                            detail = state.portalMessage ?: "O primeiro sinal chega segundos depois de ela entrar no WiFi.",
                        )

                        ConnectStep.Done -> DoneStep(state, viewModel)
                        ConnectStep.Detail -> DetailStep(state, viewModel)
                    }
                }
            }
        }
    }

    // A gaveta da fita vale em qualquer passo: no fim da instalação e na
    // geladeira já instalada, é a mesma coisa que se abre.
    if (state.ledSheetOpen) {
        LedSheet(
            led = state.led,
            picked = state.pickedColor,
            pressing = state.pressing,
            lastSent = state.lastSent,
            online = state.led.online,
            onChooseProfile = viewModel::chooseProfile,
            onPress = viewModel::pressKey,
            onSave = viewModel::saveColor,
            onDismiss = viewModel::closeColorSheet,
        )
    }

    if (state.cameraSheetOpen) {
        CameraSheet(frame = state.frame, onDismiss = viewModel::closeCamera)
    }

    if (state.sessionsSheetOpen) {
        SessionsSheet(
            sessions = state.sessions,
            isLoading = state.isWorking,
            onDismiss = viewModel::closeSessions,
        )
    }
}

/** Quantas redes a lista mostra antes de pedir para ver o resto. */
private const val VISIBLE_NETWORKS = 5

// ─────────────────────────────────────────────────────────────────────────
// Passos
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun DevicesStep(state: ConnectFridgeUiState, viewModel: ConnectFridgeViewModel) {
    Text("Nova geladeira", style = PixTypography.sectionTitle, color = PixColors.Cyan)
    Text(
        "O sistema gera o código de ativação. Sem nome, ela nasce como Pixstop 01. Depois é só ligar a geladeira na tomada.",
        style = PixTypography.caption,
        color = PixColors.Gray300,
    )

    PixelInput(
        value = state.newName,
        onValueChange = viewModel::updateName,
        label = "Nome (opcional)",
        placeholder = "Pixstop 01",
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.isWorking,
    )

    PixelButton(
        text = "Criar e gerar código",
        onClick = viewModel::create,
        enabled = state.canCreate,
        isLoading = state.isWorking,
        modifier = Modifier.fillMaxWidth(),
    )

    if (state.pendingDevices.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text("Aguardando ativação", style = PixTypography.sectionTitle, color = PixColors.Yellow)

        state.pendingDevices.forEach { device ->
            SensorCard(
                icon = AppIconType.Lock,
                label = device.formattedCode?.let { "Código $it" } ?: "Aguardando ativação",
                value = device.name,
                accent = PixColors.Yellow,
                detail = "Toque para conectar: ligue a geladeira e entre na rede dela",
                onClick = { viewModel.choose(device) },
            )
        }
    }

    val others = state.devices - state.pendingDevices.toSet()

    if (others.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text("Instaladas", style = PixTypography.sectionTitle, color = PixColors.Gray300)

        others.forEach { device ->
            // Uma geladeira instalada abre o que ela está sentindo. Reinstalar
            // apaga as credenciais e recomeça do código: é coisa de dentro,
            // nas configurações dela, não de um botão ao lado do nome.
            SensorCard(
                icon = if (device.isOnline) AppIconType.Wifi else AppIconType.WifiOff,
                // O nome do equipamento só entra quando diz algo novo: em
                // geral ele é igual ao da geladeira, e repetir o mesmo nome
                // duas vezes na mesma carta não informa nada.
                label = device.applianceName?.takeIf { it != device.name } ?: "Instalada",
                value = device.name,
                accent = when (device.presence) {
                    FridgePresence.Online -> PixColors.Green
                    FridgePresence.Pending -> PixColors.Yellow
                    FridgePresence.Offline -> PixColors.Gray500
                    FridgePresence.Revoked -> PixColors.Pink
                },
                detail = buildString {
                    append(device.presence.label)
                    if (device.doorOpen == true) append(" · porta aberta")
                    device.signalStrength?.takeIf { device.isOnline }?.let { append(" · ").append(it).append(" dBm") }
                },
                onClick = { viewModel.openDetail(device) },
            )
        }
    }
}

@Composable
private fun CodeStep(state: ConnectFridgeUiState, viewModel: ConnectFridgeViewModel) {
    val device = state.chosen ?: return
    var showAdvanced by remember { mutableStateOf(false) }

    Text(device.name, style = PixTypography.sectionTitle, color = PixColors.Cyan)
    Text("Código de ativação", style = PixTypography.caption, color = PixColors.Gray300)

    Box(
        modifier = Modifier.fillMaxWidth().border(2.dp, PixColors.Cyan).padding(vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (state.isWorking) {
            CircularProgressIndicator(color = PixColors.Cyan)
        } else {
            Text(
                text = device.formattedCode ?: "— — —",
                style = PixTypography.pageTitle.copy(fontSize = 44.sp, letterSpacing = 6.sp),
                color = PixColors.Cyan,
            )
        }
    }

    Text("Vale por 24 horas e só pode ser usado uma vez.", style = PixTypography.caption, color = PixColors.Gray400)

    Spacer(Modifier.height(4.dp))
    Text("Como conectar", style = PixTypography.sectionTitle, color = PixColors.Gray100)
    Step(1, "Ligue a geladeira na tomada e espere cerca de um minuto.")
    Step(2, "Ela cria a rede WiFi ${state.hotspotSsid}.")

    if (state.canAutoJoin) {
        Step(3, "Toque em Conectar: o celular entra nessa rede sozinho, sem perder a internet do resto.")
    } else {
        Step(3, "Entre na rede ${state.hotspotSsid} pelas configurações de WiFi do celular (senha: ${state.hotspotPassword}) e volte aqui.")
    }

    Step(4, "Escolha o WiFi da empresa e digite a senha. O código já vai preenchido.")

    PixelButton(
        text = if (state.canAutoJoin) "Conectar à geladeira" else "Já estou na rede da geladeira",
        onClick = viewModel::joinHotspot,
        enabled = device.hasCode && !state.isWorking,
        isLoading = state.isWorking,
        modifier = Modifier.fillMaxWidth(),
    )

    PixelButton(
        text = "Gerar outro código",
        onClick = { viewModel.issueCode() },
        variant = PixelButtonVariant.Secondary,
        enabled = !state.isWorking,
        modifier = Modifier.fillMaxWidth(),
    )

    Text(
        text = if (showAdvanced) "Ocultar bancada" else "Bancada",
        style = PixTypography.caption,
        color = PixColors.Gray400,
        modifier = Modifier.clickable { showAdvanced = !showAdvanced }.padding(vertical = 4.dp),
    )

    if (showAdvanced) {
        // Para testar com o agente simulado num PC, que não está em 10.42.0.1.
        PixelInput(
            value = state.portalHost,
            onValueChange = viewModel::updatePortalHost,
            label = "Endereço do portal da geladeira",
            placeholder = "10.42.0.1",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        PixelInput(
            value = state.hotspotPassword,
            onValueChange = viewModel::updateHotspotPassword,
            label = "Senha da rede ${state.hotspotSsid}",
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun NetworkStep(state: ConnectFridgeUiState, viewModel: ConnectFridgeViewModel) {
    Text("WiFi da empresa", style = PixTypography.sectionTitle, color = PixColors.Cyan)
    Text(
        "A geladeira vê estas redes. Só as de 2,4 GHz aparecem: ela não fala em 5 GHz.",
        style = PixTypography.caption,
        color = PixColors.Gray300,
    )

    if (state.networks.isEmpty()) {
        Text("Nenhuma rede encontrada ainda.", style = PixTypography.caption, color = PixColors.Yellow)
    }

    // Escolhida a rede, a lista sai da frente. Onze redes empurravam a
    // senha e o botão para fora da tela, e quem estava instalando não via
    // que ainda faltava alguma coisa a fazer.
    var showAll by remember { mutableStateOf(false) }
    val chosen = state.networks.firstOrNull { it.ssid == state.ssid }

    if (state.ssid.isBlank()) {
        val visible = if (showAll) state.networks else state.networks.take(VISIBLE_NETWORKS)

        visible.forEach { network -> NetworkRow(network, selected = false) { viewModel.pickNetwork(network) } }

        if (!showAll && state.networks.size > VISIBLE_NETWORKS) {
            PixelButton(
                text = "Ver as outras ${state.networks.size - VISIBLE_NETWORKS}",
                onClick = { showAll = true },
                variant = PixelButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        PixelButton(
            text = "Procurar de novo",
            onClick = viewModel::scanNetworks,
            variant = PixelButtonVariant.Secondary,
            enabled = !state.isWorking,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        chosen?.let { NetworkRow(it, selected = true) {} }

        PixelButton(
            text = "Trocar de rede",
            onClick = { viewModel.updateSsid(""); showAll = false },
            variant = PixelButtonVariant.Secondary,
            enabled = !state.isWorking,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    PixelInput(
        value = state.ssid,
        onValueChange = viewModel::updateSsid,
        label = "Rede escolhida",
        placeholder = "ou digite o nome",
        modifier = Modifier.fillMaxWidth(),
    )

    PixelInput(
        value = state.password,
        onValueChange = viewModel::updatePassword,
        label = "Senha do WiFi",
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )

    state.code?.let { Text("Código: ${state.chosen?.formattedCode}", style = PixTypography.caption, color = PixColors.Gray300) }

    PixelButton(
        text = "Conectar a geladeira",
        onClick = viewModel::configure,
        enabled = state.canConfigure,
        isLoading = state.isWorking,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun WaitingStep(title: String, detail: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PixColors.Cyan)
    }
    Text(title, style = PixTypography.sectionTitle, color = PixColors.Cyan, modifier = Modifier.fillMaxWidth())
    Text(detail, style = PixTypography.caption, color = PixColors.Gray300)
}

@Composable
private fun DoneStep(state: ConnectFridgeUiState, viewModel: ConnectFridgeViewModel) {
    val device = state.chosen

    Text("Geladeira conectada!", style = PixTypography.sectionTitle, color = PixColors.Green)

    device?.let {
        Text(it.name, style = PixTypography.bodySecondary, color = PixColors.Gray100)
        it.signalStrength?.let { rssi ->
            Text("Sinal do WiFi: $rssi dBm${if (rssi < -75) " (fraco — considere aproximar o roteador)" else ""}", style = PixTypography.caption, color = PixColors.Gray300)
        }
    }

    // A geladeira online ainda não vende nada: o que interessa agora é o
    // que falta para a vitrine encher, e isso muda de empresa para empresa.
    Text(
        device?.stock?.nextStep
            ?: "Pedidos pagos já abrem a trava, e toda abertura da porta fica gravada.",
        style = PixTypography.caption,
        color = PixColors.Gray300,
    )

    // A fita é a última coisa da instalação, e a única que se faz de pé na
    // frente da geladeira: a cor tem de acender ali para valer a escolha.
    PixelButton(
        text = if (state.led.profileId != null) "Mudar a cor da fita" else "Escolher a cor da fita",
        onClick = viewModel::openColorStep,
        modifier = Modifier.fillMaxWidth(),
    )

    PixelButton(text = "Voltar às geladeiras", onClick = viewModel::restart, modifier = Modifier.fillMaxWidth())
}

/**
 * A geladeira por dentro (Fase 9.10): o que ela está sentindo agora.
 *
 * É a tela que a web tem e o app não tinha. Quem está de pé na frente dela
 * quer três respostas — a porta fechou, o sinal chega, a câmera existe — e
 * nenhuma delas cabe numa lista.
 */
@Composable
private fun DetailStep(state: ConnectFridgeUiState, viewModel: ConnectFridgeViewModel) {
    val device = state.chosen ?: return

    Text(device.name, style = PixTypography.sectionTitle, color = PixColors.Cyan)

    device.applianceName?.let {
        Text(it, style = PixTypography.caption, color = PixColors.Gray300)
    }

    // O primeiro mostrador responde "ela está viva?", que é o que decide se
    // vale a pena olhar o resto.
    SensorCard(
        icon = if (device.isOnline) AppIconType.Wifi else AppIconType.WifiOff,
        label = "Conexão",
        value = device.presence.label,
        accent = if (device.isOnline) PixColors.Green else PixColors.Pink,
        detail = if (device.isOnline) "Falando com o servidor agora" else "Sem dar notícia; confira a tomada e o WiFi",
    )

    // A porta é o mostrador que mais importa de longe: cadeado aberto é o
    // que faz alguém levantar e ir olhar.
    SensorCard(
        icon = if (device.doorOpen == true) AppIconType.LockOpen else AppIconType.Lock,
        label = "Porta",
        value = device.doorLabel,
        accent = when (device.doorOpen) {
            true -> PixColors.Yellow
            false -> PixColors.Green
            null -> PixColors.Gray500
        },
        detail = when (device.doorOpen) {
            true -> "Aberta agora — passados 45 s a geladeira avisa"
            false -> "Trancada, à espera de uma compra"
            null -> "Esta geladeira não tem sensor de porta ligado"
        },
    )

    device.signalLabel?.let {
        SensorCard(
            icon = AppIconType.Wifi,
            label = "Sinal do WiFi",
            value = it,
            accent = if ((device.signalStrength ?: 0) < -75) PixColors.Yellow else PixColors.Cyan,
            detail = if ((device.signalStrength ?: 0) < -75) {
                "Fraco: é daqui que começam as quedas. Aproxime o roteador"
            } else {
                "Chega bem onde ela está"
            },
        )
    }

    // A fita é a única carta que leva a algum lugar: bater nela abre o
    // controle, sem tirar a pessoa desta tela.
    SensorCard(
        icon = AppIconType.Bulb,
        label = "Fita LED",
        value = if (state.led.profileId != null) state.led.colorLabel else "Sem controle escolhido",
        accent = PixColors.Cyan,
        swatch = state.led.profileId?.let { FridgeLight.from(state.led.color).toSwatch() },
        detail = if (state.led.profileId != null) {
            "A cor de descanso. Toque para abrir o controle"
        } else {
            "Toque para dizer qual controle veio com esta fita"
        },
        onClick = { viewModel.openColorStep(device) },
    )

    // Abrir é a conferência da trava, não uma compra: não cobra ninguém e
    // fica registrada com o nome de quem apertou.
    SensorCard(
        icon = AppIconType.LockOpen,
        label = "Trava",
        value = if (state.unlocking) "Abrindo…" else "Abrir a geladeira",
        accent = if (device.isOnline) PixColors.Green else PixColors.Gray500,
        detail = "Destrava por um instante para você conferir. Sem compra e sem cobrança",
        onClick = if (device.isOnline && !state.unlocking) viewModel::unlock else null,
    )

    SensorCard(
        icon = AppIconType.Camera,
        label = "Câmera",
        value = device.cameraLabel,
        accent = if (device.cameraKind == "none" || device.cameraKind == null) PixColors.Gray500 else PixColors.Cyan,
        detail = if (device.isOnline) {
            "Toque para ver o que ela está enxergando agora"
        } else {
            "Toda abertura da porta vira vídeo, com ou sem compra"
        },
        onClick = if (device.isOnline && device.cameraKind != "none") viewModel::openCamera else null,
    )

    SensorCard(
        icon = AppIconType.Box,
        label = "Aberturas",
        value = "Ver as últimas",
        accent = PixColors.Cyan,
        detail = "Quem abriu, por quanto tempo, e o que a análise achou",
        onClick = viewModel::openSessions,
    )

    device.stock?.let { stock ->
        SensorCard(
            icon = AppIconType.Store,
            label = "Dentro dela",
            value = if (stock.inAppliance == 1) "1 produto" else "${stock.inAppliance} produtos",
            accent = if (stock.inAppliance == 0) PixColors.Yellow else PixColors.Green,
            detail = stock.nextStep,
        )
    }

    device.provisioningLabel?.takeIf { device.presence != FridgePresence.Pending }?.let {
        SensorCard(
            icon = AppIconType.Check,
            label = "Ativação",
            value = it,
            accent = PixColors.Green,
            detail = buildString {
                append("Versão ")
                append(device.firmwareVersion ?: "desconhecida")
                device.localIp?.let { ip -> append(" · ").append(ip) }
            },
        )
    }

    Spacer(Modifier.height(8.dp))

    // A reinstalação mora aqui, e não na lista: ela apaga as credenciais da
    // geladeira e recomeça do código. Não é coisa para ficar ao lado do nome,
    // a um toque de distância de quem só queria ver o estado dela.
    Text("Configurações", style = PixTypography.sectionTitle, color = PixColors.Gray300)

    SensorCard(
        icon = AppIconType.Refresh,
        label = "Estado",
        value = "Atualizar agora",
        accent = PixColors.Gray500,
        detail = "Pergunta de novo à geladeira o que ela está sentindo",
        onClick = viewModel::refreshDetail,
    )

    SensorCard(
        icon = AppIconType.Settings,
        label = "Instalação",
        value = "Reinstalar",
        accent = PixColors.Yellow,
        detail = "Gera um código novo e refaz a conexão com o WiFi. A geladeira sai do ar até terminar",
        onClick = { viewModel.choose(device) },
    )

    PixelButton(
        text = "Voltar às geladeiras",
        onClick = viewModel::restart,
        variant = PixelButtonVariant.Secondary,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NetworkRow(network: WifiNetwork, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (selected) PixColors.Cyan else PixColors.Gray600)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(network.ssid, style = PixTypography.bodySecondary, color = if (selected) PixColors.Cyan else PixColors.Gray100, modifier = Modifier.weight(1f))
        Text("${network.signal}%${if (!network.secured) " · aberta" else ""}", style = PixTypography.caption, color = PixColors.Gray400)
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(22.dp).border(1.dp, PixColors.Cyan),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", style = PixTypography.caption, color = PixColors.Cyan)
        }
        Spacer(Modifier.size(8.dp))
        Text(text, style = PixTypography.caption, color = PixColors.Gray100, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Notice(text: String, color: androidx.compose.ui.graphics.Color, onDismiss: () -> Unit) {
    Text(
        text = text,
        style = PixTypography.caption,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color)
            .clickable(onClick = onDismiss)
            .padding(12.dp),
    )
}
