package com.pixstop.mobile.data.repository

import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.PickupStatus
import com.pixstop.mobile.support.FakeApi
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * "Abrir a geladeira" e o bilhete de Bluetooth (Fases 9.7 e 9.8).
 *
 * O que importa aqui é o caminho e o que volta: a recusa do servidor traz o
 * estado junto, e é por isso que ela não pode ser engolida — a tela precisa
 * da mensagem *e* do bloco.
 */
class OrderUnlockRepositoryTest {

    private val unlocked = """
        {"success":true,"message":"Abrindo!","data":{"pickup":{"status":"unlocking","can_unlock":true,
         "attempts":1,"max_attempts":3,"window_until":"2026-09-09T18:10:00+00:00","has_ticket":false}}}
    """.trimIndent()

    @Test
    fun `abrir vai no caminho do pedido e devolve a retirada`() = runTest {
        val api = FakeApi()

        val result = OrderRepository(api.clientReturning(unlocked)).unlock(9)

        assertContains(api.lastRequest?.url.toString(), "orders/9/unlock")
        assertIs<Outcome.Success<*>>(result)
        assertEquals(PickupStatus.Unlocking, (result.value as com.pixstop.mobile.domain.model.UnlockAttempt).pickup.status)
    }

    @Test
    fun `a recusa vira falha com a mensagem que o servidor escreveu`() = runTest {
        val api = FakeApi(HttpStatusCode.UnprocessableEntity)

        val result = OrderRepository(
            api.clientReturning(
                """{"success":false,"error":{"code":"unlock_busy","message":"Alguém está usando a geladeira agora."}}""",
            ),
        ).unlock(9)

        assertIs<Outcome.Failure>(result)
        assertContains(result.error.message, "usando a geladeira")
    }

    @Test
    fun `pedir outro bilhete devolve o bilhete ja lido`() = runTest {
        val api = FakeApi()
        val ticket = "eyJraWQiOiJrIiwiZGV2IjoiTkhNQkUwQ1hKNSIsIm9yZGVyIjo5LCJjbWQiOjMsImV4cCI6IjIwMjYtMDktMDlUMTg6MTA6MDArMDA6MDAifQ.c2ln"

        val result = OrderRepository(
            api.clientReturning("""{"success":true,"data":{"unlock_ticket":"$ticket","unlock_ticket_expires_at":"2026-09-09T18:10:00+00:00"}}"""),
        ).reissueTicket(9)

        assertContains(api.lastRequest?.url.toString(), "orders/9/unlock-ticket")
        assertIs<Outcome.Success<*>>(result)
        assertEquals("NHMBE0CXJ5", (result.value as com.pixstop.mobile.domain.model.UnlockTicket?)?.deviceIdentifier)
    }

    @Test
    fun `a consulta de status traz a retirada para a tela acompanhar`() = runTest {
        val api = FakeApi()

        val result = OrderRepository(
            api.clientReturning(
                """{"success":true,"data":{"status":"paid","status_label":"Pago",
                   "pickup":{"status":"opened","can_unlock":false,"door_opened_at":"2026-09-09T18:05:00+00:00"}}}""",
            ),
        ).pickupStatus(9)

        assertIs<Outcome.Success<*>>(result)
        val attempt = result.value as com.pixstop.mobile.domain.model.UnlockAttempt
        assertEquals(PickupStatus.Opened, attempt.pickup.status)
        assertNotNull(attempt.pickup.doorOpenedAt)
    }
}
