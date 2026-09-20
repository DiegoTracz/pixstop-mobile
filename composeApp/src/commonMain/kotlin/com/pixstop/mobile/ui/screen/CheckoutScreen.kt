package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.checkout.PaymentMethod
import com.pixstop.mobile.domain.model.MascotState
import com.pixstop.mobile.domain.model.SavedCard
import com.pixstop.mobile.ui.components.CardForm
import com.pixstop.mobile.ui.components.PixelEmptyState
import com.pixstop.mobile.ui.components.PixelMascot
import com.pixstop.mobile.ui.viewmodel.CardMode
import com.pixstop.mobile.ui.components.AppIcon
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.domain.model.FridgeLight
import com.pixstop.mobile.ui.components.FridgeLamp
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelCoin
import com.pixstop.mobile.ui.components.PixelLoader
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.components.formatMoney
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.CheckoutViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Fechamento do pedido.
 *
 * Mostra a mesma conta que o servidor vai fazer: pixels viram desconto, o
 * saldo cobre o que sobrou e só o resto é cobrado. Ver um valor aqui e ser
 * cobrado outro seria o pior desfecho possível desta tela.
 */
@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onOrderPlaced: (Long) -> Unit,
    viewModel: CheckoutViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.placedOrder?.id) {
        state.placedOrder?.let { onOrderPlaced(it.id) }
    }

    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark)) {
        PixelScreenTopBar(title = "Fechar pedido", onBack = onBack)

        when {
            state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PixelLoader()
            }

            state.checkout == null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PixelEmptyState(message = state.error ?: "Não foi possível abrir o fechamento.", isError = true)
            }

            else -> {
                val checkout = state.checkout!!
                val totals = state.breakdown

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // A geladeira desta compra (Fase 9.7): a mesma cor que a
                    // fita está mostrando, e o aviso quando ela não vai abrir.
                    val appliance = checkout.appliance

                    if (appliance.light != FridgeLight.Off || appliance.blocksPayment) {
                        Section(title = "A geladeira") {
                            FridgeLamp(
                                light = appliance.light,
                                label = appliance.label.ifBlank { "Geladeira" },
                                hint = when {
                                    appliance.blocksPayment && appliance.supportPhone != null ->
                                        "Nada será cobrado enquanto ela estiver assim. Fale com o responsável: ${appliance.supportPhone}."

                                    appliance.blocksPayment ->
                                        "Nada será cobrado enquanto ela estiver assim. Procure o responsável pela empresa."

                                    appliance.bleAvailable && appliance.presence != com.pixstop.mobile.domain.model.Presence.Online ->
                                        "Sem internet, mas você abre por Bluetooth depois de pagar, aqui do lado dela."

                                    else -> null
                                },
                            )
                        }
                    }

                    Section(title = "Como pagar") {
                        state.availableMethods.forEach { method ->
                            MethodOption(
                                method = method,
                                selected = state.method == method,
                                onClick = { viewModel.onMethodChange(method) },
                            )
                        }

                        if (!checkout.gatewayAvailable) {
                            Text(
                                text = "Esta empresa ainda não configurou o recebimento de pagamentos, " +
                                    "então só dá para pagar com saldo e pixels.",
                                style = PixTypography.caption,
                                color = PixColors.Gray400,
                            )
                        }
                    }

                    if (state.method == PaymentMethod.Card) {
                        Section(title = "Qual cartão") {
                            // A escolha entre guardado e novo só aparece
                            // havendo os dois: com um só, ela seria uma
                            // pergunta de resposta única.
                            if (state.cardModes.size > 1) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.cardModes.forEach { mode ->
                                        ModeChip(
                                            label = mode.label,
                                            selected = state.cardMode == mode,
                                            onClick = { viewModel.onCardModeChange(mode) },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }

                            if (state.cardMode == CardMode.Saved) {
                                state.savedCards.forEach { card ->
                                    CardOption(
                                        card = card,
                                        selected = state.savedCardId == card.id,
                                        onClick = { viewModel.onSavedCardChange(card.id) },
                                    )
                                }
                            } else {
                                CardForm(
                                    card = state.card,
                                    errors = state.cardErrors,
                                    onChange = viewModel::onCardChange,
                                    enabled = !state.isPlacing,
                                )

                                Toggle(
                                    label = "Guardar este cartão para a próxima compra",
                                    checked = state.saveCard,
                                    onToggle = { viewModel.onSaveCardChange(!state.saveCard) },
                                )
                            }
                        }

                        if (state.canChooseInstallments) {
                            Section(title = "Parcelas") {
                                InstallmentsPicker(
                                    value = state.installments,
                                    max = state.maxInstallments,
                                    total = totals.charged,
                                    onChange = viewModel::onInstallmentsChange,
                                )
                            }
                        }
                    }

                    if (state.maxPixels > 0) {
                        Section(title = "Usar pixels") {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PixelCoin(size = 16.dp)

                                Text(
                                    text = "${state.pixels} de ${state.maxPixels}",
                                    style = PixTypography.sectionTitle,
                                    color = PixColors.Yellow,
                                )

                                Text(
                                    text = "= −${formatMoney(totals.pixelsDiscount)}",
                                    style = PixTypography.caption,
                                    color = PixColors.Green,
                                )
                            }

                            Slider(
                                value = state.pixels.toFloat(),
                                onValueChange = { viewModel.onPixelsChange(it.toInt()) },
                                valueRange = 0f..state.maxPixels.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = PixColors.Yellow,
                                    activeTrackColor = PixColors.Yellow,
                                    inactiveTrackColor = PixColors.Gray700,
                                ),
                            )

                            Text(
                                text = if (checkout.walletPixelsAsMoney > 0) {
                                    "Mínimo de ${checkout.minPixelsRedeem} pixels. Seus ${checkout.walletPixelsAsMoney} " +
                                        "pixels comprados pagam a compra inteira; o teto de " +
                                        "${checkout.maxDiscountPercentage.toInt()}% vale para os de bônus."
                                } else {
                                    "Mínimo de ${checkout.minPixelsRedeem} pixels, " +
                                        "até ${checkout.maxDiscountPercentage.toInt()}% da compra."
                                },
                                style = PixTypography.caption,
                                color = PixColors.Gray400,
                            )
                        }
                    }

                    Section(title = "Resumo") {
                        SummaryLine("Produtos", formatMoney(totals.products))

                        if (totals.pixelsDiscount > 0) {
                            SummaryLine("Pixels", "−${formatMoney(totals.pixelsDiscount)}", PixColors.Yellow)
                        }

                        if (totals.cardFee > 0) {
                            SummaryLine("Taxa do cartão", formatMoney(totals.cardFee), PixColors.Gray300)
                        }

                        SummaryLine(
                            label = if (totals.charged > 0) "A pagar" else "Total",
                            value = formatMoney(totals.charged),
                            color = PixColors.Cyan,
                            emphasis = true,
                        )

                        if (state.estimatedCashback > 0) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PixelCoin(size = 12.dp)

                                Text(
                                    text = "Você recebe ${state.estimatedCashback} pixels de volta",
                                    style = PixTypography.caption,
                                    color = PixColors.Yellow,
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PixColors.Darker)
                        .navigationBarsPadding()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // A recusa do pagamento: o mascote pisca vermelho três vezes ao lado
                    // da mensagem, e a mensagem é quem diz o que fazer.
                    state.error?.let {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PixelMascot(state = MascotState.Error, size = 40.dp, decorative = true)
                            Text(text = it, style = PixTypography.errorText)
                        }
                    }

                    if (state.missingMoney) {
                        Text(
                            text = "Seu saldo e seus pixels não cobrem ${formatMoney(totals.charged)}. " +
                                "Escolha outra forma de pagamento.",
                            style = PixTypography.caption,
                            color = PixColors.Pink,
                        )
                    }

                    PixelButton(
                        text = if (totals.charged > 0) "Pagar ${formatMoney(totals.charged)}" else "Concluir pedido",
                        onClick = viewModel::place,
                        enabled = state.canPlace,
                        isLoading = state.isPlacing,
                        loadingText = "Enviando",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = PixTypography.sectionTitle, color = PixColors.Cyan)

        content()
    }
}

@Composable
private fun MethodOption(
    method: PaymentMethod,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val label = when (method) {
        PaymentMethod.Money -> "PIX"
        PaymentMethod.Card -> "Cartão"
        PaymentMethod.Pixels -> "Só pixels"
        PaymentMethod.Mixed -> "Misto"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, if (selected) PixColors.Cyan else PixColors.Gray600)
            .background(if (selected) PixColors.CyanAlpha10 else PixColors.Darker)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .border(2.dp, if (selected) PixColors.Cyan else PixColors.Gray500)
                .background(if (selected) PixColors.Cyan else PixColors.Transparent),
        )

        Text(text = label, color = if (selected) PixColors.Cyan else PixColors.Gray100)
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        color = if (selected) PixColors.Cyan else PixColors.Gray300,
        modifier = modifier
            .border(2.dp, if (selected) PixColors.Cyan else PixColors.Gray600)
            .background(if (selected) PixColors.CyanAlpha10 else PixColors.Darker)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun CardOption(card: SavedCard, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, if (selected) PixColors.Cyan else PixColors.Gray600)
            .background(if (selected) PixColors.CyanAlpha10 else PixColors.Darker)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .border(2.dp, if (selected) PixColors.Cyan else PixColors.Gray500)
                .background(if (selected) PixColors.Cyan else PixColors.Transparent),
        )

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "${card.brand ?: "Cartão"} •••• ${card.lastFour}",
                color = if (selected) PixColors.Cyan else PixColors.Gray100,
            )

            card.expires?.let {
                Text(text = "Vence em $it", style = PixTypography.caption, color = PixColors.Gray400)
            }
        }
    }
}

/**
 * Parcelamento.
 *
 * Mostra o valor de cada parcela porque é isso que a pessoa decide — o número
 * de vezes sozinho não diz nada sobre o que vai cair na fatura.
 */
@Composable
private fun InstallmentsPicker(value: Int, max: Int, total: Double, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (1..max).forEach { times ->
            val selected = times == value

            Column(
                modifier = Modifier
                    .border(2.dp, if (selected) PixColors.Cyan else PixColors.Gray600)
                    .background(if (selected) PixColors.CyanAlpha10 else PixColors.Darker)
                    .clickable { onChange(times) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(text = "${times}x", color = if (selected) PixColors.Cyan else PixColors.Gray100)

                Text(
                    text = formatMoney(total / times),
                    style = PixTypography.caption,
                    color = PixColors.Gray400,
                )
            }
        }
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(2.dp, if (checked) PixColors.Cyan else PixColors.Gray500)
                .background(if (checked) PixColors.Cyan else PixColors.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                AppIcon(
                    icon = AppIconType.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = PixColors.Dark,
                )
            }
        }

        Text(text = label, color = PixColors.Gray100)
    }
}

@Composable
private fun SummaryLine(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color = PixColors.Gray100,
    emphasis: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = if (emphasis) PixTypography.sectionTitle else PixTypography.bodySecondary,
            color = if (emphasis) color else PixColors.Gray300,
        )

        Text(
            text = value,
            style = if (emphasis) PixTypography.sectionTitle else PixTypography.bodyRegular,
            color = color,
        )
    }
}
