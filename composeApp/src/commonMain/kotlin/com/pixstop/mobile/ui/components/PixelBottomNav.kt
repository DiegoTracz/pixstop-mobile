package com.pixstop.mobile.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * Um destino da barra inferior.
 *
 * @param route rota do destino, igual à de `Routes`
 * @param label texto sob o ícone; o Material pede rótulo sempre visível
 * @param icon ícone quando não selecionado
 * @param selectedIcon ícone quando selecionado; usa o mesmo se não informado
 * @param badge contagem sobre o ícone, `null` para não mostrar
 */
data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: AppIconType,
    val selectedIcon: AppIconType = icon,
    val badge: Int? = null,
)

/**
 * Barra de navegação inferior.
 *
 * Segue as medidas do Material 3 — 80dp de altura, 3 a 5 destinos, ícone de
 * 24dp, rótulo sempre visível e alvo de toque de 48dp — mas com a assinatura
 * 8-bit do app: o indicador do item ativo é um retângulo de cantos retos, não
 * a pílula arredondada padrão, e a barra é fechada por uma linha sólida.
 *
 * É implementação comum às duas plataformas de propósito: a identidade do app
 * é a mesma no Android e no iOS, e um `expect/actual` faria cada uma divergir
 * ao primeiro ajuste.
 */
@Composable
fun PixelBottomNav(
    items: List<BottomNavItem>,
    selectedRoute: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(PixColors.Darker)
            // Só a linha de cima: as outras três encostariam nas bordas da tela.
            .drawBehind {
                drawRect(PixColors.Gray700, Offset.Zero, Size(size.width, 2.dp.toPx()))
            }
            .navigationBarsPadding()
            .height(BAR_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            PixelNavItem(
                item = item,
                selected = item.route == selectedRoute,
                onClick = { onItemSelected(item.route) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PixelNavItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    // O item ativo sobe um degrau — movimento em passo, sem suavizar, como
    // convém a um sprite.
    val lift by animateDpAsState(
        targetValue = if (selected) (-2).dp else 0.dp,
        animationSpec = tween(60),
    )

    Column(
        modifier = modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .offset(y = lift)
                .width(INDICATOR_WIDTH)
                .height(INDICATOR_HEIGHT)
                .background(if (selected) PixColors.CyanAlpha20 else PixColors.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            BadgedIcon(item = item, selected = selected)
        }

        Text(
            text = item.label,
            style = PixTypography.caption,
            color = if (selected) PixColors.Cyan else PixColors.Gray400,
        )
    }
}

/**
 * Ícone com a contagem de não lidas presa ao canto.
 *
 * O badge é um quadrado de cantos retos, não o círculo do Material: é a mesma
 * informação, na mesma posição, com a forma do resto do app.
 */
@Composable
private fun BadgedIcon(item: BottomNavItem, selected: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        AppIcon(
            icon = if (selected) item.selectedIcon else item.icon,
            contentDescription = item.label,
            modifier = Modifier.size(ICON_SIZE),
            tint = if (selected) PixColors.Cyan else PixColors.Gray400,
        )

        item.badge?.let { count ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-6).dp)
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

/** Altura da barra do Material 3. */
private val BAR_HEIGHT = 80.dp

/** Indicador do item ativo: as medidas do Material, com cantos retos. */
private val INDICATOR_WIDTH = 64.dp
private val INDICATOR_HEIGHT = 32.dp
private val ICON_SIZE = 24.dp
