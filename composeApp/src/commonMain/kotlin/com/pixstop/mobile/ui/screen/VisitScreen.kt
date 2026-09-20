package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.CountProduct
import com.pixstop.mobile.domain.model.CountResultItem
import com.pixstop.mobile.domain.model.LossSuggestion
import com.pixstop.mobile.ui.components.AppIcon
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonSize
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelInput
import com.pixstop.mobile.ui.components.PixelLoader
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.components.QrCodeScannerScreen
import com.pixstop.mobile.ui.components.QuantityStepper
import com.pixstop.mobile.ui.components.SensorCard
import com.pixstop.mobile.ui.components.pixelShadow
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.VisitStep
import com.pixstop.mobile.ui.viewmodel.VisitViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * A visita do repositor (docs/plans/CONCILIACAO_MOBILE.md).
 *
 * Ele está de pé numa copa, com a porta aberta numa mão e o celular na outra.
 * Tudo aqui é feito para esse minuto: poucos toques, número grande, nada para
 * ler, e nada que se perca se a rede cair.
 *
 * Contar vem **antes** de abastecer, e não é ordem de tela: depois de encher a
 * prateleira não há mais o que conciliar. E o que o sistema esperava não
 * aparece enquanto ela conta — só depois de enviar, na gaveta das diferenças.
 */
@Composable
fun VisitScreen(
    applianceId: Long,
    onBack: () -> Unit,
    viewModel: VisitViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(applianceId) { viewModel.load(applianceId) }

    if (state.scannerOpen) {
        QrCodeScannerScreen(
            onCodeScanned = viewModel::onBarcode,
            onDismiss = viewModel::closeScanner,
            instruction = "Aponte para o código de barras da embalagem",
        )

        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixColors.Dark)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        PixelScreenTopBar(
            title = if (state.applianceName.isBlank()) "Visita" else state.applianceName,
            onBack = onBack,
        )

        when {
            state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { PixelLoader() }

            state.step == VisitStep.Counting -> CountingStep(state, viewModel)

            state.step == VisitStep.Differences -> DifferencesStep(state, viewModel)

            else -> DoneStep(state, onBack)
        }
    }
}

/* ── 1. Contar ─────────────────────────────────────────────────────────── */

@Composable
private fun CountingStep(state: com.pixstop.mobile.ui.viewmodel.VisitUiState, viewModel: VisitViewModel) {
    var confirmingSkipped by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            text = "CONTANDO ${state.countedCount} DE ${state.products.size}",
            style = PixTypography.inputLabel,
            color = PixColors.Cyan,
            modifier = Modifier.padding(top = 12.dp),
        )

        state.lastCountLabel?.let {
            Text(text = it, style = PixTypography.caption, color = PixColors.Gray400)
        }

        state.error?.let { Notice(it, PixColors.Pink, viewModel::dismissMessage) }
        state.message?.let { Notice(it, PixColors.Green, viewModel::dismissMessage) }

        if (state.draftFound) {
            Notice(
                "Você tinha uma contagem começada nesta geladeira. Ela está aqui do jeito que ficou.",
                PixColors.Yellow,
                viewModel::dismissMessage,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PixelInput(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                label = "Buscar produto",
                modifier = Modifier.weight(1f),
                placeholder = "nome do produto",
                leadingIcon = { AppIcon(icon = AppIconType.Search, contentDescription = null, tint = PixColors.Gray400) },
            )

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(PixColors.Darker)
                    .border(2.dp, PixColors.Cyan)
                    .clickable(onClick = viewModel::openScanner),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(icon = AppIconType.QrCodeScanner, contentDescription = "Ler código de barras", tint = PixColors.Cyan)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.visibleProducts, key = { it.id }) { product ->
                CountRow(
                    product = product,
                    counted = state.countOf(product.id),
                    focused = state.focusedProductId == product.id,
                    onChange = { viewModel.setCount(product.id, it) },
                    onEmpty = { viewModel.markEmpty(product.id) },
                )
            }
        }

        if (confirmingSkipped) {
            Notice(
                "${state.skipped.size} produto(s) ficaram sem contagem. Eles não serão acertados, e ficam registrados como não contados. Toque de novo para enviar assim.",
                PixColors.Yellow,
            ) { confirmingSkipped = false }
        }

        PixelButton(
            text = "Conferir diferenças (${state.countedCount})",
            onClick = {
                if (state.hasSkipped && !confirmingSkipped) {
                    confirmingSkipped = true
                } else {
                    viewModel.review()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            enabled = state.canReview,
            isLoading = state.isWorking,
            loadingText = "Enviando...",
        )
    }
}

/**
 * Uma linha da contagem.
 *
 * Cinza enquanto não contada, ciano depois: não há verde nem vermelho aqui,
 * porque antes de enviar não existe certo nem errado — existe o que a pessoa
 * está vendo.
 */
@Composable
private fun CountRow(
    product: CountProduct,
    counted: Int?,
    focused: Boolean,
    onChange: (Int) -> Unit,
    onEmpty: () -> Unit,
) {
    var typing by remember { mutableStateOf(false) }
    val accent = when {
        focused -> PixColors.Yellow
        counted != null -> PixColors.Cyan
        else -> PixColors.Gray600
    }

    Box(modifier = Modifier.fillMaxWidth().pixelShadow(color = accent, offsetX = 4.dp, offsetY = 4.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PixColors.Darker)
                .border(2.dp, accent)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = product.name, style = PixTypography.bodySecondary, color = PixColors.Gray100)
                    Text(
                        text = product.category ?: if (counted == null) "não contado" else "contado",
                        style = PixTypography.caption,
                        color = PixColors.Gray400,
                    )
                }

                if (counted == null) {
                    PixelButton(
                        text = "não tem",
                        onClick = onEmpty,
                        variant = PixelButtonVariant.Secondary,
                        buttonSize = PixelButtonSize.Small,
                    )
                }

                QuantityStepper(
                    value = counted,
                    onChange = onChange,
                    onNumberClick = { typing = true },
                )
            }

            if (typing) {
                PixelInput(
                    value = counted?.toString().orEmpty(),
                    onValueChange = { text ->
                        val digits = text.filter { it.isDigit() }.take(5)

                        onChange(digits.toIntOrNull() ?: 0)
                    },
                    label = "Quantas unidades",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        Box(modifier = Modifier.clickable { typing = false }) {
                            AppIcon(icon = AppIconType.Check, contentDescription = "Pronto", tint = PixColors.Cyan)
                        }
                    },
                )
            }
        }
    }
}

/* ── 2. Diferenças ─────────────────────────────────────────────────────── */

@Composable
private fun DifferencesStep(state: com.pixstop.mobile.ui.viewmodel.VisitUiState, viewModel: VisitViewModel) {
    val result = state.result ?: return

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "DIFERENÇAS", style = PixTypography.sectionTitle, color = PixColors.Cyan, modifier = Modifier.padding(top = 12.dp))

        Text(
            text = if (result.hasDifferences) {
                "A geladeira tinha ${result.missing.size} item(ns) a menos e ${result.extra.size} a mais do que o sistema esperava."
            } else {
                "Tudo bateu: ${result.matched} produtos conferidos."
            },
            style = PixTypography.caption,
            color = PixColors.Gray400,
        )

        if (result.missingValue > 0) {
            SensorCard(
                icon = AppIconType.Box,
                label = "Faltando",
                value = money(result.missingValue),
                accent = PixColors.Pink,
                detail = if (result.alert) "O dono do estoque foi avisado" else null,
            )
        }

        if (result.skippedItems > 0) {
            Text(
                text = "${result.skippedItems} produto(s) não foram contados, e ficaram registrados assim.",
                style = PixTypography.caption,
                color = PixColors.Yellow,
            )
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(result.missing + result.extra, key = { it.productId }) { item ->
                DifferenceRow(item = item, onSuggest = { viewModel.suggest(item, it) }, onRecount = { viewModel.recount(item) })
            }
        }

        Text(
            text = "Quem decide o que vira perda é o dono do estoque, no fechamento do mês.",
            style = PixTypography.caption,
            color = PixColors.Gray500,
        )

        PixelButton(
            text = "Concluir visita",
            onClick = viewModel::finish,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun DifferenceRow(item: CountResultItem, onSuggest: (LossSuggestion) -> Unit, onRecount: () -> Unit) {
    val accent = if (item.difference < 0) PixColors.Pink else PixColors.Yellow

    Box(modifier = Modifier.fillMaxWidth().pixelShadow(color = accent, offsetX = 4.dp, offsetY = 4.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().background(PixColors.Darker).border(2.dp, accent).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.product, style = PixTypography.bodySecondary, color = PixColors.Gray100)
                    Text(
                        text = "esperado ${item.expected} · contou ${item.counted}",
                        style = PixTypography.caption,
                        color = PixColors.Gray400,
                    )
                }

                Text(
                    text = if (item.difference > 0) "+${item.difference}" else item.difference.toString(),
                    style = PixTypography.sectionTitle,
                    color = accent,
                )

                Text(text = money(item.differenceValue), style = PixTypography.caption, color = PixColors.Gray400)
            }

            if (item.difference < 0 && !item.recounted) {
                Text(text = "sugerir:", style = PixTypography.caption, color = PixColors.Gray500)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LossSuggestion.entries.forEach { suggestion ->
                        PixelButton(
                            text = suggestion.label,
                            onClick = { onSuggest(suggestion) },
                            variant = if (item.suggestedReason == suggestion) PixelButtonVariant.Primary else PixelButtonVariant.Secondary,
                            buttonSize = PixelButtonSize.Small,
                        )
                    }
                }

                PixelButton(
                    text = "Contei errado",
                    onClick = onRecount,
                    variant = PixelButtonVariant.Secondary,
                    buttonSize = PixelButtonSize.Small,
                )
            }
        }
    }
}

/* ── 3. Fim ────────────────────────────────────────────────────────────── */

@Composable
private fun DoneStep(state: com.pixstop.mobile.ui.viewmodel.VisitUiState, onBack: () -> Unit) {
    val result = state.result

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "VISITA CONCLUÍDA", style = PixTypography.pageTitle, color = PixColors.Green)

        SensorCard(
            icon = AppIconType.Check,
            label = "Contados",
            value = "${result?.countedItems ?: 0} produtos",
            accent = PixColors.Green,
            detail = result?.skippedItems?.takeIf { it > 0 }?.let { "$it não contados" },
        )

        SensorCard(
            icon = AppIconType.Box,
            label = "Diferença",
            value = if ((result?.missingValue ?: 0.0) > 0) "− ${money(result?.missingValue ?: 0.0)}" else "nenhuma",
            accent = if ((result?.missingValue ?: 0.0) > 0) PixColors.Pink else PixColors.Green,
        )

        PixelButton(text = "Voltar", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Notice(text: String, color: Color, onDismiss: () -> Unit) {
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

/** R$ 25,50 — com vírgula, que é como se lê em português. */
private fun money(value: Double): String {
    val cents = (kotlin.math.abs(value) * 100).toLong()

    return "R$ ${cents / 100},${(cents % 100).toString().padStart(2, '0')}"
}
