package com.pixstop.mobile.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pixstop.mobile.App
import com.pixstop.mobile.core.notification.NotificationEventBus
import com.pixstop.mobile.domain.notification.NotificationRouter
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val notifications: NotificationEventBus by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Arranque frio: o link que abriu o app chega antes de a navegação
        // existir. O barramento guarda o alvo até alguém poder atendê-lo.
        handleDeepLink(intent)

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
    }

    private fun handleDeepLink(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) {
            return
        }

        NotificationRouter.fromLink(intent.dataString)?.let(notifications::publish)
    }
}
