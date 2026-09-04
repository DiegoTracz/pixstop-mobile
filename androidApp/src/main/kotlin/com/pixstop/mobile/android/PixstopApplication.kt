package com.pixstop.mobile.android

import android.app.Application
import com.pixstop.mobile.core.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

/**
 * Sobe o Koin uma vez, quando o processo nasce.
 *
 * O contexto do Android entra aqui porque o armazenamento seguro do token
 * precisa dele, e nenhuma tela deveria carregar esse detalhe.
 */
class PixstopApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidLogger()
            androidContext(this@PixstopApplication)
        }
    }
}
