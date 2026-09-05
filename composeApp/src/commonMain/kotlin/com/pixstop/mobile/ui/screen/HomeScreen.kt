package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.AccountUser
import com.pixstop.mobile.domain.model.ActiveCompany
import com.pixstop.mobile.ui.components.AppIcon
import com.pixstop.mobile.ui.components.AppIconType
import com.pixstop.mobile.ui.components.BottomNavItem
import com.pixstop.mobile.ui.components.CompanySwitcherSheet
import com.pixstop.mobile.ui.components.PixelBottomNav
import com.pixstop.mobile.ui.components.PixelTopBar
import com.pixstop.mobile.ui.components.PlayerHud
import com.pixstop.mobile.ui.components.PlayerMenuHeader
import com.pixstop.mobile.ui.theme.AppBranding
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.CartViewModel
import com.pixstop.mobile.ui.viewmodel.NotificationsViewModel
import com.pixstop.mobile.ui.viewmodel.SessionUiState
import com.pixstop.mobile.ui.viewmodel.SessionViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * Destinos da barra inferior.
 *
 * Só lugares para onde se vai, e todos existem para todo papel. Avisos saiu
 * daqui para o sino da barra superior: é uma caixa de entrada, não um destino.
 */
private val bottomNavItems = listOf(
    BottomNavItem(route = "home", label = "Início", icon = AppIconType.Home),
    BottomNavItem(route = "search", label = "Loja", icon = AppIconType.Search),
    BottomNavItem(route = "profile", label = "Perfil", icon = AppIconType.Person),
)

/**
 * Tela principal, com menu lateral, barra superior e barra inferior.
 *
 * Todo o estado da conta vem do `SessionViewModel` — o mesmo que a navegação
 * consulta. Guardar uma segunda cópia aqui já fez a Home mostrar o nome antigo
 * depois de alterá-lo no perfil.
 */
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onJoinCompany: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenCart: () -> Unit = {},
    onOpenProduct: (Long) -> Unit = {},
    onOpenOrders: () -> Unit = {},
    onOpenPixels: () -> Unit = {},
    onOpenTeam: () -> Unit = {},
    onOpenCompany: () -> Unit = {},
    sessionViewModel: SessionViewModel = koinViewModel(),
    notificationsViewModel: NotificationsViewModel = koinViewModel(),
    cartViewModel: CartViewModel = koinViewModel(),
) {
    val session by sessionViewModel.uiState.collectAsState()
    val notifications by notificationsViewModel.uiState.collectAsState()
    val cart by cartViewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var switcherOpen by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("home") }

    if (switcherOpen) {
        CompanySwitcherSheet(
            memberships = session.account?.memberships.orEmpty(),
            isSwitching = session.isSwitching,
            onSelect = { membership ->
                sessionViewModel.switchCompany(membership.id)
                switcherOpen = false
            },
            onJoinCompany = {
                switcherOpen = false
                onJoinCompany()
            },
            onDismiss = { switcherOpen = false },
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                DrawerContent(
                    user = session.account?.user,
                    company = session.company,
                    canSwitchCompany = session.account?.canSwitchCompany == true,
                    // A área do gestor só existe para quem gere um time; o
                    // `/me` é quem diz isso.
                    isManager = session.company?.isManager == true,
                    onPixels = {
                        scope.launch { drawerState.close() }
                        onOpenPixels()
                    },
                    onTeam = {
                        scope.launch { drawerState.close() }
                        onOpenTeam()
                    },
                    // O painel da empresa é do admin; o servidor recusa os
                    // demais, e esconder aqui evita oferecer o que não abre.
                    isCompanyAdmin = session.company?.role?.isAdmin == true,
                    onCompany = {
                        scope.launch { drawerState.close() }
                        onOpenCompany()
                    },
                    onOrders = {
                        scope.launch { drawerState.close() }
                        onOpenOrders()
                    },
                    onSettings = {
                        scope.launch { drawerState.close() }
                        selectedTab = "profile"
                    },
                    onSwitchCompany = {
                        scope.launch { drawerState.close() }
                        switcherOpen = true
                    },
                    onJoinCompany = {
                        scope.launch { drawerState.close() }
                        onJoinCompany()
                    },
                    onLogout = {
                        scope.launch {
                            drawerState.close()
                            sessionViewModel.logout()
                            onLogout()
                        }
                    },
                )
            }
        },
    ) {
        Scaffold(
            containerColor = PixColors.Dark,
            topBar = {
                PixelTopBar(
                    title = session.company?.name ?: AppBranding.APP_NAME,
                    canSwitch = session.account?.canSwitchCompany == true,
                    onTitleClick = { switcherOpen = true },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNotificationsClick = onOpenNotifications,
                    unreadCount = notifications.unread,
                    onCartClick = onOpenCart,
                    cartCount = cart.itemCount,
                )
            },
            bottomBar = {
                PixelBottomNav(
                    items = bottomNavItems,
                    selectedRoute = selectedTab,
                    onItemSelected = { selectedTab = it },
                )
            },
        ) { paddingValues ->
            when (selectedTab) {
                "search" -> ShopScreen(
                    onProductClick = onOpenProduct,
                    modifier = Modifier.padding(paddingValues),
                )

                "profile" -> ProfileScreen(
                    user = session.account?.user,
                    // O nome aparece em outras telas; sem recarregar a conta
                    // elas continuariam mostrando o antigo.
                    onProfileSaved = sessionViewModel::refresh,
                    modifier = Modifier.padding(paddingValues),
                )

                else -> HomeContent(
                    session = session,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

/**
 * Menu lateral, no formato do PLAYER MENU da web.
 *
 * Traz o mesmo painel — retrato, nome, empresa e saldo — e as mesmas ações,
 * para quem usa os dois reconhecer o lugar sem reaprender.
 */
@Composable
private fun DrawerContent(
    user: AccountUser?,
    company: ActiveCompany?,
    canSwitchCompany: Boolean,
    isManager: Boolean,
    isCompanyAdmin: Boolean,
    onCompany: () -> Unit,
    onPixels: () -> Unit,
    onTeam: () -> Unit,
    onOrders: () -> Unit,
    onSettings: () -> Unit,
    onSwitchCompany: () -> Unit,
    onJoinCompany: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxHeight().background(PixColors.Gray900)) {
        Column(modifier = Modifier.statusBarsPadding()) {
            PlayerMenuHeader(title = "PLAYER MENU")

            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(PixColors.Cyan))
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (user != null) {
                PlayerHud(user = user, company = company, showEmail = true)
            } else {
                Text(text = "Carregando…", style = PixTypography.bodyMuted)
            }
        }

        HorizontalDivider(color = PixColors.Gray700, thickness = 2.dp)

        if (canSwitchCompany) {
            DrawerAction(
                icon = AppIconType.ChevronDown,
                label = "Trocar de empresa",
                onClick = onSwitchCompany,
            )
        }

        DrawerAction(icon = AppIconType.Cart, label = "Meus pedidos", onClick = onOrders)

        DrawerAction(icon = AppIconType.Check, label = "Meus pixels", onClick = onPixels)

        if (isManager) {
            DrawerAction(icon = AppIconType.Person, label = "Meu time", onClick = onTeam)
        }

        if (isCompanyAdmin) {
            DrawerAction(icon = AppIconType.Settings, label = "Empresa", onClick = onCompany)
        }

        DrawerAction(icon = AppIconType.PersonAdd, label = "Entrar em outra empresa", onClick = onJoinCompany)

        DrawerAction(icon = AppIconType.Settings, label = "Configurações", onClick = onSettings)

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider(color = PixColors.Gray700, thickness = 2.dp)

        DrawerAction(
            icon = AppIconType.Logout,
            label = "Sair do Jogo",
            color = PixColors.Pink,
            onClick = onLogout,
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DrawerAction(
    icon: AppIconType,
    label: String,
    onClick: () -> Unit,
    color: Color = PixColors.Gray100,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            // 48dp de alvo de toque, como o Material pede.
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(icon = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = color)

        Text(text = label, color = color)
    }
}

/**
 * Início.
 *
 * Ainda é uma saudação: a vitrine, o saldo e os pixels chegam na etapa da loja.
 */
@Composable
private fun HomeContent(
    session: SessionUiState,
    modifier: Modifier = Modifier,
) {
    val user = session.account?.user

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when {
            user == null && session.isLoading -> CircularProgressIndicator(color = PixColors.Cyan)

            user != null -> {
                Box(
                    modifier = Modifier.size(96.dp).background(PixColors.Cyan),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = user.name.take(2).uppercase(),
                        style = PixTypography.pageTitle,
                        color = PixColors.Dark,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Olá, ${user.firstName}!",
                    style = PixTypography.pageTitle,
                    color = PixColors.Cyan,
                    textAlign = TextAlign.Center,
                )

                session.company?.let { company ->
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${company.pixelAvailable} pixels disponíveis",
                        style = PixTypography.bodySecondary,
                    )
                }
            }

            else -> Text(
                text = session.error ?: "Não foi possível carregar a sua conta.",
                style = PixTypography.errorText,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Espaço reservado para as telas que ainda não existem.
 */
@Composable
private fun PlaceholderContent(
    title: String,
    icon: AppIconType,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(72.dp).background(PixColors.Gray800),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                icon = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = PixColors.Cyan,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = title, style = PixTypography.sectionTitle, color = PixColors.Cyan)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Em construção.",
            style = PixTypography.bodyMuted,
            textAlign = TextAlign.Center,
        )
    }
}
