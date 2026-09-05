package com.pixstop.mobile.data.repository

import com.pixstop.mobile.domain.model.AppNotification
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.data.remote.dto.Page
import com.pixstop.mobile.support.FakeApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * O badge da barra inferior sai do rodapé desta listagem: o servidor manda a
 * contagem de não lidas junto para não custar uma segunda chamada. Perder esse
 * campo apagaria o badge sem nada indicar que houve erro.
 */
class NotificationRepositoryTest {

    private val body = """
        {"success":true,"data":[
          {"id":"a1","type":"order_paid","title":"Pedido aprovado","body":"Pode retirar o produto.",
           "read":false,"read_at":null,"created_at":"2026-09-05T09:00:00+00:00"},
          {"id":"b2","type":"pixels_received","title":null,"body":"Você recebeu 50 pixels.",
           "read":true,"read_at":"2026-09-05T09:10:00+00:00","created_at":"2026-09-05T09:05:00+00:00"}
        ],
        "meta":{"current_page":1,"last_page":2,"per_page":20,"total":25,"unread":7}}
    """.trimIndent()

    @Test
    fun `listagem traz os avisos e a contagem de nao lidas`() = runTest {
        val result = NotificationRepository(FakeApi().clientReturning(body)).list()

        assertIs<Outcome.Success<Page<AppNotification>>>(result)
        assertEquals(2, result.value.items.size)
        assertEquals(7, result.value.meta.unread)
        assertTrue(result.value.meta.hasNextPage)
    }

    @Test
    fun `aviso sem titulo usa o corpo para nao ficar em branco`() = runTest {
        val result = NotificationRepository(FakeApi().clientReturning(body)).list()

        assertIs<Outcome.Success<Page<AppNotification>>>(result)

        val semTitulo = result.value.items.last()
        assertEquals("Você recebeu 50 pixels.", semTitulo.title)
        assertNull(semTitulo.body, "o corpo não se repete embaixo do título")
        assertTrue(semTitulo.read)
    }

    @Test
    fun `aviso com titulo mantem o corpo separado`() = runTest {
        val result = NotificationRepository(FakeApi().clientReturning(body)).list()

        assertIs<Outcome.Success<Page<AppNotification>>>(result)

        val primeiro = result.value.items.first()
        assertEquals("Pedido aprovado", primeiro.title)
        assertEquals("Pode retirar o produto.", primeiro.body)
        assertFalse(primeiro.read)
    }

    @Test
    fun `pagina pedida vai na consulta`() = runTest {
        val api = FakeApi()

        NotificationRepository(api.clientReturning(body)).list(page = 3, perPage = 50)

        val url = api.lastRequest?.url.toString()
        assertTrue(url.contains("page=3"), url)
        assertTrue(url.contains("per_page=50"), url)
    }
}
