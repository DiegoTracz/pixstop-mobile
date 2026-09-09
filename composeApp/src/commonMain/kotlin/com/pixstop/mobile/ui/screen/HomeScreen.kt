package com.pixstop.mobile.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.LaunchedEffect
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
import com.pixstop.mobile.domain.access.Destination
import com.pixstop.mobile.domain.access.RoleHelper
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
import com.pixstop.mobile.core.storage.currentHourOfDay
import com.pixstop.mobile.core.text.greetingForHour
import kotlinx.coroutines.delay
import com.pixstop.mobile.ui.viewmodel.HomeFeedViewModel
import com.pixstop.mobile.ui.viewmodel.ShopViewModel
import com.pixstop.mobile.ui.viewmodel.CartViewModel
import com.pixstop.mobile.ui.viewmodel.NotificationsViewModel
import com.pixstop.mobile.ui.viewmodel.SessionUiState
import com.pixstop.mobile.ui.viewmodel.SessionViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * Como cada destino se apresenta na barra inferior.
 *
 * Quais destinos aparecem é decisão da matriz de acesso, não desta lista: uma
 * aba escrita à mão sobreviveria a um plano que desligou a funcionalidade dela
 * e levaria a uma tela em branco.
 */
private fun bottomNavItems(destinations: List<Destination>) = destinations.map { destination ->
    when (destination) {
        // Vitrine, e não lupa: a aba diz "Loja", e uma lupa prometeria
        // busca. É o mesmo ícone da barra da web.
        Destination.Shop -> BottomNavItem(route = destination.name, label = "Loja", icon = AppIconType.Store)
        Destination.Profile -> BottomNavItem(route = destination.name, label = "Perfil", icon = AppIconType.Person)
        else -> BottomNavItem(route = destination.name, label = "Início", icon = AppIconType.Home)
    }
}

/** Rótulo e ícone de cada item do menu lateral. */
private fun drawerLabel(destination: Destination): Pair<String, AppIconType> = when (destination) {
    Destination.Orders -> "Meus pedidos" to AppIconType.Cart
    Destination.Pixels -> "Meus pixels" to AppIconType.Check
    Destination.Progress -> "Meu progresso" to AppIconType.Home
    Destination.Team -> "Meu time" to AppIconType.Person
    Destination.Rewards -> "Recompensas" to AppIconType.Check
    Destination.Staff -> "Registrar visita" to AppIconType.Store
    Destination.Company -> "Empresa" to AppIconType.Settings
    Destination.Fridges -> "Geladeiras" to AppIconType.Lock
    else -> destination.name to AppIconType.Info
}

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
    onDestination: (Destination) -> Unit = {},
    onOpenCards: () -> Unit = {},
    onOpenOrder: (Long) -> Unit = {},
    sessionViewModel: SessionViewModel = koinViewModel(),
    notificationsViewModel: NotificationsViewModel = koinViewModel(),
    cartViewModel: CartViewModel = koinViewModel(),
    // A vitrine é a mesma da aba Loja: tocar numa categoria no início precisa
    // abrir a loja já filtrada, e não uma segunda cópia dela.
    shopViewModel: ShopViewModel = koinViewModel(),
    homeFeedViewModel: HomeFeedViewModel = koinViewModel(),
) {
    val session by sessionViewModel.uiState.collectAsState()
    val notifications by notificationsViewModel.uiState.collectAsState()
    val cart by cartViewModel.uiState.collectAsState()
    val feed by homeFeedViewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    // Uma segunda gaveta, do mesmo lado, aberta só pelo ícone do carrinho: o
    // gesto de arrastar continua sendo do menu, que é o de fora.
    val cartDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val openCartDrawer: () -> Unit = { scope.launch { cartDrawerState.open() } }
    var switcherOpen by remember { mutableStateOf(false) }
    val destinations = RoleHelper.bottomBar(session.company)
    var selectedTab by remember { mutableStateOf(Destination.Home.name) }

    // "Adicionado ao carrinho" some sozinho, para não virar mais uma coisa
    // para a pessoa fechar. Mas o aviso tem um botão dentro — e um botão que
    // desaparece em dois segundos e meio é pior do que não existir: a pessoa
    // lê, estica o dedo, e o toque cai na barra de navegação que estava
    // embaixo. O prazo é o tempo de ler e alcançar.
    LaunchedEffect(cart.message) {
        if (cart.message != null) {
            delay(CART_NOTICE_MILLIS)
            cartViewModel.dismissMessage()
        }
    }

    // Um plano que desliga a loja tira a aba dela; quem estava nela precisa
    // sair, senão ficaria olhando uma tela que já não existe.
    LaunchedEffect(destinations) {
        if (destinations.none { it.name == selectedTab }) {
            selectedTab = destinations.firstOrNull()?.name ?: Destination.Home.name
        }
    }

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
                    destinations = RoleHelper.drawer(session.company),
                    onDestination = { destination ->
                        scope.launch { drawerState.close() }
                        onDestination(destination)
                    },
                    onSettings = {
                        scope.launch { drawerState.close() }
                        selectedTab = Destination.Profile.name
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
        ModalNavigationDrawer(
            drawerState = cartDrawerState,
            // Só o ícone do carrinho abre: o arrastar da borda é do menu.
            gesturesEnabled = false,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.width(320.dp), drawerContainerColor = PixColors.Dark) {
                    CartDrawerSheet(
                        viewModel = cartViewModel,
                        onKeepShopping = { scope.launch { cartDrawerState.close() } },
                        onCheckout = {
                            scope.launch { cartDrawerState.close() }
                            onOpenCart()
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
                    // Sem a loja no plano não há carrinho a mostrar.
                    onCartClick = openCartDrawer.takeIf { RoleHelper.canOpen(Destination.Cart, session.company) },
                    cartCount = cart.itemCount,
                )
            },
            bottomBar = {
                Column {
                    cart.message?.let { text ->
                        CartNotice(text = text, onOpenCart = openCartDrawer)
                    }

                    PixelBottomNav(
                        items = bottomNavItems(destinations),
                        selectedRoute = selectedTab,
                        onItemSelected = { selectedTab = it },
                    )
                }
            },
        ) { paddingValues ->
            when (selectedTab) {
                Destination.Shop.name -> ShopScreen(
                    viewModel = shopViewModel,
                    onProductClick = onOpenProduct,
                    onAddToCart = { cartViewModel.add(it) },
                    modifier = Modifier.padding(paddingValues),
                )

                Destination.Profile.name -> ProfileScreen(
                    user = session.account?.user,
                    // Conta excluída: o token já não vale, e ficar na Home
                    // levaria a um 401 na próxima tela.
                    onAccountDeleted = {
                        sessionViewModel.logout()
                        onLogout()
                    },
                    // O nome aparece em outras telas; sem recarregar a conta
                    // elas continuariam mostrando o antigo.
                    onProfileSaved = sessionViewModel::refresh,
                    onOpenCards = onOpenCards,
                    modifier = Modifier.padding(paddingValues),
                )

                else -> HomeFeed(
                    state = feed,
                    user = session.account?.user,
                    company = session.company,
                    greeting = greetingForHour(currentHourOfDay()),
                    onOpenOrder = onOpenOrder,
                    onOpenProduct = onOpenProduct,
                    onOpenCategory = { categoryId ->
                        shopViewModel.onCategorySelected(categoryId)
                        selectedTab = Destination.Shop.name
                    },
                    onOpenShop = { selectedTab = Destination.Shop.name },
                    onOpenPixels = { onDestination(Destination.Pixels) },
                    onOpenProgress = { onDestination(Destination.Progress) },
                    onAddToCart = { cartViewModel.add(it) },
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
        }
    }
}

/**
 * "Adicionado ao carrinho", com o caminho para o carrinho ao lado.
 *
 * Antes só o número do ícone mudava: quem tocou no botão não tinha como
 * saber se o toque pegou sem procurar o contador no canto da tela.
 */
/** Quanto o aviso do carrinho fica no ar. Tem um botão dentro: precisa dar tempo de tocar. */
private const val CART_NOTICE_MILLIS = 6_000L

@Composable
private fun CartNotice(text: String, onOpenCart: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PixColors.Green)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Fonte de texto, não a de rótulo: a pixelada de 6sp em cima do
        // verde vivo é bonita e ilegível, e este aviso existe para ser lido
        // de relance.
        Text(text = text, style = PixTypography.bodyRegular, color = PixColors.Dark)

        Text(
            text = "Ver carrinho",
            style = PixTypography.bodyRegular,
            color = PixColors.Dark,
            modifier = Modifier
                .border(2.dp, PixColors.Dark)
                .clickable(onClick = onOpenCart)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
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
    destinations: List<Destination>,
    onDestination: (Destination) -> Unit,
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

        destinations.forEach { destination ->
            val (label, icon) = drawerLabel(destination)

            DrawerAction(icon = icon, label = label, onClick = { onDestination(destination) })
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
