package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.HourSales
import com.pixstop.mobile.domain.model.OperatorAttention
import com.pixstop.mobile.domain.model.OperatorInsightCompany
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonSize
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelLoader
import com.pixstop.mobile.ui.components.SensorCard
import com.pixstop.mobile.ui.components.formatMoney
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.HeadlineCard
import com.pixstop.mobile.ui.viewmodel.OperatorHomeViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * A tela inicial de quem opera geladeiras (PAINEL_ESTOQUE.md, D1).
 *
 * Responde à pergunta da manhã: onde preciso ir hoje e por quê. Primeiro os
 * números do dia, depois o que precisa de alguém (porta fora do ar, vencido,
 * zerado que vende), as vendas por hora e as empresas. Tocar num aviso ou
 * numa empresa entra nela e abre as geladeiras.
 */
@Composable
fun OperatorHomeScreen(
    greeting: String,
    onOpenCompany: (String) -> Unit,
    onOpenRound: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OperatorHomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val insights = state.insights

    if (insights == null && state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { PixelLoader() }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(PixColors.Dark),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(text = greeting, style = PixTypography.bodyMuted)
                Text(text = insights?.operatorName ?: "Operador", style = PixTypography.sectionTitle, color = PixColors.Gray100)
                insights?.let {
                    Text(
                        text = "${it.totals.companies} empresa(s) · ${it.totals.appliances} geladeira(s)",
                        style = PixTypography.caption,
                        color = PixColors.Gray400,
                    )
                }
            }
        }

        state.error?.let { error ->
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = error, style = PixTypography.errorText, modifier = Modifier.weight(1f))
                    PixelButton(text = "Tentar de novo", onClick = viewModel::load, buttonSize = PixelButtonSize.Small, variant = PixelButtonVariant.Secondary)
                }
            }
        }

        if (state.headline.isNotEmpty()) {
            item { Headline(state.headline) }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelButton(text = "Ronda e compras", onClick = onOpenRound, buttonSize = PixelButtonSize.Small)
                PixelButton(
                    text = if (state.isLoading) "Atualizando…" else "Atualizar",
                    onClick = viewModel::load,
                    enabled = !state.isLoading,
                    buttonSize = PixelButtonSize.Small,
                    variant = PixelButtonVariant.Secondary,
                )
            }
        }

        item { SectionLabel("PRECISA DE ATENÇÃO") }

        if (state.allGood) {
            item {
                SensorCard(
                    icon = AppIconType.Check,
                    label = "Tudo em ordem",
                    value = "Nada zerado, vencido ou fora do ar",
                    accent = PixColors.Green,
                )
            }
        }

        items(state.visibleAttention, key = { "${it.companyId}-${it.kind}-${it.title}" }) { item ->
            AttentionCard(item, seesMoney = insights?.seesMoney == true, onClick = { onOpenCompany(item.companyId) })
        }

        if (state.canToggleAttention) {
            item {
                PixelButton(
                    text = if (state.showAllAttention) "Mostrar menos" else "Ver mais ${state.hiddenAttention}",
                    onClick = viewModel::toggleAllAttention,
                    buttonSize = PixelButtonSize.Small,
                    variant = PixelButtonVariant.Secondary,
                )
            }
        }

        if (state.chartHours.isNotEmpty()) {
            item { SectionLabel("VENDAS POR HORA · hoje × média") }
            item { HourChart(state.chartHours) }
        }

        insights?.companies?.takeIf { it.isNotEmpty() }?.let { companies ->
            item { SectionLabel("EMPRESAS") }
            items(companies, key = { it.id }) { company ->
                CompanyCard(company, seesMoney = insights.seesMoney, onClick = { onOpenCompany(company.id) })
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, style = PixTypography.inputLabel, color = PixColors.Cyan)
}

/** Os números do dia, dois por linha. */
@Composable
private fun Headline(cards: List<HeadlineCard>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cards.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { card ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(PixColors.Darker)
                            .border(2.dp, if (card.isAlert) PixColors.Pink else PixColors.Gray700)
                            .padding(10.dp),
                    ) {
                        Text(text = card.label, style = PixTypography.caption, color = PixColors.Gray400)
                        Text(
                            text = card.value,
                            style = PixTypography.sectionTitle,
                            color = if (card.isAlert) PixColors.Pink else PixColors.Gray100,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (row.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AttentionCard(item: OperatorAttention, seesMoney: Boolean, onClick: () -> Unit) {
    val (icon, accent) = when (item.kind) {
        "offline" -> AppIconType.WifiOff to PixColors.Pink
        "expired" -> AppIconType.Info to PixColors.Pink
        "zeroed" -> AppIconType.Box to PixColors.Pink
        else -> AppIconType.Box to PixColors.Yellow
    }

    SensorCard(
        icon = icon,
        label = item.companyName,
        value = item.title,
        detail = listOfNotNull(
            item.detail,
            item.costPerDay?.takeIf { seesMoney && it > 0 }?.let {
                if (item.kind == "expired" || item.kind == "expiring") "${formatMoney(it)} no custo" else "${formatMoney(it)}/dia"
            },
        ).joinToString(" · ").ifBlank { null },
        accent = accent,
        onClick = onClick,
    )
}

@Composable
private fun CompanyCard(company: OperatorInsightCompany, seesMoney: Boolean, onClick: () -> Unit) {
    val status = when {
        company.offline > 0 -> "${company.offline} fora do ar"
        company.attention > 0 -> "${company.attention} aviso(s)"
        else -> "tudo em ordem"
    }

    SensorCard(
        icon = if (company.offline > 0) AppIconType.WifiOff else AppIconType.Store,
        label = company.name,
        value = status,
        detail = listOfNotNull(
            "${company.soldToday} un. hoje",
            company.revenueToday?.takeIf { seesMoney }?.let { formatMoney(it) },
        ).joinToString(" · "),
        accent = when {
            company.offline > 0 || company.zeroed > 0 || company.expired > 0 -> PixColors.Pink
            company.attention > 0 -> PixColors.Yellow
            else -> PixColors.Green
        },
        onClick = onClick,
    )
}

/** Barras de hoje por hora, com um traço amarelo na média do mesmo dia da semana. */
@Composable
private fun HourChart(hours: List<HourSales>) {
    val max = hours.maxOf { maxOf(it.today.toDouble(), it.average) }.coerceAtLeast(1.0)
    // As cores do tema vêm da composição; o Canvas desenha fora dela.
    val barColor = PixColors.Cyan
    val averageColor = PixColors.Yellow

    Column(
        modifier = Modifier.fillMaxWidth().background(PixColors.Darker).border(2.dp, PixColors.Gray700).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
            val slot = size.width / hours.size
            val barWidth = slot * 0.6f

            hours.forEachIndexed { index, hour ->
                val left = index * slot + (slot - barWidth) / 2
                val barHeight = (hour.today / max * size.height).toFloat()
                drawRect(
                    color = barColor,
                    topLeft = Offset(left, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                )

                if (hour.average > 0) {
                    val y = (size.height - hour.average / max * size.height).toFloat()
                    drawRect(
                        color = averageColor,
                        topLeft = Offset(index * slot, y - 2f),
                        size = Size(slot, 4f),
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = "${hours.first().hour}h", style = PixTypography.caption, color = PixColors.Gray400, modifier = Modifier.weight(1f))
            Text(text = "${hours.last().hour}h", style = PixTypography.caption, color = PixColors.Gray400)
        }

        Text(
            text = "Hoje ${hours.sumOf { it.today }} un. · média ${hours.sumOf { it.average }.toInt()} un.",
            style = PixTypography.caption,
            color = PixColors.Gray400,
        )
    }
}
