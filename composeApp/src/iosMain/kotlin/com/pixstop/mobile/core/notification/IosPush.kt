package com.pixstop.mobile.core.notification

import com.pixstop.mobile.domain.notification.NotificationRouter
import com.pixstop.mobile.domain.notification.NotificationTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

/**
 * A porta do push do iPhone para o Kotlin.
 *
 * O Firebase e o `UNUserNotificationCenter` só existem do lado do Swift
 * (`iOSApp.swift`); o que eles entregam — o token e o toque numa notificação —
 * entra aqui e segue pelo mesmo caminho do Android: o token vai ao
 * [PushTokenRegistry], o toque vira alvo no [NotificationEventBus].
 */
object IosPush {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** O Firebase entregou o token FCM, novo ou o de sempre. */
    fun onNewToken(token: String) {
        val registry = KoinPlatform.getKoin().get<PushTokenRegistry>()
        scope.launch { registry.onNewToken(token) }
    }

    /**
     * A pessoa tocou numa notificação. `userInfo` é o `data` do push, igual
     * aos extras que o Android lê; push que não aponta para lugar nenhum deixa
     * a pessoa na caixa de avisos.
     */
    fun onNotificationTapped(userInfo: Map<Any?, *>) {
        val type = userInfo["type"] as? String ?: return

        val target = NotificationRouter.fromPayload(type, (userInfo["id"] ?: userInfo["order_id"])?.toString())
            ?: NotificationRouter.fromLink(userInfo["link"] as? String)
            ?: NotificationTarget.Notifications

        KoinPlatform.getKoin().get<NotificationEventBus>().publish(target)
    }
}
