package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.model.AppNotification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * A marcação de lido acontece na tela antes de o servidor confirmar, senão a
 * lista pareceria travada a cada toque. Isso obriga a saber desfazer, e é a
 * transição de estado — pura — que decide se o desfazer é possível.
 */
class NotificationsUiStateTest {

    private fun aviso(id: String, read: Boolean) = AppNotification(
        id = id,
        type = null,
        title = "Aviso $id",
        body = null,
        actionUrl = null,
        read = read,
        createdAt = null,
    )

    private val estado = NotificationsUiState(
        items = listOf(aviso("a1", read = false), aviso("b2", read = false), aviso("c3", read = true)),
        unread = 2,
    )

    @Test
    fun `marcar um aviso desconta um da contagem`() {
        val depois = estado.withRead("a1")

        assertEquals(1, depois.unread)
        assertTrue(depois.items.first { it.id == "a1" }.read)
        assertFalse(depois.items.first { it.id == "b2" }.read)
    }

    @Test
    fun `marcar um aviso ja lido nao mexe em nada`() {
        // Devolver o mesmo objeto é o que faz o ViewModel nem chamar o servidor.
        assertSame(estado, estado.withRead("c3"))
        assertSame(estado, estado.withRead("inexistente"))
    }

    @Test
    fun `marcar tudo zera a contagem`() {
        val depois = estado.withAllRead()

        assertEquals(0, depois.unread)
        assertTrue(depois.items.all { it.read })
    }

    @Test
    fun `marcar tudo sem nada por ler nao mexe em nada`() {
        val jaLido = estado.withAllRead()

        assertSame(jaLido, jaLido.withAllRead())
    }

    @Test
    fun `a contagem nunca fica negativa`() {
        val desalinhado = NotificationsUiState(items = listOf(aviso("a1", read = false)), unread = 0)

        assertEquals(0, desalinhado.withRead("a1").unread)
    }

    @Test
    fun `o estado anterior segue intacto para o desfazer`() {
        val depois = estado.withRead("a1")

        assertEquals(2, estado.unread, "a transição não pode alterar o estado de origem")
        assertFalse(estado.items.first { it.id == "a1" }.read)
        assertEquals(1, depois.unread)
    }
}
