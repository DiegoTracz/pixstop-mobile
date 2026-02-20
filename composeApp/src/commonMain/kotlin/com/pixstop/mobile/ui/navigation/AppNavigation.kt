package com.pixstop.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pixstop.mobile.data.repository.AuthRepository
import com.pixstop.mobile.ui.screen.HomeScreen
import com.pixstop.mobile.ui.screen.LoginScreen
import com.pixstop.mobile.ui.screen.SplashScreen

/**
 * Componente principal de navegação do app
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authRepository = remember { AuthRepository() }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        // Splash Screen - tela inicial
        composable(Routes.SPLASH) {
            SplashScreen(
                onSplashFinished = {
                    // Navega para a tela apropriada baseado no estado de autenticação
                    val destination = if (authRepository.isAuthenticated()) {
                        Routes.HOME
                    } else {
                        Routes.LOGIN
                    }
                    navController.navigate(destination) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}