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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.pixstop.mobile.domain.model.FridgeProduct
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelInput
import com.pixstop.mobile.ui.components.PixelLoader
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.components.QrCodeScannerScreen
import com.pixstop.mobile.ui.components.QuantityStepper
import com.pixstop.mobile.ui.components.SensorCard
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.FridgeProductsUiState
import com.pixstop.mobile.ui.viewmodel.FridgeProductsViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * "Produtos desta geladeira" (APP_OPERADOR_PRODUTOS.md, G2).
 *
 * Em cima o que mora nela, com quanto tem; embaixo o resto do cadastro, com
 * o que há no depósito. Cada linha tem um contador de quanto pôr agora, e um
 * botão só abastece tudo de uma vez.
 */
@Composable
fun FridgeProductsScreen(
    applianceId: Long,
    onBack: () -> Unit,
    onNewProduct: () -> Unit,
    viewModel: FridgeProductsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    // Volta do cadastro de produto com ele já na lista.
    LifecycleResumeEffect(applianceId) {
        viewModel.load(applianceId)
        onPauseOrDispose {}
    }

    if (state.scannerOpen) {
        QrCodeScannerScreen(
            onCodeScanned = viewModel::onBarcode,
            onDismiss = viewModel::closeScanner,
            instruction = "Aponte para o código de barras da embalagem",
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark).navigationBarsPadding()) {
        PixelScreenTopBar(title = state.products?.applianceName ?: "Produtos", onBack = onBack)

        (state.error ?: state.message)?.let { text ->
            Text(
                text,
                style = PixTypography.caption,
                color = if (state.error != null) PixColors.Pink else PixColors.Green,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .border(1.dp, if (state.error != null) PixColors.Pink else PixColors.Green)
                    .clickable(onClick = viewModel::dismissMessage)
                    .padding(12.dp),
            )
        }

        if (state.isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { PixelLoader() }
        } else {
            ProductList(state, viewModel, onNewProduct, modifier = Modifier.weight(1f))
        }

        if (state.units > 0) {
            if (state.firstLoadUnits > 0) {
                Text(
                    "${state.firstLoadUnits} un. sem estoque no depósito entram como primeira carga (acerto de estoque, sem nota).",
                    style = PixTypography.caption,
                    color = PixColors.Yellow,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            PixelButton(
                text = "Pôr ${state.units} unidade(s) na geladeira",
                onClick = viewModel::restock,
                enabled = state.canRestock,
                isLoading = state.isSaving,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
    }
}

@Composable
private fun ProductList(state: FridgeProductsUiState, viewModel: FridgeProductsViewModel, onNewProduct: () -> Unit, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                PixelInput(
                    value = state.query,
                    onValueChange = viewModel::onQuery,
                    label = "Buscar",
                    placeholder = "nome, categoria ou código",
                    modifier = Modifier.weight(1f),
                )
                PixelButton(text = "Ler", onClick = viewModel::openScanner, variant = PixelButtonVariant.Secondary)
            }
        }

        item {
            SensorCard(
                icon = AppIconType.Add,
                label = "Não achou o produto?",
                value = "Cadastrar produto",
                detail = "Fotografe: a IA preenche nome, categoria e preço",
                onClick = onNewProduct,
            )
        }

        if (state.hasNoProducts) {
            item {
                Text(
                    "A empresa ainda não tem produtos. Cadastre o primeiro acima e depois ponha na geladeira.",
                    style = PixTypography.caption,
                    color = PixColors.Yellow,
                )
            }
        }

        if (state.here.isNotEmpty()) {
            item { Label("NESTA GELADEIRA") }
            items(state.here, key = { "here-${it.id}" }) { product ->
                ProductRow(product, adding = state.addingOf(product.id), showsHere = true) { viewModel.setAdding(product, it) }
            }
        } else if (state.isEmptyFridge && !state.hasNoProducts) {
            item {
                Text(
                    "A geladeira está vazia. Escolha embaixo o que vai nela e quantas unidades.",
                    style = PixTypography.caption,
                    color = PixColors.Yellow,
                )
            }
        }

        if (state.others.isNotEmpty()) {
            item { Label("PÔR NESTA GELADEIRA") }
            items(state.others, key = { "other-${it.id}" }) { product ->
                ProductRow(product, adding = state.addingOf(product.id), showsHere = false) { viewModel.setAdding(product, it) }
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text = text, style = PixTypography.inputLabel, color = PixColors.Cyan, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun ProductRow(product: FridgeProduct, adding: Int, showsHere: Boolean, onChange: (Int) -> Unit) {
    val accent = when {
        adding > 0 -> PixColors.Cyan
        showsHere && product.quantity == 0 -> PixColors.Pink
        else -> PixColors.Gray700
    }

    Row(
        modifier = Modifier.fillMaxWidth().background(PixColors.Darker).border(2.dp, accent).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            product.category?.let { Text(it.uppercase(), style = PixTypography.inputLabel, color = PixColors.Gray400) }
            Text(product.name, style = PixTypography.bodySecondary, color = PixColors.Gray100)
            Text(
                buildString {
                    if (showsHere) append("${product.quantity} aqui · ")
                    append(if (product.inWarehouse > 0) "${product.inWarehouse} no depósito" else "nada no depósito: entra como primeira carga")
                },
                style = PixTypography.caption,
                color = if (showsHere && product.quantity == 0) PixColors.Pink else PixColors.Gray400,
            )
        }

        QuantityStepper(
            value = adding,
            onChange = onChange,
            max = if (product.inWarehouse > 0) product.inWarehouse else FridgeProductsUiState.MAX_FIRST_LOAD,
        )
    }
}
