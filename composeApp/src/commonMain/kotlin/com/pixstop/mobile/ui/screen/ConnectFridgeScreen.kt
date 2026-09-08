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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixstop.mobile.domain.model.FridgeDevice
import com.pixstop.mobile.domain.model.FridgePresence
import com.pixstop.mobile.domain.model.WifiNetwork
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
                    }
                }
            }
        }
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
            DeviceRow(device = device, action = "Conectar", onClick = { viewModel.choose(device) })
        }
    }

    val others = state.devices - state.pendingDevices.toSet()

    if (others.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text("Instaladas", style = PixTypography.sectionTitle, color = PixColors.Gray300)

        others.forEach { device ->
            DeviceRow(device = device, action = "Reinstalar", onClick = { viewModel.choose(device) })
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

    PixelButton(text = "Voltar às geladeiras", onClick = viewModel::restart, modifier = Modifier.fillMaxWidth())
}

// ─────────────────────────────────────────────────────────────────────────
// Peças
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun DeviceRow(device: FridgeDevice, action: String, onClick: () -> Unit) {
    val color = when (device.presence) {
        FridgePresence.Online -> PixColors.Green
        FridgePresence.Pending -> PixColors.Yellow
        FridgePresence.Offline -> PixColors.Gray400
        FridgePresence.Revoked -> PixColors.Pink
    }

    Row(
        modifier = Modifier.fillMaxWidth().border(1.dp, PixColors.Gray600).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(device.name, style = PixTypography.bodySecondary, color = PixColors.Gray100)
            Text(
                buildString {
                    append(device.presence.label)
                    device.applianceName?.let { append(" · ").append(it) }
                    device.signalStrength?.takeIf { device.isOnline }?.let { append(" · ").append(it).append(" dBm") }
                },
                style = PixTypography.caption,
                color = color,
            )
        }

        PixelButton(text = action, onClick = onClick, variant = PixelButtonVariant.Secondary)
    }
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
