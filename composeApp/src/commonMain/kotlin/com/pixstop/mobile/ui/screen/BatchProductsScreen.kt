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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonSize
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelCameraScreen
import com.pixstop.mobile.ui.components.PixelInput
import com.pixstop.mobile.ui.components.PixelLoader
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.components.rememberPhotoPicker
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.BatchProductsUiState
import com.pixstop.mobile.ui.viewmodel.BatchProductsViewModel
import com.pixstop.mobile.ui.viewmodel.BatchRow
import com.pixstop.mobile.ui.viewmodel.BatchStatus
import org.koin.compose.viewmodel.koinViewModel

/**
 * Vários produtos numa foto (APP_OPERADOR_PRODUTOS.md, G4.1).
 *
 * A compra na mesa, uma foto, e a lista para conferir: o recorte de cada
 * produto, o nome e o preço que a foto sugeriu, e o custo para a pessoa
 * dizer. Com parecido já cadastrado, a linha começa de fora.
 */
@Composable
fun BatchProductsScreen(
    onBack: () -> Unit,
    viewModel: BatchProductsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val pickFromGallery = rememberPhotoPicker { photo -> viewModel.onPhoto(photo) }

    if (state.cameraOpen) {
        PixelCameraScreen(
            onPhoto = viewModel::onPhoto,
            onDismiss = viewModel::closeCamera,
            title = "VÁRIOS PRODUTOS",
            instruction = "Todos na mesa, rótulos de frente, sem um tampar o outro.",
            startWithFront = false,
            squareFrame = false,
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark).navigationBarsPadding().imePadding()) {
        PixelScreenTopBar(title = "Vários produtos", onBack = onBack)

        state.error?.let {
            Text(
                it,
                style = PixTypography.caption,
                color = PixColors.Pink,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).border(1.dp, PixColors.Pink).clickable(onClick = viewModel::dismissError).padding(12.dp),
            )
        }

        when {
            state.photo == null -> Start(onCamera = viewModel::openCamera, onGallery = pickFromGallery)

            state.isReading -> Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PixelLoader()
                Text("Separando os produtos da foto…", style = PixTypography.caption, color = PixColors.Gray100)
                Text("Leva até um minuto.", style = PixTypography.caption, color = PixColors.Gray400)
            }

            state.isDone -> Done(state, onAgain = viewModel::restart, onBack = onBack)

            else -> {
                Review(state, viewModel, modifier = Modifier.weight(1f))
                Footer(state, viewModel)
            }
        }
    }
}

@Composable
private fun Start(onCamera: () -> Unit, onGallery: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Cadastrar vários de uma vez", style = PixTypography.sectionTitle, color = PixColors.Cyan)
        Text(
            "Ponha a compra na mesa, um de cada produto, com os rótulos de frente. A IA separa cada um, sugere nome e preço, e você só diz o custo.",
            style = PixTypography.caption,
            color = PixColors.Gray300,
        )
        PixelButton(text = "Fotografar a mesa", onClick = onCamera, modifier = Modifier.fillMaxWidth())
        PixelButton(text = "Escolher da galeria", onClick = onGallery, variant = PixelButtonVariant.Secondary, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Review(state: BatchProductsUiState, viewModel: BatchProductsViewModel, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "${state.rows.size} produto(s) na foto. Confira o nome, diga o custo e o preço.",
                style = PixTypography.caption,
                color = PixColors.Yellow,
            )
            if (state.truncated) {
                Text("A foto tinha mais de ${state.limit}: tire outra com os que faltaram.", style = PixTypography.caption, color = PixColors.Pink)
            }
        }

        itemsIndexed(state.rows) { index, row ->
            RowCard(row, enabled = !state.isSaving, viewModel = viewModel, index = index)
        }

        item {
            PixelButton(
                text = "Outra foto",
                onClick = viewModel::restart,
                variant = PixelButtonVariant.Secondary,
                buttonSize = PixelButtonSize.Small,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RowCard(row: BatchRow, enabled: Boolean, viewModel: BatchProductsViewModel, index: Int) {
    val accent = when {
        row.status == BatchStatus.Saved -> PixColors.Green
        row.status is BatchStatus.Failed -> PixColors.Pink
        !row.included -> PixColors.Gray700
        else -> PixColors.Cyan
    }
    val editable = enabled && row.included && row.status != BatchStatus.Saved

    Column(
        modifier = Modifier.fillMaxWidth().background(PixColors.Darker).border(2.dp, accent).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Crop(row.item.photo)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    listOfNotNull(row.item.category?.name, "${row.item.quantity} na foto").joinToString(" · "),
                    style = PixTypography.caption,
                    color = PixColors.Gray400,
                )
                when (val status = row.status) {
                    BatchStatus.Saved -> Text("Cadastrado", style = PixTypography.caption, color = PixColors.Green)
                    BatchStatus.Saving -> Text("Salvando…", style = PixTypography.caption, color = PixColors.Gray100)
                    is BatchStatus.Failed -> Text(status.message, style = PixTypography.caption, color = PixColors.Pink)
                    BatchStatus.Waiting -> Unit
                }
            }
            if (row.status != BatchStatus.Saved) {
                PixelButton(
                    text = if (row.included) "Tirar" else "Incluir",
                    onClick = { viewModel.setIncluded(index, !row.included) },
                    variant = PixelButtonVariant.Secondary,
                    buttonSize = PixelButtonSize.Small,
                    enabled = enabled,
                )
            }
        }

        row.item.similar.firstOrNull()?.takeIf { row.status != BatchStatus.Saved }?.let { similar ->
            Text(
                if (row.included) "Já existe \"${similar.name}\": confira se não é o mesmo." else "Já existe \"${similar.name}\": ficou de fora. Toque em Incluir se for outro.",
                style = PixTypography.caption,
                color = PixColors.Yellow,
            )
        }

        if (row.included) {
            PixelInput(
                value = row.name,
                onValueChange = { viewModel.setName(index, it) },
                label = "Nome",
                enabled = editable,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelInput(
                    value = row.cost,
                    onValueChange = { viewModel.setCost(index, it) },
                    label = "Custo",
                    placeholder = "0,00",
                    enabled = editable,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                PixelInput(
                    value = row.price,
                    onValueChange = { viewModel.setPrice(index, it) },
                    label = "Preço",
                    placeholder = "0,00",
                    enabled = editable,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Text(row.name, style = PixTypography.bodySecondary, color = PixColors.Gray500)
        }
    }
}

@Composable
private fun Crop(bytes: ByteArray?) {
    val bitmap = remember(bytes) { bytes?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() } }

    Box(Modifier.size(72.dp).background(PixColors.Dark).border(1.dp, PixColors.Gray700), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text("sem\nrecorte", style = PixTypography.caption, color = PixColors.Gray500)
        }
    }
}

@Composable
private fun Footer(state: BatchProductsUiState, viewModel: BatchProductsViewModel) {
    Column(modifier = Modifier.fillMaxWidth().background(PixColors.Darker).padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (state.missingCost > 0 && !state.isSaving) {
            Text(
                if (state.missingCost == 1) "Falta o custo de 1 produto." else "Falta o custo de ${state.missingCost} produtos.",
                style = PixTypography.caption,
                color = PixColors.Gray400,
            )
        }
        PixelButton(
            text = "Cadastrar ${state.toSave.size} produto(s)",
            onClick = viewModel::save,
            enabled = state.canSave,
            isLoading = state.isSaving,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}

@Composable
private fun Done(state: BatchProductsUiState, onAgain: () -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("${state.saved} produto(s) cadastrado(s)!", style = PixTypography.sectionTitle, color = PixColors.Green)
        Text(
            "O estoque deles entra pela compra (nota) ou pela primeira carga da geladeira.",
            style = PixTypography.caption,
            color = PixColors.Gray300,
        )
        PixelButton(text = "Outra foto", onClick = onAgain, modifier = Modifier.fillMaxWidth())
        PixelButton(text = "Pronto", onClick = onBack, variant = PixelButtonVariant.Secondary, modifier = Modifier.fillMaxWidth())
    }
}
