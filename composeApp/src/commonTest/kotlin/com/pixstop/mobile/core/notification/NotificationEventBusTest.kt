package com.pixstop.mobile.core.notification

import com.pixstop.mobile.domain.notification.NotificationTarget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationEventBusTest {

    @Test
    fun `o alvo publicado antes de alguem ouvir nao se perde`() = runTest {
        // É o arranque frio: o sistema abre o app por um link e o alvo chega
        // antes de a navegação existir.
        val bus = NotificationEventBus()
        bus.publish(NotificationTarget.Order(3))

        assertEquals(NotificationTarget.Order(3), bus.targets.first())
    }

    @Test
    fun `alvo atendido nao volta a abrir`() = runTest {
        val bus = NotificationEventBus()
        bus.publish(NotificationTarget.Pixels)

        assertEquals(NotificationTarget.Pixels, bus.targets.first())
        bus.consume()

        assertNull(bus.targets.replayCache.firstOrNull())
    }

    @Test
    fun `uma rajada de avisos chega em ordem a quem ja ouve`() = runTest {
        val bus = NotificationEventBus()
        val recebidos = mutableListOf<NotificationTarget>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            bus.targets.toList(recebidos)
        }

        bus.publish(NotificationTarget.Order(1))
        bus.publish(NotificationTarget.Order(2))
        bus.publish(NotificationTarget.Company)

        assertEquals(
            listOf(NotificationTarget.Order(1), NotificationTarget.Order(2), NotificationTarget.Company),
            recebidos,
        )
    }

    @Test
    fun `publicar nunca trava quem publicou`() = runTest {
        // Ninguém está ouvindo e o buffer é finito: mais pushes que espaço têm
        // de derrubar os antigos, não segurar a thread do sistema.
        val bus = NotificationEventBus()

        repeat(50) { bus.publish(NotificationTarget.Order(it.toLong())) }

        assertEquals(NotificationTarget.Order(49), bus.targets.first())
    }
}
