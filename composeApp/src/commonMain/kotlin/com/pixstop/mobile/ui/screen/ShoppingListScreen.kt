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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.MascotFace
import com.pixstop.mobile.domain.model.ShoppingRow
import com.pixstop.mobile.ui.components.AppIcon
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonSize
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelEmptyState
import com.pixstop.mobile.ui.components.PixelLoader
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.components.QuantityStepper
import com.pixstop.mobile.ui.components.SensorCard
import com.pixstop.mobile.ui.components.formatMoney
import com.pixstop.mobile.ui.components.pixelShadow
import com.pixstop.mobile.ui.components.rememberShareText
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.ShoppingListUiState
import com.pixstop.mobile.ui.viewmodel.ShoppingListViewModel
import com.pixstop.mobile.ui.viewmodel.WEEKDAYS
import org.koin.compose.viewmodel.koinViewModel

/**
 * A lista de compras da semana (docs/plans/COMPRAS_MOBILE.md, passo 1).
 *
 * Uma tela de leitura: o que falta, quanto, e um botão de mandar para o grupo
 * de quem compra. A quantidade editada fica só aqui e no texto.
 *
 * @param canManage quem administra a empresa vê o ritmo de compra; o repositor compra com o ritmo que a empresa definiu
 */
@Composable
fun ShoppingListScreen(
    onBack: () -> Unit,
    onOpenPurchase: (Long) -> Unit,
    canManage: Boolean,
    viewModel: ShoppingListViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val share = rememberShareText()

    LaunchedEffect(Unit) { viewModel.load() }

    if (state.settingsOpen) {
        SettingsPanel(state, viewModel)

        return
    }

    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark).navigationBarsPadding()) {
        PixelScreenTopBar(
            title = state.list?.company?.ifBlank { null }?.let { "Compras · $it" } ?: "Lista de compras",
            onBack = onBack,
            action = {
                if (canManage) {
                    Box(modifier = Modifier.clickable(onClick = viewModel::openSettings).padding(8.dp)) {
                        AppIcon(icon = AppIconType.Settings, contentDescription = "Ritmo de compra", tint = PixColors.Cyan, modifier = Modifier.size(20.dp))
                    }
                }
            },
        )

        val list = state.list

        when {
            state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { PixelLoader() }

            list == null -> PixelEmptyState(
                message = state.error ?: "Não foi possível carregar a lista.",
                isError = true,
                modifier = Modifier.padding(24.dp),
                action = { PixelButton(text = "Tentar de novo", onClick = { viewModel.load() }) },
            )

            else -> LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        text = "Para ${list.coverDays} dias, com ${list.safetyPercent}% de folga. A geladeira vende " +
                            "${formatDecimal(list.sellingDaysPerWeek)} dias por semana.",
                        style = PixTypography.caption,
                        color = PixColors.Gray400,
                    )
                }

                state.error?.let { item { ListNotice(it, PixColors.Pink, viewModel::dismissMessage) } }
                state.message?.let { item { ListNotice(it, PixColors.Green, viewModel::dismissMessage) } }

                if (list.appliances.size > 1) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                PixelButton(
                                    text = "Empresa inteira",
                                    onClick = { viewModel.load(null) },
                                    variant = if (list.applianceId == null) PixelButtonVariant.Primary else PixelButtonVariant.Secondary,
                                    buttonSize = PixelButtonSize.Small,
                                )
                            }
                            items(list.appliances, key = { it.id }) { appliance ->
                                PixelButton(
                                    text = appliance.name,
                                    onClick = { viewModel.load(appliance.id) },
                                    variant = if (list.applianceId == appliance.id) PixelButtonVariant.Primary else PixelButtonVariant.Secondary,
                                    buttonSize = PixelButtonSize.Small,
                                )
                            }
                        }
                    }
                }

                if (state.rows.isEmpty()) {
                    item {
                        PixelEmptyState(message = "Nada a comprar: o estoque cobre até a próxima compra.", face = MascotFace.Happy)
                    }
                } else {
                    item {
                        SensorCard(
                            icon = AppIconType.Cart,
                            label = "Total estimado",
                            value = formatMoney(state.totalCost),
                            detail = "${state.totalUnits} unidades em ${state.rows.size} produtos" +
                                if (state.zeroed > 0) " · ${state.zeroed} zerados" else "",
                        )
                    }

                    state.groups.forEach { (category, rows) ->
                        item(key = "cat-$category") {
                            Text(text = category.uppercase(), style = PixTypography.inputLabel, color = PixColors.Cyan, modifier = Modifier.padding(top = 4.dp))
                        }
                        items(rows, key = { it.productId }) { row ->
                            ShoppingRowCard(row, state.quantityOf(row)) { viewModel.setQuantity(row.productId, it) }
                        }
                    }
                }

                if (list.covered.isNotEmpty()) {
                    item {
                        Text(
                            text = "✓ Não precisa: " + list.covered.joinToString(", ") { "${it.name} (${it.stock})" },
                            style = PixTypography.caption,
                            color = PixColors.Gray500,
                        )
                    }
                }
            }
        }

        if (list != null && state.rows.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PixelButton(
                    text = "Enviar",
                    onClick = { if (!share(state.messageText)) viewModel.notifyCopied() },
                    variant = PixelButtonVariant.Secondary,
                    enabled = state.totalUnits > 0,
                    modifier = Modifier.weight(1f),
                )
                PixelButton(
                    text = "Vou comprar",
                    onClick = { viewModel.startPurchase(onOpenPurchase) },
                    enabled = state.totalUnits > 0 && !state.isWorking,
                    isLoading = state.isWorking,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ShoppingRowCard(row: ShoppingRow, quantity: Int, onChange: (Int) -> Unit) {
    val accent = if (row.zeroedSince != null) PixColors.Pink else PixColors.Cyan

    Box(modifier = Modifier.fillMaxWidth().pixelShadow(color = accent, offsetX = 4.dp, offsetY = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(PixColors.Darker).border(2.dp, accent).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = row.name, style = PixTypography.bodySecondary, color = PixColors.Gray100)
                Text(
                    text = "tem ${row.stock} · vende ~${formatDecimal(row.weeklyRate)}/sem",
                    style = PixTypography.caption,
                    color = PixColors.Gray400,
                )
                row.zeroedSince?.let {
                    Text(text = "● zerado desde ${shortDate(it)}", style = PixTypography.caption, color = PixColors.Pink)
                }
            }

            QuantityStepper(value = quantity, onChange = onChange)
        }
    }
}

/** O ritmo da empresa: de quantos em quantos dias compra, quanto de folga, e o dia do aviso. */
@Composable
private fun SettingsPanel(state: ShoppingListUiState, viewModel: ShoppingListViewModel) {
    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark).navigationBarsPadding()) {
        PixelScreenTopBar(title = "Ritmo de compra", onBack = viewModel::closeSettings)

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            state.error?.let { ListNotice(it, PixColors.Pink, viewModel::dismissMessage) }

            Text(text = "COMPRAR A CADA (DIAS)", style = PixTypography.inputLabel, color = PixColors.Gray400)
            QuantityStepper(value = state.coverDays, onChange = viewModel::onCoverDays, min = 1, max = 30)

            Text(text = "FOLGA (%)", style = PixTypography.inputLabel, color = PixColors.Gray400)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 10, 20, 30, 50).forEach { percent ->
                    PixelButton(
                        text = "$percent%",
                        onClick = { viewModel.onSafety(percent) },
                        variant = if (state.safetyPercent == percent) PixelButtonVariant.Primary else PixelButtonVariant.Secondary,
                        buttonSize = PixelButtonSize.Small,
                    )
                }
            }

            Text(text = "AVISAR NA", style = PixTypography.inputLabel, color = PixColors.Gray400)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(WEEKDAYS.indices.toList()) { index ->
                    PixelButton(
                        text = WEEKDAYS[index],
                        onClick = { viewModel.onWeekday(index) },
                        variant = if (state.weekday == index) PixelButtonVariant.Primary else PixelButtonVariant.Secondary,
                        buttonSize = PixelButtonSize.Small,
                    )
                }
                item {
                    PixelButton(
                        text = "não avisar",
                        onClick = { viewModel.onWeekday(null) },
                        variant = if (state.weekday == null) PixelButtonVariant.Primary else PixelButtonVariant.Secondary,
                        buttonSize = PixelButtonSize.Small,
                    )
                }
            }

            PixelButton(
                text = "Salvar",
                onClick = viewModel::saveSettings,
                isLoading = state.isWorking,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ListNotice(text: String, color: Color, onDismiss: () -> Unit) {
    Text(
        text = text,
        style = PixTypography.caption,
        color = color,
        modifier = Modifier.fillMaxWidth().border(1.dp, color).clickable(onClick = onDismiss).padding(12.dp),
    )
}

/** 9.8 → "9,8"; 5.0 → "5". */
private fun formatDecimal(value: Double): String {
    val tenths = kotlin.math.round(value * 10).toLong()
    return if (tenths % 10 == 0L) (tenths / 10).toString() else "${tenths / 10},${kotlin.math.abs(tenths % 10)}"
}

/** "2026-10-16" → "16/10". */
private fun shortDate(date: String): String = date.split("-").let { if (it.size == 3) "${it[2]}/${it[1]}" else date }
