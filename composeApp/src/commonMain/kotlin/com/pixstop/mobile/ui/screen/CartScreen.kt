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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.CartLine
import com.pixstop.mobile.ui.components.AppIcon
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.components.formatMoney
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.CartViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Carrinho, com o contador da reserva.
 *
 * O prazo aparece porque ele é real: passado o tempo, o servidor devolve o
 * estoque para a vitrine mesmo que ninguém mexa na tela.
 */
@Composable
fun CartScreen(
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    viewModel: CartViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark)) {
        PixelScreenTopBar(title = "Carrinho", onBack = onBack)

        state.secondsLeft?.let { seconds ->
            ReservationBanner(seconds)
        }

        when {
            state.isLoading && state.cart.isEmpty -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = PixColors.Cyan)
            }

            state.cart.isEmpty -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = state.error ?: "Seu carrinho está vazio.",
                    style = if (state.error != null) PixTypography.errorText else PixTypography.bodyMuted,
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.cart.items, key = { it.id }) { line ->
                        CartRow(
                            line = line,
                            busy = state.busyItemId == line.id,
                            onChangeQuantity = { viewModel.changeQuantity(line.id, it) },
                            onRemove = { viewModel.remove(line.id) },
                        )
                    }
                }

                CartFooter(
                    total = state.cart.total,
                    itemCount = state.cart.totalItems,
                    error = state.error,
                    onCheckout = onCheckout,
                )
            }
        }
    }
}

/**
 * Aviso do prazo de reserva, em minutos e segundos.
 */
@Composable
private fun ReservationBanner(seconds: Long) {
    val expired = seconds <= 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (expired) PixColors.PinkAlpha20 else PixColors.CyanAlpha10)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(
            icon = if (expired) AppIconType.Warning else AppIconType.Info,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (expired) PixColors.Pink else PixColors.Cyan,
        )

        Text(
            text = if (expired) {
                "A reserva venceu. O estoque voltou para a vitrine."
            } else {
                "Reserva por mais ${formatCountdown(seconds)}"
            },
            style = PixTypography.caption,
            color = if (expired) PixColors.Pink else PixColors.Cyan,
        )
    }
}

@Composable
private fun CartRow(
    line: CartLine,
    busy: Boolean,
    onChangeQuantity: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, PixColors.Gray600)
            .background(PixColors.Darker)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = line.product.name,
                color = PixColors.Gray100,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = "${formatMoney(line.unitPrice)} cada",
                style = PixTypography.caption,
                color = PixColors.Gray400,
            )

            Text(
                text = formatMoney(line.subtotal),
                style = PixTypography.sectionTitle,
                color = PixColors.Green,
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CartStep(
                    icon = AppIconType.Remove,
                    description = "Menos um",
                    enabled = !busy,
                    onClick = { onChangeQuantity(line.quantity - 1) },
                )

                Text(
                    text = line.quantity.toString(),
                    style = PixTypography.badgeText,
                    color = PixColors.Gray100,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                CartStep(
                    icon = AppIconType.Add,
                    description = "Mais um",
                    // O servidor é quem decide de verdade; aqui só evita-se
                    // pedir mais do que o estoque conhecido.
                    enabled = !busy && line.quantity < line.product.available,
                    onClick = { onChangeQuantity(line.quantity + 1) },
                )
            }

            Text(
                text = "Remover",
                style = PixTypography.caption,
                color = PixColors.Pink,
                modifier = Modifier.clickable(enabled = !busy, onClick = onRemove).padding(8.dp),
            )
        }
    }
}

@Composable
private fun CartStep(
    icon: AppIconType,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .border(2.dp, if (enabled) PixColors.Cyan else PixColors.Gray700)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(
            icon = icon,
            contentDescription = description,
            modifier = Modifier.size(16.dp),
            tint = if (enabled) PixColors.Cyan else PixColors.Gray700,
        )
    }
}

@Composable
private fun CartFooter(
    total: Double,
    itemCount: Int,
    error: String?,
    onCheckout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PixColors.Darker)
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        error?.let {
            Text(text = it, style = PixTypography.errorText)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (itemCount == 1) "1 item" else "$itemCount itens",
                style = PixTypography.caption,
                color = PixColors.Gray400,
            )

            Text(text = formatMoney(total), style = PixTypography.pageTitle, color = PixColors.Green)
        }

        PixelButton(
            text = "Finalizar compra",
            onClick = onCheckout,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Minutos e segundos, com o zero à esquerda que o relógio pede.
 */
fun formatCountdown(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
