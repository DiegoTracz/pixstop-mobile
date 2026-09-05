package com.pixstop.mobile.core.notification

import com.pixstop.mobile.domain.notification.NotificationTarget
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Onde os toques em avisos viram navegação.
 *
 * Quem publica — o deep link do sistema, o push, a própria caixa de avisos —
 * não conhece a navegação, e a navegação não conhece nenhum deles.
 *
 * O `replay` de um existe por causa do arranque frio: quando o sistema abre o
 * app por um link, o alvo chega antes de a navegação existir para ouvi-lo. Sem
 * guardar o último evento, o toque se perderia e o app abriria na tela inicial
 * como se nada tivesse sido tocado. Quem consome avisa com [consume] que já
 * navegou, para que o mesmo alvo não volte a abrir na próxima recomposição.
 */
class NotificationEventBus {

    private val _targets = MutableSharedFlow<NotificationTarget>(
        replay = 1,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val targets: SharedFlow<NotificationTarget> = _targets.asSharedFlow()

    /**
     * Anuncia um alvo. Nunca suspende nem falha: uma rajada de pushes prefere
     * perder os antigos a segurar quem publicou.
     */
    fun publish(target: NotificationTarget) {
        _targets.tryEmit(target)
    }

    /** Marca o último alvo como já atendido. */
    fun consume() {
        _targets.resetReplayCache()
    }
}
