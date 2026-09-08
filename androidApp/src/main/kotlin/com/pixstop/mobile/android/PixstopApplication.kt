package com.pixstop.mobile.android

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.util.DebugLogger
import com.pixstop.mobile.core.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

/**
 * Sobe o Koin uma vez, quando o processo nasce.
 *
 * O contexto do Android entra aqui porque o armazenamento seguro do token
 * precisa dele, e nenhuma tela deveria carregar esse detalhe.
 */
class PixstopApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidLogger()
            androidContext(this@PixstopApplication)
        }
    }

    /**
     * O carregador de imagem, montado uma vez com o buscador do Ktor.
     *
     * Sem registrar o buscador, o Coil não sabe falar HTTP e a foto de perfil
     * — que mora numa URL — simplesmente não aparece, sem erro nenhum na tela.
     * Fica aqui, e não dentro da composição, porque o Coil resolve o singleton
     * antes de a primeira tela existir.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .logger(DebugLogger())
            .build()
}
