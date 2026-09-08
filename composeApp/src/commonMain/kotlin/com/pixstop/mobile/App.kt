package com.pixstop.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.pixstop.mobile.core.storage.ThemeStore
import com.pixstop.mobile.ui.navigation.AppNavigation
import com.pixstop.mobile.ui.theme.AppTheme
import org.koin.compose.koinInject

/**
 * Ponto de entrada do aplicativo.
 *
 * Para configurar a URL da API do seu projeto Laravel, chame:
 * ```
 * ApiConfig.configure("https://seu-servidor.com/api")
 * ```
 * antes de chamar App().
 *
 * Exemplo no MainActivity.kt:
 * ```
 * setContent {
 *     ApiConfig.configure("https://api.meuapp.com/api")
 *     App()
 * }
 * ```
 *
 * Para personalizar as cores do app, edite:
 * - ui/theme/AppColors.kt
 */
@Composable
fun App() {
    val themeStore: ThemeStore = koinInject()
    val mode by themeStore.mode.collectAsState()

    AppTheme(mode = mode) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
        ) {
            AppNavigation()
        }
    }
}

