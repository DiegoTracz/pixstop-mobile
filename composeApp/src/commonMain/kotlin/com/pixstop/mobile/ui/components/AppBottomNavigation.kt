package com.pixstop.mobile.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Item de navegação inferior
 *
 * @param route Rota de navegação (deve corresponder ao Routes.*)
 * @param label Texto exibido abaixo do ícone
 * @param icon Ícone quando não selecionado
 * @param selectedIcon Ícone quando selecionado (opcional, usa icon se não definido)
 * @param badge Número para badge (notificações), null para esconder
 */
data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: AppIconType,
    val selectedIcon: AppIconType = icon,
    val badge: Int? = null
)

/**
 * Bottom Navigation multiplataforma
 *
 * Android: Material 3 NavigationBar com indicador pill
 * iOS: Tab Bar estilo nativo com tint color
 *
 * @param items Lista de itens de navegação (3-5 itens recomendado)
 * @param selectedRoute Rota atualmente selecionada
 * @param onItemSelected Callback quando um item é selecionado
 * @param modifier Modifier para customização
 */
@Composable
expect fun AppBottomNavigation(
    items: List<BottomNavItem>,
    selectedRoute: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
)
