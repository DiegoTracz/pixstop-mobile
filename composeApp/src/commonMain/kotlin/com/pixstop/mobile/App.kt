package com.pixstop.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
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
    // O carregador de imagem precisa saber falar HTTP: sem registrar o
    // buscador do Ktor, a foto de perfil que mora numa URL simplesmente não
    // aparece, e sem erro nenhum na tela.
    SingletonImageLoader.setSafe { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }

    AppTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
        ) {
            AppNavigation()
        }
    }
}

