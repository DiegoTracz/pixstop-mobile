package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.PurchaseStatus
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelEmptyState
import com.pixstop.mobile.ui.components.PixelLoader
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.components.SensorCard
import com.pixstop.mobile.ui.components.formatMoney
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.viewmodel.PurchasesViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * As compras da empresa (docs/plans/COMPRAS_MOBILE.md, passo 5): o rascunho
 * primeiro, porque compra começada e não confirmada é mercadoria na
 * prateleira que o sistema não sabe que existe.
 *
 * Estorno não entra aqui: é decisão de escritório, com o painel na frente.
 */
@Composable
fun PurchasesScreen(
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    onNewReceipt: () -> Unit,
    onOpenShoppingList: () -> Unit,
    viewModel: PurchasesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark).navigationBarsPadding()) {
        PixelScreenTopBar(title = "Compras", onBack = onBack)

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.canReadReceipts) {
                item {
                    PixelButton(text = "Lançar nota", onClick = onNewReceipt, modifier = Modifier.fillMaxWidth())
                }
            }

            item {
                PixelButton(
                    text = "Lista de compras",
                    onClick = onOpenShoppingList,
                    variant = PixelButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            when {
                state.isLoading -> item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { PixelLoader() }
                }

                state.error != null -> item {
                    PixelEmptyState(
                        message = state.error.orEmpty(),
                        isError = true,
                        action = { PixelButton(text = "Tentar de novo", onClick = viewModel::load) },
                    )
                }

                state.purchases.isEmpty() -> item {
                    PixelEmptyState(message = "Nenhuma compra lançada ainda. Na volta do mercado, fotografe a nota aqui.")
                }

                else -> items(state.purchases, key = { it.id }) { purchase ->
                    SensorCard(
                        icon = if (purchase.status == PurchaseStatus.Draft) AppIconType.Edit else AppIconType.Box,
                        label = listOfNotNull(purchase.date?.let(::brDate), purchase.statusLabel).joinToString(" · "),
                        value = purchase.label,
                        accent = when (purchase.status) {
                            PurchaseStatus.Confirmed -> PixColors.Green
                            PurchaseStatus.Draft, PurchaseStatus.Reading -> PixColors.Yellow
                            else -> PixColors.Gray500
                        },
                        detail = "${formatMoney(purchase.total)} · ${purchase.items} itens",
                        onClick = { onOpen(purchase.id) },
                    )
                }
            }
        }
    }
}

/** "2026-10-14" → "14/10/2026". */
private fun brDate(date: String): String = date.split("-").let { if (it.size == 3) "${it[2]}/${it[1]}/${it[0]}" else date }
