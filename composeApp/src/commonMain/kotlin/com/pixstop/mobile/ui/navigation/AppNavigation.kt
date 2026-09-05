package com.pixstop.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pixstop.mobile.data.repository.AuthRepository
import com.pixstop.mobile.ui.screen.HomeScreen
import com.pixstop.mobile.ui.screen.JoinCompanyScreen
import com.pixstop.mobile.ui.screen.LegalConsentScreen
import com.pixstop.mobile.ui.screen.LoginScreen
import com.pixstop.mobile.ui.screen.CartScreen
import com.pixstop.mobile.ui.screen.CheckoutScreen
import com.pixstop.mobile.ui.screen.OrderScreen
import com.pixstop.mobile.ui.screen.OrdersScreen
import com.pixstop.mobile.ui.screen.PixelHistoryScreen
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
                onOpenOrders = { navController.navigate(Routes.ORDERS) },
                onOpenPixels = { navController.navigate(Routes.PIXELS) },
                onOpenTeam = { navController.navigate(Routes.TEAM) },
                onOpenProduct = { navController.navigate(Routes.product(it)) },
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
            PixelHistoryScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.TEAM) {
            TeamScreen(onBack = { navController.popBackStack() })
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
            )
        }
    }
}
