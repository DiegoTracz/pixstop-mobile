package com.pixstop.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pixstop.mobile.core.notification.NotificationEventBus
import com.pixstop.mobile.data.repository.AppConfigRepository
import com.pixstop.mobile.domain.access.Destination
import com.pixstop.mobile.domain.access.RoleHelper
import com.pixstop.mobile.domain.notification.NotificationTarget
import com.pixstop.mobile.data.repository.AuthRepository
import com.pixstop.mobile.ui.screen.HomeScreen
import com.pixstop.mobile.ui.screen.JoinCompanyScreen
import com.pixstop.mobile.ui.screen.LegalConsentScreen
import com.pixstop.mobile.ui.screen.LoginScreen
import com.pixstop.mobile.ui.screen.CartScreen
import com.pixstop.mobile.ui.screen.CompanyScreen
import com.pixstop.mobile.ui.screen.CheckoutScreen
import com.pixstop.mobile.ui.screen.OrderScreen
import com.pixstop.mobile.ui.screen.OrdersScreen
import com.pixstop.mobile.ui.screen.PixelHistoryScreen
import com.pixstop.mobile.ui.screen.CardsScreen
import com.pixstop.mobile.ui.screen.PixelInviteScreen
import com.pixstop.mobile.ui.screen.StaffCheckinScreen
import com.pixstop.mobile.ui.screen.ProgressScreen
import com.pixstop.mobile.ui.screen.TeamScreen
import com.pixstop.mobile.ui.screen.NotificationsScreen
import com.pixstop.mobile.ui.screen.ProductDetailScreen
import com.pixstop.mobile.ui.screen.RegisterScreen
import com.pixstop.mobile.ui.screen.SplashScreen
import com.pixstop.mobile.ui.viewmodel.CartViewModel
import com.pixstop.mobile.ui.viewmodel.NotificationsViewModel
import com.pixstop.mobile.ui.viewmodel.SessionViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Navegação do app.
 *
 * O `SessionViewModel` vive aqui, acima das telas, porque a empresa ativa e a
 * pendência de consentimento decidem para onde a pessoa pode ir — e essas
 * decisões não podem estar espalhadas por cada tela.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authRepository: AuthRepository = koinInject()
    val sessionViewModel: SessionViewModel = koinViewModel()
    // Uma instância só: o badge da barra superior e a lista de avisos mostram
    // a mesma contagem, e marcar como lido tem de valer para os dois.
    val notificationsViewModel: NotificationsViewModel = koinViewModel()
    // O carrinho também é um só: o contador da barra superior e a tela do
    // carrinho têm de contar a mesma coisa.
    val cartViewModel: CartViewModel = koinViewModel()
    val session by sessionViewModel.uiState.collectAsState()
    val appConfig: AppConfigRepository = koinInject()
    val notificationEvents: NotificationEventBus = koinInject()

    /**
     * Leva a um alvo de aviso, se o papel de agora ainda o alcança.
     *
     * Um aviso não é autorização: quem deixou de administrar a empresa não
     * chega ao painel dela por um aviso de semana passada. A mesma matriz que
     * monta as barras decide aqui.
     */
    fun openTarget(target: NotificationTarget) {
        if (!RoleHelper.canOpen(target.destination, session.company)) {
            return
        }

        val route = when (target) {
            is NotificationTarget.Order -> Routes.order(target.id)
            NotificationTarget.Orders -> Routes.ORDERS
            NotificationTarget.Pixels -> Routes.PIXELS
            NotificationTarget.Progress -> Routes.PROGRESS
            is NotificationTarget.Invite -> Routes.invite(target.code)
            NotificationTarget.Company -> Routes.COMPANY
            NotificationTarget.Notifications -> Routes.NOTIFICATIONS
        }

        navController.navigate(route) {
            // Voltar de um destino aberto por aviso cai no início, não numa
            // pilha do que a pessoa estava fazendo antes de o link chegar.
            launchSingleTop = true
        }
    }

    // Revalida as regras do servidor no arranque. A rota é pública e o app já
    // tem padrões embutidos, então isto nunca segura a primeira tela.
    LaunchedEffect(Unit) {
        appConfig.refresh()
    }

    // O carrinho e os avisos são da empresa ativa. Trocar de empresa — ou
    // entrar na primeira, logo depois do login — muda os dois, então eles
    // recarregam junto em vez de cada tela lembrar de fazer isso.
    LaunchedEffect(session.company?.id) {
        if (session.company != null) {
            cartViewModel.refresh()
            notificationsViewModel.refresh()
        } else {
            cartViewModel.clear()
        }
    }

    // Um deep link ou um push pode chegar antes de a pessoa estar dentro: o
    // barramento guarda o alvo e ele só é atendido quando há sessão e empresa,
    // depois do login e do aceite dos documentos.
    val canNavigate = session.company != null &&
        !session.needsConsent && !session.needsCompany

    // O convite com pixels é a exceção: quem o abre pode não ter empresa
    // nenhuma ainda — basta estar logado e ter aceitado os documentos.
    val canOpenInvite = session.account != null && !session.needsConsent

    // A empresa entra na chave porque `openTarget` decide pelo papel de agora:
    // quem troca de empresa não pode continuar sendo julgado pela anterior.
    LaunchedEffect(canNavigate, canOpenInvite, session.company?.id) {
        if (!canNavigate && !canOpenInvite) {
            return@LaunchedEffect
        }

        notificationEvents.targets.collect { target ->
            when {
                target is NotificationTarget.Invite && canOpenInvite -> {
                    navController.navigate(Routes.invite(target.code)) { launchSingleTop = true }
                    notificationEvents.consume()
                }

                target !is NotificationTarget.Invite && canNavigate -> {
                    openTarget(target)
                    notificationEvents.consume()
                }

                // Fica guardado: o barramento reentrega quando a sessão mudar.
                else -> Unit
            }
        }
    }

    // Token recusado pelo servidor: volta ao login de onde quer que esteja.
    LaunchedEffect(session.loggedOut) {
        if (session.loggedOut) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // A ordem importa. Sem aceitar os documentos o servidor recusa toda rota de
    // negócio com `consent_required`, então o aceite vem antes de escolher
    // empresa — do contrário a pessoa entraria numa e só encontraria erro.
    LaunchedEffect(session.needsConsent, session.needsCompany) {
        when {
            session.needsConsent -> navController.navigate(Routes.LEGAL_CONSENT) {
                // Sem pilha atrás: a tela de aceite não tem como ser evitada.
                popUpTo(0) { inclusive = true }
            }

            session.needsCompany -> navController.navigate(Routes.JOIN_COMPANY) {
                popUpTo(Routes.HOME) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onSplashFinished = {
                    val destination = if (authRepository.isAuthenticated()) Routes.HOME else Routes.LOGIN
                    navController.navigate(destination) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    // O login troca o token; a conta precisa ser recarregada
                    // antes de qualquer tela depender da empresa ativa.
                    sessionViewModel.refresh()
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    sessionViewModel.refresh()
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() },
            )
        }

        composable(Routes.LEGAL_CONSENT) {
            LegalConsentScreen(
                onAccepted = {
                    // O `/me` precisa voltar sem pendência antes de seguir,
                    // senão o guarda acima mandaria a pessoa de volta.
                    sessionViewModel.refresh()
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onLogout = sessionViewModel::logout,
            )
        }

        composable(Routes.JOIN_COMPANY) {
            JoinCompanyScreen(
                sessionViewModel = sessionViewModel,
                onJoined = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.JOIN_COMPANY) { inclusive = true }
                    }
                },
                // Sem empresa nenhuma não há para onde voltar.
                onBack = if (session.account?.memberships?.any { it.isActive } == true) {
                    { navController.popBackStack() }
                } else {
                    null
                },
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                sessionViewModel = sessionViewModel,
                notificationsViewModel = notificationsViewModel,
                cartViewModel = cartViewModel,
                onJoinCompany = { navController.navigate(Routes.JOIN_COMPANY) },
                onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                onOpenCart = { navController.navigate(Routes.CART) },
                onOpenCards = { navController.navigate(Routes.CARDS) },
                // A tela não conhece rotas: ela diz para onde a pessoa quer
                // ir, e a navegação sabe onde isso fica.
                onDestination = { destination ->
                    when (destination) {
                        Destination.Orders -> navController.navigate(Routes.ORDERS)
                        Destination.Pixels -> navController.navigate(Routes.PIXELS)
                        Destination.Progress -> navController.navigate(Routes.PROGRESS)
                        Destination.Team -> navController.navigate(Routes.TEAM)
                        Destination.Staff -> navController.navigate(Routes.STAFF)
                        Destination.Company -> navController.navigate(Routes.COMPANY)
                        else -> Unit
                    }
                },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable("${Routes.PRODUCT}/{id}") { entry ->
            ProductDetailScreen(
                productId = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L,
                cartViewModel = cartViewModel,
                onBack = { navController.popBackStack() },
                onOpenCart = {
                    navController.navigate(Routes.CART) {
                        // Sair do produto para o carrinho: voltar dali leva de
                        // volta à vitrine, não ao produto que já foi resolvido.
                        popUpTo("${Routes.PRODUCT}/{id}") { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.CART) {
            CartScreen(
                viewModel = cartViewModel,
                onBack = { navController.popBackStack() },
                onCheckout = { navController.navigate(Routes.CHECKOUT) },
            )
        }

        composable(Routes.CHECKOUT) {
            CheckoutScreen(
                onBack = { navController.popBackStack() },
                onOrderPlaced = { orderId ->
                    // O pedido feito esvazia o carrinho no servidor; a tela
                    // precisa saber disso antes que alguém volte para ela.
                    cartViewModel.refresh()

                    navController.navigate(Routes.order(orderId)) {
                        // Fechado o pedido, voltar não pode cair no carrinho
                        // nem no fechamento: os dois já não existem mais.
                        popUpTo(Routes.HOME)
                    }
                },
            )
        }

        composable("${Routes.ORDER}/{id}") { entry ->
            OrderScreen(
                orderId = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.PIXELS) {
            PixelHistoryScreen(
                onBack = { navController.popBackStack() },
                // O código do balcão só existe onde há visita registrada.
                memberCode = session.account?.user?.memberCode?.takeIf { session.company?.hasModule("checkin") == true },
            )
        }

        composable(Routes.PROGRESS) {
            ProgressScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.CARDS) {
            CardsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.STAFF) {
            StaffCheckinScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.INVITE) { entry ->
            PixelInviteScreen(
                code = entry.arguments?.getString("code").orEmpty(),
                onAccepted = {
                    // A empresa nova já está ativa no servidor; o `/me` traz
                    // a carteira com o presente e a Home redesenha.
                    sessionViewModel.refresh()
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.TEAM) {
            TeamScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.COMPANY) {
            CompanyScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ORDERS) {
            OrdersScreen(
                onBack = { navController.popBackStack() },
                onOrderClick = { navController.navigate(Routes.order(it)) },
            )
        }

        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(
                viewModel = notificationsViewModel,
                onBack = { navController.popBackStack() },
                onOpenTarget = ::openTarget,
            )
        }
    }
}
