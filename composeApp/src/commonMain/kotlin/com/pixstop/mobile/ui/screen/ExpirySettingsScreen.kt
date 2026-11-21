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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.ProductExpiry
import com.pixstop.mobile.ui.components.AppIcon
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonSize
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelInput
import com.pixstop.mobile.ui.components.PixelLoader
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.ExpirySettingsViewModel
import com.pixstop.mobile.ui.viewmodel.SHELF_LIFE_OPTIONS
import com.pixstop.mobile.ui.viewmodel.TracksChoice
import com.pixstop.mobile.ui.viewmodel.choice
import org.koin.compose.viewmodel.koinViewModel

/**
 * A validade de cada produto no app (VALIDADE.md, V5 d): controla ou não, e
 * o prazo de 30, 60 ou 90 dias. Cada toque grava; não há botão de salvar.
 */
@Composable
fun ExpirySettingsScreen(onBack: () -> Unit, viewModel: ExpirySettingsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark).navigationBarsPadding().imePadding()) {
        PixelScreenTopBar(title = "Validade dos produtos", onBack = onBack)

        if (state.isLoading && state.products.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { PixelLoader() }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    text = "Com validade, cada compra vira um lote com data, a venda tira o que vence antes e o vencido sai da loja. O prazo sugere a data na compra.",
                    style = PixTypography.caption,
                    color = PixColors.Gray400,
                )
            }

            state.error?.let { message ->
                item {
                    Text(
                        text = message,
                        style = PixTypography.caption,
                        color = PixColors.Pink,
                        modifier = Modifier.fillMaxWidth().border(1.dp, PixColors.Pink).clickable(onClick = viewModel::dismissError).padding(12.dp),
                    )
                }
            }

            item {
                PixelInput(
                    value = state.query,
                    onValueChange = viewModel::onQuery,
                    label = "Buscar produto",
                    placeholder = "nome ou categoria",
                    leadingIcon = { AppIcon(icon = AppIconType.Search, contentDescription = null, tint = PixColors.Gray400) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            items(state.visible, key = { it.id }) { product ->
                ProductExpiryRow(
                    product = product,
                    onChoice = { viewModel.setChoice(product, it) },
                    onShelfLife = { viewModel.setShelfLife(product, it) },
                )
            }
        }
    }
}

@Composable
private fun ProductExpiryRow(product: ProductExpiry, onChoice: (TracksChoice) -> Unit, onShelfLife: (Int) -> Unit) {
    val accent = if (product.tracks) PixColors.Cyan else PixColors.Gray600

    Column(
        modifier = Modifier.fillMaxWidth().background(PixColors.Darker).border(2.dp, accent).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = product.name, style = PixTypography.bodySecondary, color = PixColors.Gray100)
        product.category?.let { Text(text = it, style = PixTypography.caption, color = PixColors.Gray400) }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                TracksChoice.Category to "Categoria (${if (product.categoryTracks) "sim" else "não"})",
                TracksChoice.Yes to "Sim",
                TracksChoice.No to "Não",
            ).forEach { (choice, label) ->
                PixelButton(
                    text = label,
                    onClick = { onChoice(choice) },
                    variant = if (product.choice() == choice) PixelButtonVariant.Primary else PixelButtonVariant.Secondary,
                    buttonSize = PixelButtonSize.Small,
                )
            }
        }

        if (product.tracks) {
            Text(text = "PRAZO", style = PixTypography.inputLabel, color = PixColors.Gray400)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SHELF_LIFE_OPTIONS.forEach { days ->
                    PixelButton(
                        text = "$days dias",
                        onClick = { onShelfLife(days) },
                        variant = if (product.shelfLifeDays == days) PixelButtonVariant.Primary else PixelButtonVariant.Secondary,
                        buttonSize = PixelButtonSize.Small,
                    )
                }
            }
            product.shelfLifeDays?.takeIf { it !in SHELF_LIFE_OPTIONS }?.let {
                Text(text = "Prazo atual: $it dias (definido na web)", style = PixTypography.caption, color = PixColors.Gray500)
            }
        }
    }
}
