package com.pixstop.mobile.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.pixstop.mobile.App
import com.pixstop.mobile.core.notification.NotificationEventBus
import com.pixstop.mobile.domain.notification.NotificationRouter
import com.pixstop.mobile.domain.notification.NotificationTarget
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val notifications: NotificationEventBus by inject()

    // A resposta não muda nada aqui: negar só faz o aviso não aparecer.
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Arranque frio: o link que abriu o app chega antes de a navegação
        // existir. O barramento guarda o alvo até alguém poder atendê-lo.
        handleDeepLink(intent)
        handlePushTap(intent)
        askNotificationPermission()

        setContent {

            App()
        }
    }

    /**
     * O app já estava aberto e o sistema entregou outro link — `singleTask` no
     * manifesto faz o Android reaproveitar esta instância em vez de empilhar
     * outra.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
        handlePushTap(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) {
            return
        }

        NotificationRouter.fromLink(intent.dataString)?.let(notifications::publish)
    }

    /**
     * O toque numa notificação de push.
     *
     * Com o app fechado o sistema abre esta tela com o `data` do push nos
     * extras; com o app aberto quem os põe é o [PixelstopMessagingService].
     * Push que não aponta para lugar nenhum deixa a pessoa na caixa de avisos.
     * Os extras saem do intent depois de lidos para que girar a tela não
     * navegue de novo.
     */
    private fun handlePushTap(intent: Intent?) {
        val extras = intent?.extras ?: return
        val type = extras.getString(EXTRA_TYPE) ?: return

        val target = NotificationRouter.fromPayload(type, extras.getString(EXTRA_ID) ?: extras.getString(EXTRA_ORDER_ID))
            ?: NotificationRouter.fromLink(extras.getString(EXTRA_LINK))
            ?: NotificationTarget.Notifications

        notifications.publish(target)
        intent.removeExtra(EXTRA_TYPE)
    }

    /** O Android 13 passou a exigir a permissão; antes dele o push aparece sem pedir. */
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

        if (!granted) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private companion object {
        const val EXTRA_TYPE = "type"
        const val EXTRA_ID = "id"
        const val EXTRA_ORDER_ID = "order_id"
        const val EXTRA_LINK = "link"
    }
}
