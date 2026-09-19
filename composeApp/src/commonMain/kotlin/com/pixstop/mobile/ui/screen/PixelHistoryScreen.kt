package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.data.remote.dto.ReferralDto
import com.pixstop.mobile.domain.model.PixelEntry
import com.pixstop.mobile.ui.components.PixelCoin
import com.pixstop.mobile.ui.components.PixelEmptyState
import com.pixstop.mobile.ui.components.PixelLoader
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.PixelHistoryViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Carteira e extrato de pixels.
 *
 * O saldo vem do servidor, não da soma das linhas: o extrato é paginado, e
 * somar só a primeira página daria um número errado.
 */
@Composable
fun PixelHistoryScreen(
    onBack: () -> Unit,
    memberCode: String? = null,
    viewModel: PixelHistoryViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= state.entries.lastIndex - 3
        }
    }

    LaunchedEffect(shouldLoadMore, state.hasNextPage) {
        if (shouldLoadMore && state.hasNextPage) {
            viewModel.loadMore()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark)) {
        PixelScreenTopBar(title = "Meus pixels", onBack = onBack)

        WalletHeader(
            available = state.wallet.available,
            reserved = state.wallet.reserved,
            expiringSoon = state.wallet.expiringSoon,
        )

        memberCode?.let { MemberCodeCard(it) }

        state.referral?.let { ReferralCard(it) }

        when {
            state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PixelLoader()
            }

            state.entries.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PixelEmptyState(
                    message = state.error ?: "Nenhuma movimentação ainda.",
                    isError = state.error != null,
                )
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.entries, key = { it.id }) { entry ->
                    EntryRow(entry)
                }

                if (state.isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PixColors.Cyan)
                        }
                    }
                }
            }
        }
    }
}

/**
 * O que a pessoa tem, o que está preso no carrinho e o que está para vencer.
 */
@Composable
private fun WalletHeader(available: Int, reserved: Int, expiringSoon: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .border(2.dp, PixColors.Yellow)
            .background(PixColors.Darker)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PixelCoin(size = 24.dp)

            Text(text = available.toString(), style = PixTypography.pageTitle, color = PixColors.Yellow)

            Text(text = "disponíveis", style = PixTypography.bodySecondary)
        }

        if (reserved > 0) {
            Text(
                text = "$reserved reservados em pedidos abertos",
                style = PixTypography.caption,
                color = PixColors.Gray400,
            )
        }

        if (expiringSoon > 0) {
            Text(
                text = "$expiringSoon vencem em breve",
                style = PixTypography.caption,
                color = PixColors.Pink,
            )
        }
    }
}

@Composable
private fun EntryRow(entry: PixelEntry) {
    val color = if (entry.isCredit) PixColors.Green else PixColors.Pink

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, PixColors.Gray700)
            .background(PixColors.Darker)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = entry.sourceLabel, color = PixColors.Gray100)

            entry.description?.takeIf { it != entry.sourceLabel }?.let {
                Text(text = it, style = PixTypography.caption, color = PixColors.Gray400)
            }
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = if (entry.isCredit) "+${entry.amount}" else entry.amount.toString(),
                style = PixTypography.sectionTitle,
                color = color,
            )

            Text(
                text = "saldo ${entry.balanceAfter}",
                style = PixTypography.caption,
                color = PixColors.Gray500,
            )
        }
    }
}

/**
 * O código do balcão (Fase 14): a pessoa diz ou mostra, e o staff registra a
 * visita. Sem QR aqui — o app não desenha um; a web desenha.
 */
@Composable
private fun MemberCodeCard(code: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(2.dp, PixColors.Green)
            .background(PixColors.Darker)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = "SEU CÓDIGO NO BALCÃO", style = PixTypography.badgeText, color = PixColors.Gray400)
        Text(text = code.chunked(4).joinToString(" "), style = PixTypography.pageTitle, color = PixColors.Green)
        Text(text = "Diga o código para registrarem sua visita.", style = PixTypography.caption, color = PixColors.Gray400)
    }
}

/**
 * O link de indicação (Fase 14): o cliente traz o cliente, com o mesmo
 * código do balcão. Quem chega só rende quando compra pela primeira vez —
 * por isso o cartão conta "vieram", e não "convidei".
 */
@Composable
private fun ReferralCard(referral: ReferralDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .border(2.dp, PixColors.Purple)
            .background(PixColors.Darker)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = "INDIQUE E GANHE", style = PixTypography.badgeText, color = PixColors.Gray400)
        Text(text = "Quem entrar pelo seu link e comprar te rende ${referral.xp} XP.", color = PixColors.Gray100)
        Text(text = referral.url, style = PixTypography.caption, color = PixColors.Cyan)
        Text(
            text = "${referral.accepted} vieram" + if (referral.pending > 0) " · ${referral.pending} entraram e ainda não compraram" else "",
            style = PixTypography.caption,
            color = PixColors.Gray400,
        )
    }
}
