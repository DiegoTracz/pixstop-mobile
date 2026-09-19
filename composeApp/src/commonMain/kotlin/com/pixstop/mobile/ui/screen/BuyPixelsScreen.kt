package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.MascotState
import com.pixstop.mobile.domain.model.TopupMethod
import com.pixstop.mobile.ui.components.CardForm
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelEmptyState
import com.pixstop.mobile.ui.components.PixelInput
import com.pixstop.mobile.ui.components.PixelLoader
import com.pixstop.mobile.ui.components.PixelMascot
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.components.formatMoney
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.BuyPixelsStep
import com.pixstop.mobile.ui.viewmodel.BuyPixelsUiState
import com.pixstop.mobile.ui.viewmodel.BuyPixelsViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Comprar pixels para a carteira (docs/plans/CARTEIRA_PIXELS.md no servidor, P2).
 *
 * Valor pronto ou livre, Pix ou cartão, e os pixels entrando com o mascote.
 * Pixel comprado paga a compra inteira e não vence, e é isso que a tela diz
 * antes de cobrar, junto de quanto se recebe e quanto se paga.
 */
@Composable
fun BuyPixelsScreen(
    onBack: () -> Unit,
    onCredited: () -> Unit,
    viewModel: BuyPixelsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current

    // A carteira de quem chamou recarrega assim que os pixels entram.
    LaunchedEffect(state.step) {
        if (state.step == BuyPixelsStep.Done) onCredited()
    }

    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark)) {
        PixelScreenTopBar(
            title = "Comprar pixels",
            onBack = { if (state.step == BuyPixelsStep.Card) viewModel.back() else onBack() },
        )

        when {
            state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PixelLoader()
            }

            !state.offer.enabled -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PixelEmptyState(
                    message = state.error ?: state.offer.reason ?: "A compra de pixels não está disponível nesta empresa.",
                    isError = state.error != null,
                )
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when (state.step) {
                    BuyPixelsStep.Choose -> ChooseStep(state, viewModel)
                    BuyPixelsStep.Card -> CardStep(state, viewModel)
                    BuyPixelsStep.Pix -> state.topup?.let { topup ->
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "${formatPixelCount(topup.pixels)} pixels por ${formatMoney(topup.charged)}",
                                style = PixTypography.sectionTitle,
                            )
                            PixBlock(
                                code = topup.pixCode.orEmpty(),
                                qrCodeBase64 = topup.pixQrCodeBase64,
                                secondsLeft = state.pixSecondsLeft,
                                onCopy = { clipboard.setText(AnnotatedString(topup.pixCode.orEmpty())) },
                            )
                            // O PixBlock já diz que está esperando o pagamento; aqui
                            // só o que muda nesta compra: para onde vão os pixels.
                            Text(
                                text = "Assim que o pagamento cair, os pixels entram na carteira.",
                                style = PixTypography.bodySecondary,
                            )
                            PixelButton(
                                text = "Escolher outro valor",
                                onClick = viewModel::back,
                                variant = PixelButtonVariant.Secondary,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    BuyPixelsStep.Done -> state.topup?.let { topup ->
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            PixelMascot(state = MascotState.Success, size = 120.dp, decorative = true)
                            Text(
                                text = "+${formatPixelCount(topup.pixels)} pixels",
                                style = PixTypography.pageTitle,
                                color = PixColors.Green,
                            )
                            Text(
                                text = "Já estão na sua carteira e pagam a compra inteira.",
                                style = PixTypography.bodySecondary,
                                textAlign = TextAlign.Center,
                            )
                            PixelButton(text = "Voltar", onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChooseStep(state: BuyPixelsUiState, viewModel: BuyPixelsViewModel) {
    val offer = state.offer

    Text(
        text = "R$ 1 compra ${offer.pixelsPerReal} pixels. Pixels comprados pagam a compra inteira e não vencem.",
        style = PixTypography.bodySecondary,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        offer.presets.forEach { reais ->
            val selected = state.customText.isBlank() && state.preset == reais
            Choice(
                selected = selected,
                onClick = { viewModel.pick(reais) },
                modifier = Modifier.weight(1f),
            ) {
                Text(text = formatMoney(reais.toDouble()), style = PixTypography.bodyRegular)
                Text(text = "${formatPixelCount(reais * offer.pixelsPerReal)} pixels", style = PixTypography.caption)
            }
        }
    }

    PixelInput(
        value = state.customText,
        onValueChange = viewModel::onCustomChange,
        label = "OUTRO VALOR (R$ ${offer.min} A R$ ${offer.max})",
        placeholder = "Ex.: 30",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        TopupMethod.entries.forEach { method ->
            Choice(
                selected = state.method == method,
                onClick = { viewModel.onMethodChange(method) },
                modifier = Modifier.weight(1f),
            ) {
                Text(text = if (method == TopupMethod.Pix) "Pix" else "Cartão", style = PixTypography.bodyRegular)
            }
        }
    }

    Summary(state)

    state.error?.let { ErrorLine(it) }

    if (state.isSubmitting) {
        PixelLoader(modifier = Modifier.fillMaxWidth(), message = "Gerando o pagamento…", size = 40.dp)
    }

    PixelButton(
        text = if (state.method == TopupMethod.Pix) "Gerar Pix" else "Continuar para o cartão",
        onClick = viewModel::next,
        enabled = state.isValid && !state.isSubmitting,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CardStep(state: BuyPixelsUiState, viewModel: BuyPixelsViewModel) {
    Text(
        text = "${formatPixelCount(state.pixels)} pixels por ${formatMoney(state.charged)}",
        style = PixTypography.sectionTitle,
    )

    CardForm(
        card = state.card,
        errors = state.cardErrors,
        onChange = viewModel::onCardChange,
        enabled = !state.isSubmitting,
    )

    state.error?.let { ErrorLine(it) }

    PixelButton(
        text = "Pagar ${formatMoney(state.charged)}",
        onClick = viewModel::payWithCard,
        isLoading = state.isSubmitting,
        loadingText = "Pagando...",
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Quanto se recebe e quanto se paga, antes de cobrar. */
@Composable
private fun Summary(state: BuyPixelsUiState) {
    Column(
        modifier = Modifier.fillMaxWidth().background(PixColors.Darker).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Você recebe", style = PixTypography.bodySecondary)
            Text(
                text = if (state.isValid) "${formatPixelCount(state.pixels)} pixels" else "—",
                style = PixTypography.sectionTitle,
                color = PixColors.Yellow,
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Você paga", style = PixTypography.bodySecondary)
            Text(text = if (state.isValid) formatMoney(state.charged) else "—", style = PixTypography.bodyRegular)
        }
        if (state.method == TopupMethod.Card && state.offer.cardFeePercentage > 0) {
            Text(
                text = "O cartão tem taxa de ${formatPercent(state.offer.cardFeePercentage)}. No Pix não tem.",
                style = PixTypography.caption,
            )
        }
    }
}

@Composable
private fun Choice(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .border(2.dp, if (selected) PixColors.Cyan else PixColors.Gray700)
            .background(if (selected) PixColors.Cyan.copy(alpha = 0.12f) else PixColors.Darker)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        content()
    }
}

@Composable
private fun ErrorLine(message: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        PixelMascot(state = MascotState.Error, size = 36.dp, decorative = true)
        Text(text = message, style = PixTypography.errorText)
    }
}

private fun formatPixelCount(pixels: Int): String =
    pixels.toString().reversed().chunked(3).joinToString(".").reversed()

private fun formatPercent(value: Double): String =
    if (value % 1.0 == 0.0) "${value.toInt()}%" else "${value.toString().replace('.', ',')}%"
