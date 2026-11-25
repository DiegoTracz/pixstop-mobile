package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import com.pixstop.mobile.domain.model.ProductCategory
import com.pixstop.mobile.ui.components.PixelButtonSize
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.CatalogSuggestion
import com.pixstop.mobile.domain.model.CreatedProduct
import com.pixstop.mobile.domain.model.PhotoBackground
import com.pixstop.mobile.domain.model.PhotoScene
import com.pixstop.mobile.ui.components.ProductImage
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.ui.viewmodel.ProductDraft
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelCameraScreen
import com.pixstop.mobile.ui.components.PixelInput
import com.pixstop.mobile.ui.components.PixelLoader
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.components.QrCodeScannerScreen
import com.pixstop.mobile.ui.components.SensorCard
import com.pixstop.mobile.ui.components.formatMoney
import com.pixstop.mobile.ui.components.rememberPhotoPicker
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.ProductFormUiState
import com.pixstop.mobile.ui.viewmodel.ProductFormViewModel
import androidx.compose.ui.graphics.decodeToImageBitmap
import org.koin.compose.viewmodel.koinViewModel

/**
 * Cadastrar produto no celular (APP_OPERADOR_PRODUTOS.md, G4).
 *
 * Na ordem em que a pessoa faz: foto, o que a foto achou, o custo que só ela
 * sabe, e salvar. Antes de salvar, os parecidos já cadastrados pedem uma
 * resposta, para a mesma lata não virar dois produtos.
 */
@Composable
fun ProductFormScreen(
    onBack: () -> Unit,
    /** Vindo de um item da nota: o que ela já diz do produto. */
    prefill: ProductDraft? = null,
    /** Quem abriu quer o produto de volta, sem a tela de "cadastrado". */
    onCreated: ((CreatedProduct) -> Unit)? = null,
    viewModel: ProductFormViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val pickFromGallery = rememberPhotoPicker { photo -> viewModel.onPhoto(photo) }

    LaunchedEffect(prefill) { prefill?.let(viewModel::prefill) }

    LaunchedEffect(state.created) {
        val created = state.created
        if (created != null && onCreated != null) onCreated(created)
    }

    if (state.cameraOpen) {
        PixelCameraScreen(
            onPhoto = viewModel::onPhoto,
            onDismiss = viewModel::closeCamera,
            title = "FOTO DO PRODUTO",
            instruction = "O produto inteiro no quadro, com o rótulo de frente.",
            startWithFront = false,
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

    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark)) {
        PixelScreenTopBar(title = "Novo produto", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val created = state.created

            if (created != null && onCreated == null) {
                CreatedStep(state, onAnother = viewModel::startAnother, onDone = onBack)
            } else if (created == null) {
                FormStep(state, viewModel, onGallery = pickFromGallery)
            }
        }
    }
}

@Composable
private fun FormStep(state: ProductFormUiState, viewModel: ProductFormViewModel, onGallery: () -> Unit) {
    val priceFocus = remember { FocusRequester() }

    // Vindo da nota, nome e custo já estão lá: o cursor vai direto ao preço.
    LaunchedEffect(state.fromNote) {
        if (state.fromNote && state.price.isBlank()) runCatching { priceFocus.requestFocus() }
    }

    PhotoHeader(state, viewModel, onGallery)

    state.error?.let {
        Text(
            it,
            style = PixTypography.caption,
            color = PixColors.Pink,
            modifier = Modifier.fillMaxWidth().border(1.dp, PixColors.Pink).clickable(onClick = viewModel::dismissError).padding(12.dp),
        )
    }

    when {
        state.fromNote -> Text("Nome e custo vieram da nota: confira e diga o preço de venda.", style = PixTypography.caption, color = PixColors.Yellow)
        state.reading != null -> Text("Nome, categoria e preço vieram da foto: confira.", style = PixTypography.caption, color = PixColors.Yellow)
    }

    SimilarBlock(state, viewModel)
    CatalogBlock(state, viewModel)

    PixelInput(
        value = state.name,
        onValueChange = viewModel::updateName,
        label = "Nome",
        placeholder = "Guaraná Antarctica lata 350 ml",
        error = state.nameError,
        modifier = Modifier.fillMaxWidth(),
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PixelInput(
            value = state.cost,
            onValueChange = viewModel::updateCost,
            label = "Custo unitário",
            placeholder = "2,80",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
        PixelInput(
            value = state.price,
            onValueChange = viewModel::updatePrice,
            label = "Preço de venda",
            placeholder = "5,50",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).focusRequester(priceFocus),
        )
    }

    state.marginPercent?.let { margin ->
        Text(
            if (state.sellsAtLoss) "O custo está acima do preço: confira a vírgula." else "Margem de $margin% sobre o preço.",
            style = PixTypography.caption,
            color = if (state.sellsAtLoss) PixColors.Pink else PixColors.Gray300,
        )
    }

    if (state.categories.isNotEmpty()) {
        Chips(
            options = listOf<ProductCategory?>(null) + state.categories,
            selected = state.categories.firstOrNull { it.id == state.categoryId },
            label = { it?.name ?: "Sem categoria" },
            onSelect = { viewModel.chooseCategory(it?.id) },
        )
    }

    Text(
        if (state.moreOpen) "Menos detalhes" else "Mais detalhes: estoque, código de barras, descrição",
        style = PixTypography.caption,
        color = PixColors.Cyan,
        modifier = Modifier.fillMaxWidth().clickable(onClick = viewModel::toggleMore).padding(vertical = 6.dp),
    )

    if (state.moreOpen) {
        // Da nota, o estoque entra quando a compra for confirmada: perguntar
        // aqui faria a mesma mercadoria entrar duas vezes.
        if (!state.fromNote) {
            PixelInput(
                value = state.initialStock,
                onValueChange = viewModel::updateStock,
                label = "Quantas unidades você tem agora",
                placeholder = "0",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            PixelInput(
                value = state.barcode,
                onValueChange = viewModel::updateBarcode,
                label = "Código de barras",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            PixelButton(text = "Ler", onClick = viewModel::openScanner, variant = PixelButtonVariant.Secondary)
        }

        PixelInput(
            value = state.description,
            onValueChange = viewModel::updateDescription,
            label = "Descrição",
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    PixelButton(
        text = "Cadastrar produto",
        onClick = viewModel::save,
        enabled = state.canSave,
        isLoading = state.isSaving,
        modifier = Modifier.fillMaxWidth(),
    )

    if (state.similar.isNotEmpty()) {
        Text("Responda sobre os parecidos acima para cadastrar.", style = PixTypography.caption, color = PixColors.Gray400)
    }

    if (state.enhanceOpen) {
        EnhanceSheet(state, viewModel)
    }
}

/**
 * A foto pequena, com as ações ao lado: a foto importa, mas não pode empurrar
 * o nome e o preço para fora da tela.
 */
@Composable
private fun PhotoHeader(state: ProductFormUiState, viewModel: ProductFormViewModel, onGallery: () -> Unit) {
    val catalogPhoto = state.catalogChoice?.takeIf { it.hasPhoto && state.photo == null }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(112.dp).background(PixColors.Darker).border(2.dp, if (state.useEnhanced) PixColors.Cyan else PixColors.Gray700).clickable(onClick = viewModel::openCamera),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.useEnhanced && state.enhanced != null -> Thumbnail(state.enhanced)
                catalogPhoto != null -> ProductImage(name = catalogPhoto.name, imageUrl = catalogPhoto.imageUrl, initialStyle = PixTypography.caption, modifier = Modifier.fillMaxSize())
                state.photo != null -> Thumbnail(state.photo)
                else -> Text("Sem foto", style = PixTypography.caption, color = PixColors.Gray500)
            }

            if (state.isReading) {
                Box(Modifier.fillMaxSize().background(PixColors.Dark.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) { PixelLoader() }
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PixelButton(
                    text = if (state.photo == null) "Foto" else "Outra",
                    onClick = viewModel::openCamera,
                    enabled = !state.isReading,
                    buttonSize = PixelButtonSize.Small,
                    modifier = Modifier.weight(1f),
                )
                PixelButton(
                    text = "Galeria",
                    onClick = onGallery,
                    variant = PixelButtonVariant.Secondary,
                    enabled = !state.isReading,
                    buttonSize = PixelButtonSize.Small,
                    modifier = Modifier.weight(1f),
                )
            }

            if (state.photo != null || catalogPhoto != null) {
                PixelButton(
                    text = if (state.useEnhanced) "Foto com IA ✓" else "Melhorar com IA",
                    onClick = viewModel::openEnhance,
                    variant = PixelButtonVariant.Secondary,
                    buttonSize = PixelButtonSize.Small,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            when {
                state.isReading -> Text("Lendo a foto…", style = PixTypography.caption, color = PixColors.Gray300)
                catalogPhoto != null && !state.useEnhanced -> Text("Foto do catálogo", style = PixTypography.caption, color = PixColors.Green)
            }
        }
    }
}

/**
 * "Melhorar com IA" (APP_OPERADOR_PRODUTOS.md, G6): a foto do celular ou a do
 * catálogo vira foto de vitrine ou de propaganda. A prévia aparece ao lado da
 * original, e só vale se a pessoa disser "usar esta".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhanceSheet(state: ProductFormUiState, viewModel: ProductFormViewModel) {
    ModalBottomSheet(
        onDismissRequest = viewModel::closeEnhance,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = PixColors.Darker,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EnhanceContent(state, viewModel)
        }
    }
}

/**
 * O conteúdo da gaveta: escolher não gera nada; só o botão gera, porque cada
 * geração é paga.
 */
@Composable
private fun EnhanceContent(state: ProductFormUiState, viewModel: ProductFormViewModel) {
    run {
        Text("MELHORAR COM IA", style = PixTypography.inputLabel, color = PixColors.Cyan)

        Chips(
            options = PhotoBackground.entries,
            selected = state.style.background,
            label = { it.label },
            onSelect = viewModel::setBackground,
        )

        if (state.style.background == PhotoBackground.Scene) {
            Chips(options = PhotoScene.entries, selected = state.style.scene, label = { it.label }, onSelect = viewModel::setScene)
            PixelInput(
                value = state.style.sceneHint,
                onValueChange = viewModel::setSceneHint,
                label = "Detalhe do cenário (opcional)",
                placeholder = "com canudo, fundo de praia…",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Text(
            "O rótulo, a marca e as cores ficam como estão; a IA só mexe em volta do produto.",
            style = PixTypography.caption,
            color = PixColors.Gray400,
        )

        val enhanced = state.enhanced

        when {
            state.isEnhancing -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PixelLoader()
                Text("A IA está criando a foto… leva até um minuto.", style = PixTypography.caption, color = PixColors.Gray100)
            }

            enhanced != null && !state.useEnhanced -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        val catalogPhoto = state.catalogChoice?.takeIf { it.hasPhoto && state.photo == null }
                        Box(Modifier.fillMaxWidth().size(140.dp).border(1.dp, PixColors.Gray600)) {
                            if (catalogPhoto != null) {
                                ProductImage(name = catalogPhoto.name, imageUrl = catalogPhoto.imageUrl, initialStyle = PixTypography.caption, modifier = Modifier.fillMaxSize())
                            } else {
                                Thumbnail(state.photo)
                            }
                        }
                        Text("Antes", style = PixTypography.caption, color = PixColors.Gray400)
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().size(140.dp).border(1.dp, PixColors.Cyan)) { Thumbnail(enhanced) }
                        Text("Com IA", style = PixTypography.caption, color = PixColors.Cyan)
                    }
                }
                Text("Confira o rótulo antes de usar: marca, sabor e tamanho têm de ser os da embalagem.", style = PixTypography.caption, color = PixColors.Yellow)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelButton(text = "Usar esta", onClick = viewModel::useEnhanced, modifier = Modifier.weight(1f))
                    PixelButton(text = "Ficar com a original", onClick = viewModel::discardEnhanced, variant = PixelButtonVariant.Secondary, modifier = Modifier.weight(1f))
                }
                PixelButton(text = "Gerar de novo", onClick = viewModel::enhance, enabled = state.canEnhance, variant = PixelButtonVariant.Secondary, modifier = Modifier.fillMaxWidth())
            }

            state.useEnhanced -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Usando a foto criada pela IA.", style = PixTypography.caption, color = PixColors.Green, modifier = Modifier.weight(1f))
                PixelButton(text = "Voltar à original", onClick = viewModel::discardEnhanced, variant = PixelButtonVariant.Secondary)
            }

            else -> PixelButton(
                text = if (state.style.background == PhotoBackground.Scene) "Criar foto com cenário" else "Melhorar a foto",
                onClick = viewModel::enhance,
                enabled = state.canEnhance,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun <T> Chips(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Text(
                label(option),
                style = PixTypography.caption,
                color = if (isSelected) PixColors.Dark else PixColors.Gray100,
                modifier = Modifier
                    .background(if (isSelected) PixColors.Cyan else PixColors.Dark)
                    .border(1.dp, if (isSelected) PixColors.Cyan else PixColors.Gray600)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun Thumbnail(bytes: ByteArray?) {
    val bitmap = remember(bytes) { bytes?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() } }
    bitmap?.let { Image(it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
}

/** "Já existe algo assim": a pessoa diz se é o mesmo ou outro produto. */
@Composable
private fun SimilarBlock(state: ProductFormUiState, viewModel: ProductFormViewModel) {
    if (state.similar.isEmpty()) return

    Text("Já tem algo parecido cadastrado", style = PixTypography.sectionTitle, color = PixColors.Yellow)
    state.similar.forEach { product ->
        SensorCard(
            icon = AppIconType.Box,
            label = "Já cadastrado",
            value = product.name,
            accent = PixColors.Yellow,
            detail = "${formatMoney(product.price)} · ${product.totalStock} un. em estoque",
        )
    }
    PixelButton(
        text = "É outro produto, cadastrar mesmo assim",
        onClick = viewModel::confirmIsNew,
        variant = PixelButtonVariant.Secondary,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** O catálogo da plataforma tem foto e descrição prontas. */
@Composable
private fun CatalogBlock(state: ProductFormUiState, viewModel: ProductFormViewModel) {
    state.catalogChoice?.let { chosen ->
        SensorCard(
            icon = AppIconType.Check,
            label = "Do catálogo da plataforma",
            value = chosen.name,
            accent = PixColors.Green,
            detail = "A foto e a descrição vêm de lá. Toque para usar a sua foto",
            onClick = viewModel::clearCatalog,
        )
        return
    }

    if (state.catalog.isEmpty()) return

    Text("No catálogo da plataforma", style = PixTypography.sectionTitle, color = PixColors.Cyan)
    state.catalog.forEach { item ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PixColors.Darker)
                .border(2.dp, PixColors.Cyan)
                .clickable { viewModel.chooseCatalog(item) }
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(64.dp).background(PixColors.Dark)) {
                ProductImage(name = item.name, imageUrl = item.imageUrl, initialStyle = PixTypography.caption, modifier = Modifier.fillMaxSize())
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.category ?: "Catálogo", style = PixTypography.caption, color = PixColors.Gray400)
                Text(item.name, style = PixTypography.bodySecondary, color = PixColors.Gray100)
                Text(
                    if (item.hasPhoto) "Com foto pronta. Toque para usar" else "Toque para usar",
                    style = PixTypography.caption,
                    color = PixColors.Cyan,
                )
            }
        }
    }
}

@Composable
private fun CreatedStep(state: ProductFormUiState, onAnother: () -> Unit, onDone: () -> Unit) {
    val created = state.created ?: return

    Text("Produto cadastrado!", style = PixTypography.sectionTitle, color = PixColors.Green)
    SensorCard(
        icon = AppIconType.Check,
        label = formatMoney(created.price),
        value = created.name,
        accent = PixColors.Green,
        detail = if (created.inWarehouse > 0) "${created.inWarehouse} un. no depósito, prontas para abastecer" else "Sem estoque ainda: ele entra pela compra ou pela primeira carga",
    )
    PixelButton(text = "Cadastrar outro", onClick = onAnother, modifier = Modifier.fillMaxWidth())
    PixelButton(text = "Pronto", onClick = onDone, variant = PixelButtonVariant.Secondary, modifier = Modifier.fillMaxWidth())
}
