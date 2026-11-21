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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.ExpiryLot
import com.pixstop.mobile.domain.model.ExpiryPlace
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.ui.components.HoldToConfirmButton
import com.pixstop.mobile.ui.components.PixelLoader
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.components.SensorCard
import com.pixstop.mobile.ui.components.pixelShadow
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.ExpiryUiState
import com.pixstop.mobile.ui.viewmodel.ExpiryViewModel
import com.pixstop.mobile.ui.viewmodel.shortDate
import com.pixstop.mobile.ui.viewmodel.whenLabel
import org.koin.compose.viewmodel.koinViewModel

/**
 * "Validade" no app (docs/plans/VALIDADE.md, V5 c): o vencido para retirar
 * e o que vence logo, por lugar. É onde o aviso diário do celular abre.
 */
@Composable
fun ExpiryScreen(onBack: () -> Unit, onSettings: () -> Unit = {}, viewModel: ExpiryViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark).navigationBarsPadding()) {
        PixelScreenTopBar(title = "Validade", onBack = onBack)

        val overview = state.overview

        if (overview == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.isLoading) {
                    PixelLoader()
                } else {
                    state.error?.let { Text(text = it, style = PixTypography.caption, color = PixColors.Pink) }
                }
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "O vencido não aparece mais na loja, mas continua na prateleira até alguém retirar.",
                    style = PixTypography.caption,
                    color = PixColors.Gray400,
                )
            }

            state.message?.let { item { ExpiryNotice(it, PixColors.Green, viewModel::dismissMessage) } }
            state.error?.let { item { ExpiryNotice(it, PixColors.Pink, viewModel::dismissMessage) } }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SensorCard(
                        icon = AppIconType.Warning,
                        label = "Vencidas",
                        value = overview.expiredUnits.toString(),
                        accent = if (overview.expiredUnits > 0) PixColors.Pink else PixColors.Gray600,
                        modifier = Modifier.weight(1f),
                    )
                    SensorCard(
                        icon = AppIconType.Box,
                        label = "Vencendo",
                        value = overview.warningUnits.toString(),
                        accent = if (overview.warningUnits > 0) PixColors.Yellow else PixColors.Gray600,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                SensorCard(
                    icon = AppIconType.Box,
                    label = "Quais produtos controlam",
                    value = "Validade dos produtos",
                    detail = "Controla ou não, e o prazo de 30, 60 ou 90 dias",
                    onClick = onSettings,
                )
            }

            item {
                Text(
                    text = if (state.showAll) "✓ Mostrando todos os lotes" else "Mostrar todos os lotes",
                    style = PixTypography.caption,
                    color = PixColors.Cyan,
                    modifier = Modifier.clickable(onClick = viewModel::toggleShowAll).padding(vertical = 4.dp),
                )
            }

            if (state.isEmpty) {
                item {
                    Text(
                        text = "Nenhum produto com controle de validade em estoque. Ligue o controle na categoria ou no produto.",
                        style = PixTypography.caption,
                        color = PixColors.Gray400,
                    )
                }
            }

            items(overview.places, key = { it.applianceId ?: 0L }) { place ->
                PlaceCard(place, state, onRemove = { viewModel.removeExpired(place) })
            }
        }
    }
}

@Composable
private fun PlaceCard(place: ExpiryPlace, state: ExpiryUiState, onRemove: () -> Unit) {
    val accent = if (place.expiredUnits > 0) PixColors.Pink else PixColors.Gray600
    val lots = state.visibleLots(place)

    Box(modifier = Modifier.fillMaxWidth().pixelShadow(color = accent, offsetX = 4.dp, offsetY = 4.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().background(PixColors.Darker).border(2.dp, accent).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = place.name.uppercase(), style = PixTypography.inputLabel, color = PixColors.Cyan)

            if (lots.isEmpty() && place.lots.isNotEmpty()) {
                Text(text = "Nada vencendo aqui.", style = PixTypography.caption, color = PixColors.Gray400)
            }

            lots.forEach { LotRow(it) }

            if (place.undated.isNotEmpty()) {
                Text(
                    text = "Sem data: " + place.undated.joinToString(", ") { "${it.product} (${it.quantity})" },
                    style = PixTypography.caption,
                    color = PixColors.Gray500,
                )
            }

            if (place.expiredUnits > 0) {
                HoldToConfirmButton(
                    text = if (state.removing == place) {
                        "Retirando…"
                    } else {
                        "Segure: retirar ${place.expiredUnits} vencida${if (place.expiredUnits == 1) "" else "s"}"
                    },
                    enabled = state.removing == null,
                    onConfirm = onRemove,
                    color = PixColors.Pink,
                )
            }
        }
    }
}

@Composable
private fun LotRow(lot: ExpiryLot) {
    val color = when {
        lot.expired -> PixColors.Pink
        lot.warning -> PixColors.Yellow
        else -> PixColors.Gray400
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = lot.product, style = PixTypography.bodySecondary, color = PixColors.Gray100)
            Text(text = "${lot.quantity} un. · ${shortDate(lot.expiresOn)}", style = PixTypography.caption, color = PixColors.Gray400)
        }
        Text(text = lot.whenLabel(), style = PixTypography.caption, color = color)
    }
}

@Composable
private fun ExpiryNotice(text: String, color: Color, onDismiss: () -> Unit) {
    Text(
        text = text,
        style = PixTypography.caption,
        color = color,
        modifier = Modifier.fillMaxWidth().border(1.dp, color).clickable(onClick = onDismiss).padding(12.dp),
    )
}
