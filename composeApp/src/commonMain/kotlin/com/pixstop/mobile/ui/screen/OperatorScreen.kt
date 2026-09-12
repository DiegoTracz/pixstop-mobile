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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.OperatorAlert
import com.pixstop.mobile.domain.model.OperatorCompany
import com.pixstop.mobile.domain.model.OperatorStockRow
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonSize
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.components.SensorCard
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.OperatorTab
import com.pixstop.mobile.ui.viewmodel.OperatorViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * O painel de quem opera geladeiras, no celular (Fase 16, O8).
 *
 * É a tela da rua: a ronda diz o que entra no carro hoje, e as empresas dizem
 * onde. As portas fora do ar vêm antes de tudo, porque elas impedem a venda
 * enquanto o estoque baixo apenas a limita.
 *
 * Dinheiro só aparece para quem o vê: o servidor não envia receita nem margem
 * ao repositor, e a tela não inventa o que não recebeu.
 */
@Composable
fun OperatorScreen(
    onBack: () -> Unit,
    onOpenCompany: (String) -> Unit = {},
    viewModel: OperatorViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark)) {
        PixelScreenTopBar(
            title = state.round?.operatorName ?: state.companies?.operatorName ?: "Operador",
            onBack = onBack,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PixelButton(
                text = "Ronda",
                onClick = { viewModel.selectTab(OperatorTab.Round) },
                buttonSize = PixelButtonSize.Small,
                variant = if (state.tab == OperatorTab.Round) PixelButtonVariant.Primary else PixelButtonVariant.Secondary,
            )
            PixelButton(
                text = "Empresas",
                onClick = { viewModel.selectTab(OperatorTab.Companies) },
                buttonSize = PixelButtonSize.Small,
                variant = if (state.tab == OperatorTab.Companies) PixelButtonVariant.Primary else PixelButtonVariant.Secondary,
            )
        }

        when {
            state.isLoading && state.round == null && state.companies == null ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PixColors.Cyan)
                }

            state.tab == OperatorTab.Round -> RoundList(
                offline = state.round?.offline.orEmpty(),
                stock = state.round?.stock.orEmpty(),
                onlyLow = state.onlyLow,
                error = state.error,
                onToggleOnlyLow = viewModel::toggleOnlyLow,
            )

            else -> CompanyList(
                companies = state.companies?.companies.orEmpty(),
                seesMoney = state.companies?.seesMoney == true,
                error = state.error,
                onOpen = onOpenCompany,
            )
        }
    }
}

@Composable
private fun RoundList(
    offline: List<OperatorAlert>,
    stock: List<OperatorStockRow>,
    onlyLow: Boolean,
    error: String?,
    onToggleOnlyLow: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (error != null) {
            item { Text(text = error, style = PixTypography.errorText) }
        }

        // Primeiro o que impede a venda.
        items(offline) { alert ->
            SensorCard(
                icon = AppIconType.WifiOff,
                label = alert.companyName,
                value = alert.appliance ?: "geladeira",
                detail = alert.detail ?: "sem internet",
                accent = PixColors.Pink,
            )
        }

        item {
            PixelButton(
                text = if (onlyLow) "Mostrando o que está acabando" else "Só o que está acabando",
                onClick = onToggleOnlyLow,
                buttonSize = PixelButtonSize.Small,
                variant = if (onlyLow) PixelButtonVariant.Primary else PixelButtonVariant.Secondary,
            )
        }

        if (stock.isEmpty()) {
            item {
                Text(
                    text = if (onlyLow) "Nada para repor agora." else "Nenhuma porta com produto.",
                    style = PixTypography.bodyMuted,
                )
            }
        }

        items(stock) { row ->
            SensorCard(
                icon = AppIconType.Box,
                label = "${row.companyName} · ${row.appliance}",
                value = row.product,
                detail = "restam ${row.quantity}",
                accent = if (row.low) PixColors.Yellow else PixColors.Cyan,
            )
        }
    }
}

@Composable
private fun CompanyList(
    companies: List<OperatorCompany>,
    seesMoney: Boolean,
    error: String?,
    onOpen: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (error != null) {
            item { Text(text = error, style = PixTypography.errorText) }
        }

        if (companies.isEmpty()) {
            item { Text(text = "Você ainda não opera nenhuma empresa.", style = PixTypography.bodyMuted) }
        }

        items(companies) { company ->
            SensorCard(
                icon = if (company.offline > 0) AppIconType.WifiOff else AppIconType.Store,
                label = company.name,
                value = detailFor(company),
                detail = if (seesMoney && company.revenueToday != null) {
                    "hoje: R$ ${company.revenueToday}"
                } else {
                    null
                },
                accent = when {
                    company.offline > 0 -> PixColors.Pink
                    company.lowStock > 0 -> PixColors.Yellow
                    else -> PixColors.Green
                },
                onClick = { onOpen(company.id) },
            )
        }
    }
}

/** O estado da empresa em uma linha: o que precisa de alguém primeiro. */
private fun detailFor(company: OperatorCompany): String = when {
    company.offline > 0 -> "${company.offline} fora do ar"
    company.lowStock > 0 -> "${company.lowStock} acabando"
    else -> "${company.appliances} geladeira(s) de pé"
}
