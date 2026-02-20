package com.pixstop.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.pixstop.mobile.ui.navigation.AppNavigation
import com.pixstop.mobile.ui.theme.AppTheme

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
    AppTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize()
        ) {
            AppNavigation()
        }
    }
}

