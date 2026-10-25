package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.MascotFace
import com.pixstop.mobile.domain.model.PurchaseLine
import com.pixstop.mobile.ui.components.AppIcon
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonSize
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelCameraScreen
import com.pixstop.mobile.ui.components.PixelEmptyState
import com.pixstop.mobile.ui.components.PixelInput
import com.pixstop.mobile.ui.components.PixelLoader
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.components.QrCodeScannerScreen
import com.pixstop.mobile.ui.components.SensorCard
import com.pixstop.mobile.ui.components.formatMoney
import com.pixstop.mobile.ui.components.pixelShadow
import com.pixstop.mobile.ui.components.rememberPhotoPicker
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.CONFIDENT_MATCH
import com.pixstop.mobile.ui.viewmodel.PurchaseStep
import com.pixstop.mobile.ui.viewmodel.PurchaseUiState
import com.pixstop.mobile.ui.viewmodel.PurchaseViewModel
import com.pixstop.mobile.ui.viewmodel.averageAfter
import com.pixstop.mobile.ui.viewmodel.formatMoneyInput
import com.pixstop.mobile.ui.viewmodel.lineTotal
import org.koin.compose.viewmodel.koinViewModel

/**
 * A compra da volta do mercado (docs/plans/COMPRAS_MOBILE.md, passos 2 a 4).
 *
 * A cena é o estacionamento do atacado, com a sacola numa mão e o cupom na
 * outra. A foto sobe na hora e a compra passa a existir no servidor como
 * rascunho: fechar o app não perde nada, e ela continua em "Compras".
 *
 * @param purchaseId nulo abre a câmera para uma nota nova; um id abre a compra que já existe
 */
@Composable
fun PurchaseScreen(
    purchaseId: Long?,
    onBack: () -> Unit,
    onOpenShoppingList: () -> Unit,
    viewModel: PurchaseViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var cameraOpen by remember { mutableStateOf(purchaseId == null) }
    // Guardado aqui, acima das telas cheias (nota, produto, leitor): voltar
    // delas devolve a folha no mesmo ponto, e não no topo.
    val sheetScroll = rememberLazyListState()
    val pickFromGallery = rememberPhotoPicker { photo -> viewModel.onPhoto(photo) }

    LaunchedEffect(purchaseId) { purchaseId?.let(viewModel::open) }

    if (cameraOpen && state.step == PurchaseStep.Capture) {
        PixelCameraScreen(
            onPhoto = { photo ->
                cameraOpen = false
                viewModel.onPhoto(photo)
            },
            onDismiss = { cameraOpen = false },
            title = "FOTO DA NOTA",
            instruction = "A nota inteira, de ponta a ponta. Cupom comprido: afaste até caber.",
            startWithFront = false,
            squareFrame = false,
        )

        return
    }

    if (state.scannerOpen) {
        QrCodeScannerScreen(
            onCodeScanned = viewModel::onBarcode,
            onDismiss = viewModel::closeScanner,
            instruction = "Aponte para o código de barras da embalagem",
        )

        return
    }

    if (state.receiptOpen) {
        ReceiptViewer(state.receipt, onClose = viewModel::closeReceipt)

        return
    }

    if (state.pickingLine != null) {
        ProductPicker(state, viewModel)

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
            title = state.sheet?.label ?: "Nova compra",
            onBack = onBack,
            action = {
                if (state.sheet?.receiptUrl != null && state.step == PurchaseStep.Review) {
                    Box(modifier = Modifier.clickable(onClick = viewModel::openReceipt).padding(8.dp)) {
                        Text(text = "VER NOTA", style = PixTypography.inputLabel, color = PixColors.Cyan)
                    }
                }
            },
        )

        when (state.step) {
            PurchaseStep.Capture -> CaptureStep(
                state = state,
                onCamera = { cameraOpen = true },
                onGallery = pickFromGallery,
                onDismiss = viewModel::dismissMessage,
            )

            PurchaseStep.Uploading -> UploadingStep(state, viewModel)

            PurchaseStep.Reading -> ReadingStep(onBack)

            PurchaseStep.Review -> if (state.isWorking && state.sheet == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { PixelLoader() }
            } else {
                ReviewStep(state, viewModel, onBack, sheetScroll)
            }

            PurchaseStep.Done -> DoneStep(state, onBack, onOpenShoppingList)
        }
    }
}

/* ── Fotografar ───────────────────────────────────────────────────────── */

@Composable
private fun CaptureStep(state: PurchaseUiState, onCamera: () -> Unit, onGallery: () -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "LANÇAR A NOTA", style = PixTypography.sectionTitle, color = PixColors.Cyan)
        Text(
            text = "Fotografe o cupom ou escolha a foto na galeria. A IA lê os itens e você confere antes de entrar no estoque.",
            style = PixTypography.caption,
            color = PixColors.Gray400,
        )

        state.error?.let { PurchaseNotice(it, PixColors.Pink, onDismiss) }

        PixelButton(
            text = "Fotografar a nota",
            onClick = onCamera,
            modifier = Modifier.fillMaxWidth(),
            icon = { AppIcon(icon = AppIconType.Camera, contentDescription = null, tint = PixColors.Dark, modifier = Modifier.size(18.dp)) },
        )

        PixelButton(
            text = "Escolher da galeria",
            onClick = onGallery,
            variant = PixelButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun UploadingStep(state: PurchaseUiState, viewModel: PurchaseViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.error == null) {
            PixelLoader(message = "Enviando a foto…", delayMillis = 0)
        } else {
            PurchaseNotice(state.error, PixColors.Pink) {}

            PixelButton(text = "Tentar de novo", onClick = viewModel::retryUpload, modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * A leitura leva até um minuto. Sem o texto, parece travado; sem o botão, a
 * pessoa fica presa no estacionamento.
 */
@Composable
private fun ReadingStep(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PixelLoader(message = "Lendo a nota…", delayMillis = 0)

        Text(
            text = "Costuma levar até um minuto. A compra já está guardada: dá para voltar depois em \"Compras\".",
            style = PixTypography.caption,
            color = PixColors.Gray400,
        )

        PixelButton(
            text = "Deixar para depois",
            onClick = onBack,
            variant = PixelButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/* ── Conferir ─────────────────────────────────────────────────────────── */

@Composable
private fun ReviewStep(state: PurchaseUiState, viewModel: PurchaseViewModel, onBack: () -> Unit, listState: LazyListState) {
    val sheet = state.sheet ?: return
    var confirmingDiscard by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        state = listState,
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = if (state.editable) "Confira cada linha. Nada entra no estoque até você confirmar." else sheet.status.name.let { "Compra ${statusLabel(it)}." },
                style = PixTypography.caption,
                color = PixColors.Gray400,
            )
        }

        state.error?.let { item { PurchaseNotice(it, PixColors.Pink, viewModel::dismissMessage) } }
        state.message?.let { item { PurchaseNotice(it, PixColors.Green, viewModel::dismissMessage) } }
        sheet.readingError?.let {
            item { PurchaseNotice("Não consegui ler a nota. Digite os itens olhando a foto em \"Ver nota\".", PixColors.Pink) {} }
        }
        items(sheet.warnings) { warning -> PurchaseNotice(warning, PixColors.Yellow) {} }

        item { Header(state, viewModel) }

        sheet.duplicate?.let { twin ->
            if (state.editable) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().border(1.dp, PixColors.Yellow).padding(12.dp)) {
                        Text(text = "Parece a mesma nota que a ${twin.name}, que já entrou.", style = PixTypography.caption, color = PixColors.Yellow)
                        PixelButton(
                            text = if (state.allowDuplicate) "✓ É outra compra" else "É outra compra, pode confirmar",
                            onClick = { viewModel.onAllowDuplicate(!state.allowDuplicate) },
                            variant = if (state.allowDuplicate) PixelButtonVariant.Primary else PixelButtonVariant.Secondary,
                            buttonSize = PixelButtonSize.Small,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "${state.lines.size} ITENS" + if (state.pending > 0) " · ${state.pending} sem produto" else "",
                style = PixTypography.inputLabel,
                color = if (state.pending > 0) PixColors.Yellow else PixColors.Cyan,
            )
        }

        itemsIndexed(state.lines) { index, line ->
            LineCard(
                line = line,
                editable = state.editable,
                average = line.averageAfter(state.productOf(line)),
                onPick = { viewModel.openPicker(index) },
                onUnits = { viewModel.setUnits(index, it) },
                onPack = { viewModel.setPack(index, it) },
                onCost = { viewModel.setCost(index, it) },
                onIgnored = { viewModel.setIgnored(index, it) },
                onRemove = { viewModel.removeLine(index) },
            )
        }

        if (state.editable) {
            item {
                PixelButton(
                    text = "Adicionar item",
                    onClick = viewModel::addLine,
                    variant = PixelButtonVariant.Secondary,
                    buttonSize = PixelButtonSize.Small,
                )
            }
        }

        item {
            SensorCard(
                icon = AppIconType.Box,
                label = "Soma dos itens · ${state.units} un",
                value = formatMoney(state.itemsTotal),
                accent = if (state.totalGap == null) PixColors.Cyan else PixColors.Yellow,
                detail = state.totalGap?.let { gap ->
                    (if (gap > 0) "Passa " else "Falta ") + formatMoney(kotlin.math.abs(gap)) + " em relação ao total da nota"
                },
            )
        }

        if (state.editable) {
            item {
                if (state.pending > 0) {
                    Text(
                        text = "Escolha o produto de cada linha, ou marque \"não é estoque\", para poder confirmar.",
                        style = PixTypography.caption,
                        color = PixColors.Gray500,
                    )
                }

                PixelButton(
                    text = "Confirmar entrada",
                    onClick = viewModel::confirm,
                    enabled = state.canConfirm,
                    isLoading = state.isWorking,
                    loadingText = "Confirmando…",
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )

                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelButton(
                        text = "Salvar",
                        onClick = viewModel::save,
                        variant = PixelButtonVariant.Secondary,
                        enabled = !state.isWorking,
                        modifier = Modifier.weight(1f),
                    )
                    PixelButton(
                        text = if (confirmingDiscard) "Descartar mesmo" else "Descartar",
                        onClick = {
                            if (confirmingDiscard) viewModel.discard(onBack) else confirmingDiscard = true
                        },
                        variant = PixelButtonVariant.Secondary,
                        enabled = !state.isWorking,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(state: PurchaseUiState, viewModel: PurchaseViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PixelInput(
            value = state.supplierName,
            onValueChange = viewModel::onSupplierChange,
            label = "Fornecedor",
            placeholder = "Assaí, Atacadão…",
            enabled = state.editable,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PixelInput(
                value = state.documentNumber,
                onValueChange = viewModel::onDocumentChange,
                label = "Nota",
                enabled = state.editable,
                modifier = Modifier.weight(1f),
            )
            PixelInput(
                value = state.issuedAt,
                onValueChange = viewModel::onDateChange,
                label = "Data (AAAA-MM-DD)",
                enabled = state.editable,
                modifier = Modifier.weight(1f),
            )
        }

        PixelInput(
            value = state.totalText,
            onValueChange = viewModel::onTotalChange,
            label = "Total pago (R$)",
            enabled = state.editable,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        val appliances = state.sheet?.appliances.orEmpty()

        if (appliances.isNotEmpty()) {
            Text(text = "ENTRA EM", style = PixTypography.inputLabel, color = PixColors.Gray400)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelButton(
                    text = "Depósito",
                    onClick = { viewModel.onApplianceChange(null) },
                    enabled = state.editable,
                    variant = if (state.applianceId == null) PixelButtonVariant.Primary else PixelButtonVariant.Secondary,
                    buttonSize = PixelButtonSize.Small,
                )
                appliances.forEach { appliance ->
                    PixelButton(
                        text = appliance.name,
                        onClick = { viewModel.onApplianceChange(appliance.id) },
                        enabled = state.editable,
                        variant = if (state.applianceId == appliance.id) PixelButtonVariant.Primary else PixelButtonVariant.Secondary,
                        buttonSize = PixelButtonSize.Small,
                    )
                }
            }
        }
    }
}

/**
 * Uma linha da nota: como veio impressa em cima, o produto escolhido no meio
 * e os números embaixo. A borda amarela é o que ainda pede decisão.
 */
@Composable
private fun LineCard(
    line: PurchaseLine,
    editable: Boolean,
    average: Double?,
    onPick: () -> Unit,
    onUnits: (Int) -> Unit,
    onPack: (Int) -> Unit,
    onCost: (String) -> Unit,
    onIgnored: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    val accent = when {
        line.ignored -> PixColors.Gray600
        line.productId == null -> PixColors.Yellow
        else -> PixColors.Cyan
    }

    Box(modifier = Modifier.fillMaxWidth().pixelShadow(color = accent, offsetX = 4.dp, offsetY = 4.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().background(PixColors.Darker).border(2.dp, accent).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            line.rawDescription?.let { raw ->
                Text(text = raw, style = PixTypography.caption, color = PixColors.Gray400)
                rawMath(line)?.let { Text(text = it, style = PixTypography.caption, color = PixColors.Gray500) }
            }

            if (!line.ignored) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = editable, onClick = onPick),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = line.productName ?: "Escolha o produto…",
                        style = PixTypography.bodySecondary,
                        color = if (line.productId == null) PixColors.Yellow else PixColors.Gray100,
                        modifier = Modifier.weight(1f),
                    )
                    if (editable) {
                        AppIcon(icon = AppIconType.Search, contentDescription = "Escolher produto", tint = PixColors.Cyan, modifier = Modifier.size(18.dp))
                    }
                }

                sourceText(line)?.let { text ->
                    val unsure = line.matchSource == "ai" && (line.confidence ?: 0.0) < CONFIDENT_MATCH
                    Text(text = text, style = PixTypography.caption, color = if (unsure) PixColors.Yellow else PixColors.Gray500)
                }

                line.expectedQuantity?.let {
                    Text(text = "A lista pedia $it", style = PixTypography.caption, color = PixColors.Gray500)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    if (line.rawQuantity != null) {
                        NumberField("Emb.", line.packSize.toString(), editable, Modifier.weight(0.8f)) { text ->
                            text.filter(Char::isDigit).toIntOrNull()?.let(onPack)
                        }
                    }
                    NumberField("Unidades", line.quantityUnits.toString(), editable, Modifier.weight(1f)) { text ->
                        onUnits(text.filter(Char::isDigit).take(5).toIntOrNull() ?: 0)
                    }
                    NumberField("Custo un.", line.unitCost?.let(::formatMoneyInput).orEmpty(), editable, Modifier.weight(1.2f), decimal = true, onChange = onCost)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatMoney(line.lineTotal()) + (average?.let { " · médio vai a ${formatMoney(it)}" } ?: ""),
                        style = PixTypography.caption,
                        color = PixColors.Gray300,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (editable) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelButton(
                        text = if (line.ignored) "✓ Não é estoque" else "Não é estoque",
                        onClick = { onIgnored(!line.ignored) },
                        variant = if (line.ignored) PixelButtonVariant.Primary else PixelButtonVariant.Secondary,
                        buttonSize = PixelButtonSize.Small,
                    )
                    Box(modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.clickable(onClick = onRemove).padding(8.dp)) {
                        AppIcon(icon = AppIconType.Delete, contentDescription = "Tirar a linha", tint = PixColors.Gray500, modifier = Modifier.size(16.dp))
                    }
                }
            } else if (line.ignored) {
                Text(text = "Não é estoque", style = PixTypography.caption, color = PixColors.Gray500)
            }
        }
    }
}

/**
 * O campo numérico de uma linha. Guarda o texto enquanto a pessoa digita,
 * para "4," não virar "4" no meio do caminho.
 */
@Composable
private fun NumberField(label: String, value: String, enabled: Boolean, modifier: Modifier, decimal: Boolean = false, onChange: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }

    PixelInput(
        value = text,
        onValueChange = {
            text = it
            onChange(it)
        },
        label = label,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier,
    )
}

/** "24 Un × R$ 4,79 − R$ 7,20 = R$ 107,76": a conta da nota, para conferir de olho. */
private fun rawMath(line: PurchaseLine): String? {
    val quantity = line.rawQuantity ?: return null
    val price = line.rawUnitPrice ?: return line.rawTotal?.let { "= ${formatMoney(it)}" }
    val amount = if (quantity % 1.0 == 0.0) quantity.toLong().toString() else quantity.toString()
    val discount = line.rawDiscount?.takeIf { it > 0 }?.let { " − ${formatMoney(it)}" }.orEmpty()
    val total = line.rawTotal?.let { " = ${formatMoney(it)}" }.orEmpty()

    return "$amount ${line.rawUnit.orEmpty()} × ${formatMoney(price)}$discount$total"
}

private fun sourceText(line: PurchaseLine): String? {
    if (line.productId == null) return null

    return when (line.matchSource) {
        "barcode" -> "✓ código de barras"
        "alias" -> "✓ já conferido antes"
        "ai" -> "✨ sugestão da IA" + (line.confidence?.let { " · ${(it * 100).toInt()}%" } ?: "")
        "manual" -> "escolhido à mão"
        else -> null
    }
}

private fun statusLabel(status: String): String = when (status) {
    "Confirmed" -> "confirmada"
    "Discarded" -> "descartada"
    "Reversed" -> "estornada"
    "Reading" -> "sendo lida"
    else -> "em rascunho"
}

/* ── Escolher o produto ───────────────────────────────────────────────── */

@Composable
private fun ProductPicker(state: PurchaseUiState, viewModel: PurchaseViewModel) {
    val line = state.pickingLine?.let { state.lines.getOrNull(it) }
    val suggested = line?.productId?.let { id -> state.products.firstOrNull { it.id == id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixColors.Dark)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        PixelScreenTopBar(title = "Escolher produto", onBack = viewModel::closePicker)

        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            line?.rawDescription?.let { Text(text = "Na nota: $it", style = PixTypography.caption, color = PixColors.Gray400) }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PixelInput(
                    value = state.pickerQuery,
                    onValueChange = viewModel::onPickerQuery,
                    label = "Buscar produto",
                    placeholder = "nome do produto",
                    modifier = Modifier.weight(1f),
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

            state.message?.let { PurchaseNotice(it, PixColors.Yellow, viewModel::dismissMessage) }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (suggested != null && state.pickerQuery.isBlank()) {
                item {
                    SensorCard(
                        icon = AppIconType.Check,
                        label = "Escolhido agora",
                        value = suggested.name,
                        accent = PixColors.Green,
                        detail = "Tem ${suggested.stock} · custo médio ${formatMoney(suggested.priceCost)}",
                        onClick = { viewModel.choose(suggested) },
                    )
                }
            }

            items(state.pickerResults, key = { it.id }) { product ->
                SensorCard(
                    icon = AppIconType.Box,
                    label = "Tem ${product.stock} · custo médio ${formatMoney(product.priceCost)}",
                    value = product.name,
                    onClick = { viewModel.choose(product) },
                )
            }

            if (state.pickerResults.isEmpty()) {
                item {
                    PixelEmptyState(
                        message = "Nenhum produto com esse nome. Produto novo se cadastra no painel.",
                        face = MascotFace.Normal,
                    )
                }
            }
        }
    }
}

/* ── A nota original ──────────────────────────────────────────────────── */

@Composable
private fun ReceiptViewer(bytes: ByteArray?, onClose: () -> Unit) {
    val bitmap = remember(bytes) { bytes?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() } }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).navigationBarsPadding()) {
        PixelScreenTopBar(title = "Nota original", onBack = onClose)

        when {
            bytes == null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { PixelLoader() }

            bitmap == null -> PixelEmptyState(message = "Esta nota é um PDF. Abra pelo painel para ver.", modifier = Modifier.padding(24.dp))

            else -> Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Foto da nota",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/* ── Pronto ───────────────────────────────────────────────────────────── */

@Composable
private fun DoneStep(state: PurchaseUiState, onBack: () -> Unit, onOpenShoppingList: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "ENTRADA CONFIRMADA", style = PixTypography.pageTitle, color = PixColors.Green)

        SensorCard(
            icon = AppIconType.Check,
            label = "No estoque",
            value = "${state.confirmedUnits} unidades",
            accent = PixColors.Green,
            detail = "${state.lines.count { !it.ignored && it.productId != null }} produtos",
        )

        SensorCard(icon = AppIconType.Box, label = "Custo da compra", value = formatMoney(state.itemsTotal))

        if (state.backOnShelf.isNotEmpty()) {
            Text(
                text = "Voltaram à vitrine: " + state.backOnShelf.joinToString(", "),
                style = PixTypography.caption,
                color = PixColors.Cyan,
            )
        }

        PixelButton(text = "Ver lista de compras", onClick = onOpenShoppingList, variant = PixelButtonVariant.Secondary, modifier = Modifier.fillMaxWidth())
        PixelButton(text = "Voltar", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PurchaseNotice(text: String, color: Color, onDismiss: () -> Unit) {
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
