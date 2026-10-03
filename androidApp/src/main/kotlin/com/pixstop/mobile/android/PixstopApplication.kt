package com.pixstop.mobile.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.util.DebugLogger
import com.google.firebase.messaging.FirebaseMessaging
import com.pixstop.mobile.core.di.initKoin
import com.pixstop.mobile.core.logging.AppLogger
import com.pixstop.mobile.core.notification.PushTokenRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.android.ext.android.get

/**
 * Sobe o Koin uma vez, quando o processo nasce.
 *
 * O contexto do Android entra aqui porque o armazenamento seguro do token
 * precisa dele, e nenhuma tela deveria carregar esse detalhe.
 */
class PixstopApplication : Application(), SingletonImageLoader.Factory {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidLogger()
            androidContext(this@PixstopApplication)
        }

        createNotificationChannels()
        fetchPushToken()
    }

    /**
     * Sem canal, o Android 8+ descarta a notificação em silêncio. O id é o
     * mesmo que o manifesto declara como padrão para o Firebase.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(CHANNEL_GENERAL, "Avisos", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Pedidos, pixels e recados da sua empresa"
        }

        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /**
     * O `onNewToken` só dispara quando o token nasce ou troca. Nas outras
     * aberturas é preciso pedir o token atual, senão o servidor nunca saberia
     * de um aparelho que já tinha token antes do login.
     */
    private fun fetchPushToken() {
        val pushTokens = get<PushTokenRegistry>()

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> appScope.launch { pushTokens.onNewToken(token) } }
            .addOnFailureListener { error -> AppLogger.w("Token de push indisponível: ${error.message}", tag = "Push") }
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

    companion object {
        const val CHANNEL_GENERAL = "pixelstop_avisos"
    }
}
