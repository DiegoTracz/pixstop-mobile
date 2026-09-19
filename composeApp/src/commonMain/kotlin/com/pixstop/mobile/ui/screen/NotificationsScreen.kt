package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.AppNotification
import com.pixstop.mobile.domain.model.MascotFace
import com.pixstop.mobile.domain.notification.NotificationRouter
import com.pixstop.mobile.domain.notification.NotificationTarget
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelButtonSize
import com.pixstop.mobile.ui.components.PixelButtonVariant
import com.pixstop.mobile.ui.components.PixelEmptyState
import com.pixstop.mobile.ui.components.PixelLoader
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.NotificationsViewModel
import androidx.compose.foundation.lazy.rememberLazyListState
import org.koin.compose.viewmodel.koinViewModel

/**
 * Caixa de avisos.
 *
 * Tocar num aviso o marca como lido na hora e desfaz se o servidor recusar —
 * quem controla isso é o ViewModel. Quando o aviso aponta para uma tela, o
 * toque também leva até ela: um "pedido pago" que não abre o pedido faz a
 * pessoa procurar sozinha o que o aviso já sabia.
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenTarget: (NotificationTarget) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Carrega a próxima página ao chegar perto do fim, em vez de exigir um
    // botão que a pessoa teria de procurar.
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= state.items.lastIndex - 3
        }
    }

    LaunchedEffect(shouldLoadMore, state.hasNextPage) {
        if (shouldLoadMore && state.hasNextPage) {
            viewModel.loadMore()
        }
    }

    Column(modifier = modifier.fillMaxSize().background(PixColors.Dark)) {
        // Tela própria, alcançada pelo sino: leva a seta de voltar que o
        // Material pede em qualquer destino que não seja de primeiro nível.
        PixelScreenTopBar(
            title = if (state.unread > 0) "Avisos (${state.unread})" else "Avisos",
            onBack = onBack,
            action = {
                if (state.unread > 0) {
                    PixelButton(
                        text = "Marcar tudo",
                        onClick = viewModel::markAllRead,
                        variant = PixelButtonVariant.Secondary,
                        buttonSize = PixelButtonSize.Small,
                    )
                }
            },
        )

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {

        when {
            state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PixelLoader()
            }

            state.items.isEmpty() -> EmptyState(error = state.error, onRetry = viewModel::refresh)

            else -> LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.items, key = { it.id }) { notification ->
                    NotificationRow(
                        notification = notification,
                        // Marcar lido é inofensivo no que já está lido, e o
                        // alvo continua valendo — o pedido não desaparece
                        // porque o aviso dele foi visto ontem.
                        onClick = {
                            viewModel.markRead(notification.id)
                            NotificationRouter.resolve(notification)?.let(onOpenTarget)
                        },
                        target = NotificationRouter.resolve(notification),
                    )
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
}

@Composable
private fun NotificationRow(
    notification: AppNotification,
    target: NotificationTarget?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, if (notification.read) PixColors.Gray700 else PixColors.Cyan)
            .background(if (notification.read) PixColors.Dark else PixColors.CyanAlpha10)
            .clickable(enabled = !notification.read || target != null, onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Ponto de não lida: o que a pessoa procura ao abrir a tela.
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(8.dp)
                .background(if (notification.read) PixColors.Transparent else PixColors.Cyan),
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = notification.title,
                color = if (notification.read) PixColors.Gray300 else PixColors.Gray100,
                fontWeight = if (notification.read) FontWeight.Normal else FontWeight.Bold,
            )

            notification.body?.let {
                Text(text = it, style = PixTypography.bodyMuted)
            }
        }
    }
}

@Composable
private fun EmptyState(error: String?, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PixelEmptyState(
            message = error ?: "Nenhum aviso por aqui.",
            isError = error != null,
            face = MascotFace.Closed,
            action = if (error != null) {
                {
                    PixelButton(
                        text = "Tentar de novo",
                        onClick = onRetry,
                        variant = PixelButtonVariant.Secondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            } else {
                null
            },
        )
    }
}
