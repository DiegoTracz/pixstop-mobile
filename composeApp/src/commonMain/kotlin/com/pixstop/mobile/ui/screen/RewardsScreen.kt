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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pixstop.mobile.data.remote.dto.RewardDto
import com.pixstop.mobile.data.remote.dto.VoucherDto
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelCoin
import com.pixstop.mobile.ui.components.PixelLoader
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.RewardsViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * "Recompensas" (Fase 14, V8): o que os pixels compram onde não há loja.
 *
 * O resgate tira os pixels na hora — por isso pede confirmação — e vira um
 * voucher com código; a pessoa mostra no balcão e o staff valida uma vez.
 */
@Composable
fun RewardsScreen(
    onBack: () -> Unit,
    viewModel: RewardsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    state.confirming?.let { reward ->
        ConfirmDialog(
            reward = reward,
            onConfirm = viewModel::confirmRedeem,
            onDismiss = viewModel::dismissRedeem,
        )
    }

    state.openVoucher?.let { voucher ->
        VoucherDialog(voucher = voucher, onDismiss = { viewModel.openVoucher(null) })
    }

    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark)) {
        PixelScreenTopBar(title = "Recompensas", onBack = onBack)

        when {
            state.isLoading && state.rewards.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PixelLoader()
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        PixelCoin(size = 14.dp)
                        Text(text = "${state.available} pixels disponíveis", style = PixTypography.bodySecondary, color = PixColors.Yellow)
                    }
                }

                state.error?.let { error ->
                    item { Text(text = error, style = PixTypography.errorText, color = PixColors.Pink) }
                }

                if (state.rewards.isEmpty()) {
                    item { Text(text = "A empresa ainda não montou o catálogo.", style = PixTypography.bodyMuted) }
                }

                items(state.rewards, key = { "reward-${it.id}" }) { reward ->
                    RewardRow(reward = reward, busy = state.redeemingId == reward.id, onClick = { viewModel.askRedeem(reward) })
                }

                if (state.vouchers.isNotEmpty()) {
                    item {
                        Text(text = "Meus vouchers", style = PixTypography.sectionTitle, color = PixColors.Cyan, modifier = Modifier.padding(top = 8.dp))
                    }

                    items(state.sortedVouchers, key = { "voucher-${it.id}" }) { voucher ->
                        VoucherRow(voucher = voucher, onClick = { viewModel.openVoucher(voucher) })
                    }
                }
            }
        }
    }
}

@Composable
private fun RewardRow(reward: RewardDto, busy: Boolean, onClick: () -> Unit) {
    val enabled = reward.affordable && reward.available && !busy
    val accent = if (enabled) PixColors.Cyan else PixColors.Gray700

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, accent)
            .background(PixColors.Darker)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = reward.name, color = if (reward.available) PixColors.Gray100 else PixColors.Gray500, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                PixelCoin(size = 12.dp)
                Text(text = reward.pixels.toString(), color = PixColors.Yellow)
            }
        }

        reward.description?.let {
            Text(text = it, style = PixTypography.caption, color = PixColors.Gray400)
        }

        Text(
            text = when {
                !reward.available -> "Indisponível"
                !reward.affordable -> "Faltam ${reward.missing} pixels"
                busy -> "Resgatando..."
                else -> "Toque para resgatar" + (reward.stock?.let { " · restam $it" } ?: "")
            },
            style = PixTypography.caption,
            color = if (enabled) PixColors.Cyan else PixColors.Gray500,
        )
    }
}

@Composable
private fun VoucherRow(voucher: VoucherDto, onClick: () -> Unit) {
    val color = if (voucher.isOpen) PixColors.Green else PixColors.Gray600

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, color)
            .background(PixColors.Darker)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = voucher.rewardName, color = if (voucher.isOpen) PixColors.Gray100 else PixColors.Gray500)
            Text(text = voucher.code.chunked(4).joinToString(" "), style = PixTypography.caption, color = PixColors.Gray400)
        }

        Text(text = voucher.statusLabel ?: voucher.status, style = PixTypography.badgeText, color = color)
    }
}

@Composable
private fun ConfirmDialog(reward: RewardDto, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().border(2.dp, PixColors.Cyan).background(PixColors.Darker).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Resgatar ${reward.name}?", style = PixTypography.sectionTitle, color = PixColors.Cyan)
            Text(
                text = "${reward.pixels} pixels saem da sua carteira agora. O voucher vale ${reward.validityDays} dias.",
                style = PixTypography.bodySecondary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelButton(text = "Voltar", onClick = onDismiss, variant = PixelButtonVariant.Secondary, modifier = Modifier.weight(1f))
                PixelButton(text = "Resgatar", onClick = onConfirm, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun VoucherDialog(voucher: VoucherDto, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().border(2.dp, PixColors.Green).background(PixColors.Darker).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = voucher.rewardName.uppercase(), style = PixTypography.badgeText, color = PixColors.Gray400)
            Text(text = voucher.code.chunked(4).joinToString(" "), style = PixTypography.pageTitle, color = PixColors.Green, textAlign = TextAlign.Center)
            Text(
                text = if (voucher.isOpen) "Mostre no balcão. O staff valida uma vez." else "Voucher ${voucher.statusLabel?.lowercase() ?: voucher.status}.",
                style = PixTypography.caption,
                color = PixColors.Gray400,
                textAlign = TextAlign.Center,
            )
            PixelButton(text = "Fechar", onClick = onDismiss, variant = PixelButtonVariant.Secondary, modifier = Modifier.fillMaxWidth())
        }
    }
}
