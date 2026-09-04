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
import com.pixstop.mobile.ui.screen.LoginScreen
import com.pixstop.mobile.ui.screen.RegisterScreen
import com.pixstop.mobile.ui.screen.SplashScreen
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
    val session by sessionViewModel.uiState.collectAsState()

    // Token recusado pelo servidor: volta ao login de onde quer que esteja.
    LaunchedEffect(session.loggedOut) {
        if (session.loggedOut) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Logado e sem empresa: não há o que fazer no app além de entrar em uma.
    LaunchedEffect(session.needsCompany) {
        if (session.needsCompany) {
            navController.navigate(Routes.JOIN_COMPANY) {
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
                onJoinCompany = { navController.navigate(Routes.JOIN_COMPANY) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
