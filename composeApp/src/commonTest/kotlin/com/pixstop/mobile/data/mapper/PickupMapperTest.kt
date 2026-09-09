package com.pixstop.mobile.data.mapper

import com.pixstop.mobile.core.network.apiJson
import com.pixstop.mobile.data.remote.dto.OrderDto
import com.pixstop.mobile.domain.model.Pickup
import com.pixstop.mobile.domain.model.PickupReason
import com.pixstop.mobile.domain.model.PickupStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A retirada (Fase 9.8) chegando do servidor.
 *
 * Um servidor antigo, que não conhece a retirada, tem de continuar servindo
 * pedidos: nesse caso o bloco não vem, e o app mostra o que sempre mostrou.
 */
class PickupMapperTest {

    private fun orderWith(extra: String): OrderDto = apiJson.decodeFromString(
        """
        {"id":9,"transaction_id":"ORD-X","status":"paid","status_label":"Pago",
         "totals":{"products":8.0,"pixels":0,"balance":8.0,"money":0,"card_fee":0},
         "items":[],"is_cancelable":false$extra}
        """.trimIndent(),
    )

    @Test
    fun `pedido com a porta esperando o toque`() {
        val order = orderWith(
            ""","pickup":{"status":"awaiting","can_unlock":true,"reason":null,"attempts":1,"max_attempts":3,
                "window_until":null,"door_opened_at":null,"picked_up_at":null,"has_ticket":false}""",
        ).toDomain()

        assertEquals(PickupStatus.Awaiting, order.pickup.status)
        assertTrue(order.pickup.canUnlock)
        assertTrue(order.pickup.isFollowing)
        assertFalse(order.pickup.isDone)
        assertEquals(1, order.pickup.attempts)
        assertEquals(3, order.pickup.maxAttempts)
        assertNull(order.pickup.reason)
    }

    @Test
    fun `pedido abrindo agora traz o fim da janela`() {
        val order = orderWith(
            ""","pickup":{"status":"unlocking","can_unlock":true,"attempts":1,"max_attempts":3,
                "window_until":"2026-09-09T18:10:00+00:00","has_ticket":true}""",
        ).toDomain()

        assertTrue(order.pickup.isUnlocking)
        assertEquals(1788977400000L, order.pickup.windowUntil)
        assertTrue(order.pickup.hasTicket)
    }

    @Test
    fun `geladeira ocupada por outra compra nao deixa abrir`() {
        val order = orderWith(
            ""","pickup":{"status":"awaiting","can_unlock":false,"reason":"busy","attempts":0,"max_attempts":3}""",
        ).toDomain()

        assertEquals(PickupReason.Busy, order.pickup.reason)
        assertFalse(order.pickup.canUnlock)
    }

    @Test
    fun `porta que ja abriu e produto retirado sao desfecho`() {
        val opened = orderWith(""","pickup":{"status":"opened","door_opened_at":"2026-09-09T18:05:00+00:00"}""").toDomain()
        val taken = orderWith(""","pickup":{"status":"picked_up","picked_up_at":"2026-09-09T18:06:00+00:00"}""").toDomain()

        assertTrue(opened.pickup.isDone)
        assertFalse(opened.pickup.isFollowing)
        assertNotNull(opened.pickup.doorOpenedAt)
        assertTrue(taken.pickup.isDone)
        assertNotNull(taken.pickup.pickedUpAt)
    }

    @Test
    fun `servidor sem retirada e status desconhecido nao prometem porta nenhuma`() {
        assertEquals(Pickup.None, orderWith("").toDomain().pickup)
        assertEquals(PickupStatus.None, orderWith(""","pickup":{"status":"inventado"}""").toDomain().pickup.status)
    }

    @Test
    fun `o bilhete vem lido junto do pedido`() {
        val order = orderWith(""","unlock_ticket":"eyJraWQiOiJrIiwiZGV2IjoiTkhNQkUwQ1hKNSIsIm9yZGVyIjo5LCJjbWQiOjMsImV4cCI6IjIwMjYtMDktMDlUMTg6MTA6MDArMDA6MDAifQ.c2ln"""").toDomain()

        assertEquals("NHMBE0CXJ5", order.ticket?.deviceIdentifier)
        assertEquals(3, order.ticket?.commandId)
    }
}
