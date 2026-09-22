package com.pixstop.mobile

import androidx.compose.ui.window.ComposeUIViewController
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.pixstop.mobile.core.di.initKoin

/**
 * O que o `Application` faz no Android, o `iOSApp` faz aqui.
 *
 * Chamado uma vez pelo `init()` do ponto de entrada em Swift, antes de a
 * primeira tela existir: sem o Koin de pé, a composição morre no primeiro
 * `koinInject()`; sem o buscador do Ktor, o Coil não sabe falar HTTP e a foto
 * de perfil — que mora numa URL — não aparece, sem erro nenhum na tela.
 */
fun startApp() {
    initKoin()

    SingletonImageLoader.setSafe { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }
}

fun MainViewController() = ComposeUIViewController { App() }
