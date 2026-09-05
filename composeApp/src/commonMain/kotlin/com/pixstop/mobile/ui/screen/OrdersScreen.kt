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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.Order
import com.pixstop.mobile.domain.model.OrderStatus
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.components.formatMoney
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.OrdersViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Histórico de pedidos da empresa ativa.
 */
@Composable
fun OrdersScreen(
    onBack: () -> Unit,
    onOrderClick: (Long) -> Unit,
    viewModel: OrdersViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(PixColors.Dark)) {
        PixelScreenTopBar(title = "Meus pedidos", onBack = onBack)

        when {
            state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PixColors.Cyan)
            }

            state.orders.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = state.error ?: "Você ainda não fez nenhum pedido.",
                    style = if (state.error != null) PixTypography.errorText else PixTypography.bodyMuted,
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.orders, key = { it.id }) { order ->
                    OrderRow(order = order, onClick = { onOrderClick(order.id) })
                }
            }
        }
    }
}

@Composable
private fun OrderRow(order: Order, onClick: () -> Unit) {
    val statusColor = when (order.status) {
        OrderStatus.Paid, OrderStatus.Delivered -> PixColors.Green
        OrderStatus.Canceled -> PixColors.Pink
        else -> PixColors.Yellow
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, PixColors.Gray600)
            .background(PixColors.Darker)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = order.transactionId ?: "Pedido ${order.id}",
                style = PixTypography.caption,
                color = PixColors.Gray400,
            )

            Text(text = order.statusLabel, style = PixTypography.badgeText, color = statusColor)
        }

        Text(
            text = order.items.joinToString(", ") { "${it.quantity}× ${it.productName}" },
            style = PixTypography.bodySecondary,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )

        Text(text = formatMoney(order.productsTotal), style = PixTypography.sectionTitle, color = PixColors.Green)
    }
}
