package com.pixstop.mobile.android

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pixstop.mobile.core.notification.PushTokenRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Recebe o push do Firebase.
 *
 * Com o app em segundo plano, o próprio sistema desenha a notificação e este
 * serviço nem é chamado; o toque abre a [MainActivity] com o `data` nos extras.
 * Com o app aberto, o sistema não desenha nada e a notificação sai daqui —
 * não há banner dentro do app, então sem isso o aviso se perderia.
 */
class PixelstopMessagingService : FirebaseMessagingService() {

    private val pushTokens: PushTokenRegistry by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch { pushTokens.onNewToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"].orEmpty()

        // Os mesmos extras que o sistema poria no toque com o app fechado: a
        // MainActivity lê os dois caminhos do mesmo jeito.
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            message.data.forEach { (key, value) -> putExtra(key, value) }
        }

        // Mesmo aviso, mesmo id: um push repetido atualiza a notificação em vez de empilhar.
        val notificationId = (message.data["id"] ?: message.data["campaign_id"] ?: "$title|$body").hashCode()

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, PixstopApplication.CHANNEL_GENERAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        getSystemService(NotificationManager::class.java).notify(notificationId, notification)
    }
}
