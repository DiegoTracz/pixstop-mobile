package com.pixstop.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * Barra superior do app.
 *
 * Segue a *small top app bar* do Material 3 — 64dp de altura, ícone de menu à
 * esquerda, título e ações à direita, com alvo de toque de 48dp — e traz os
 * avisos para cá, que é onde as pessoas procuram o sino. A barra inferior fica
 * só com os lugares para onde se vai; um aviso não é um lugar.
 *
 * Os três quadradinhos coloridos à esquerda são a assinatura 8-bit que o resto
 * do sistema usa nos cantos dos cartões.
 */
@Composable
fun PixelTopBar(
    title: String,
    modifier: Modifier = Modifier,
    canSwitch: Boolean = false,
    onTitleClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    unreadCount: Int = 0,
    onCartClick: (() -> Unit)? = null,
    cartCount: Int = 0,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PixColors.Gray900)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTarget(icon = AppIconType.Menu, description = "Abrir menu", onClick = onMenuClick)

            Row(
                modifier = Modifier
                    .weight(1f)
                    .then(if (canSwitch) Modifier.clickable(onClick = onTitleClick) else Modifier)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PixelDots()

                Text(
                    text = title,
                    style = PixTypography.sectionTitle,
                    color = PixColors.Cyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                // Sem a seta o título não se anuncia como clicável.
                if (canSwitch) {
                    AppIcon(
                        icon = AppIconType.ChevronDown,
                        contentDescription = "Trocar de empresa",
                        modifier = Modifier.size(18.dp),
                        tint = PixColors.Cyan,
                    )
                }
            }

            IconTarget(
                icon = AppIconType.Notifications,
                description = if (unreadCount > 0) "Avisos, $unreadCount não lidos" else "Avisos",
                onClick = onNotificationsClick,
                badge = unreadCount.takeIf { it > 0 },
            )

            // O carrinho fica aqui, e não na barra inferior, porque a reserva
            // vence em minutos: precisa estar visível de qualquer tela.
            onCartClick?.let { click ->
                IconTarget(
                    icon = AppIconType.Cart,
                    description = if (cartCount > 0) "Carrinho, $cartCount itens" else "Carrinho",
                    onClick = click,
                    badge = cartCount.takeIf { it > 0 },
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(PixColors.Gray700),
        )
    }
}

/**
 * Ícone com o alvo de toque de 48dp que o Material exige, mesmo o desenho
 * tendo 24dp.
 */
@Composable
private fun IconTarget(
    icon: AppIconType,
    description: String,
    onClick: () -> Unit,
    badge: Int? = null,
) {
    Box(
        modifier = Modifier
            .size(TOUCH_TARGET)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(
            icon = icon,
            contentDescription = description,
            modifier = Modifier.size(ICON_SIZE),
            tint = PixColors.Cyan,
        )

        badge?.let { count ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 8.dp)
                    .background(PixColors.Pink)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            ) {
                Text(
                    text = if (count > 99) "99+" else count.toString(),
                    style = PixTypography.badgeText,
                    color = PixColors.White,
                )
            }
        }
    }
}

/** Os três quadradinhos que aparecem nos cantos dos cartões do sistema. */
@Composable
private fun PixelDots() {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(PixColors.Pink, PixColors.Yellow, PixColors.Green).forEach { color ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .drawBehind { drawRect(color, Offset.Zero, Size(size.width, size.height)) },
            )
        }
    }
}

private val BAR_HEIGHT = 64.dp
private val TOUCH_TARGET = 48.dp
private val ICON_SIZE = 24.dp

/**
 * Barra superior das telas de segundo nível.
 *
 * O Material pede a seta de voltar em todo destino que não é de primeiro
 * nível; a ação da direita fica a cargo de cada tela.
 */
@Composable
fun PixelScreenTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PixColors.Gray900)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                .padding(start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTarget(icon = AppIconType.ArrowBack, description = "Voltar", onClick = onBack)

            Text(
                text = title,
                style = PixTypography.sectionTitle,
                color = PixColors.Cyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )

            action()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(PixColors.Gray700),
        )
    }
}
