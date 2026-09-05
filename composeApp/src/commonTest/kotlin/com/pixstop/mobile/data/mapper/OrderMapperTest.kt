package com.pixstop.mobile.data.mapper

import com.pixstop.mobile.core.network.apiJson
import com.pixstop.mobile.data.remote.dto.CheckoutDto
import com.pixstop.mobile.data.remote.dto.OrderDto
import com.pixstop.mobile.domain.model.OrderStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * O que decide a tela do pedido: o estado e a existência do PIX. Um status
 * desconhecido tem de cair em pendente, o único que não promete nada a quem
 * está esperando o produto.
 */
class OrderMapperTest {

    @Test
    fun `pedido pago com saldo e pixels nao tem pix`() {
        val json = """
            {"id":2,"transaction_id":"ORD-QIX3ATWMUV","status":"paid","status_label":"Pago",
             "payment_method":"balance","payment_method_label":"Saldo",
             "totals":{"products":3,"pixels":14,"balance":2.86,"money":0,"card_fee":0},
             "items":[{"id":2,"product_name":"Água Mineral 500ml","quantity":1,"unit_price":3,"subtotal":3}],
             "is_cancelable":false,"created_at":"2026-09-05T17:06:00+00:00"}
        """.trimIndent()

        val order = apiJson.decodeFromString<OrderDto>(json).toDomain()

        assertTrue(order.isPaid)
        assertFalse(order.isPending)
        assertNull(order.pix)
        assertEquals(14, order.pixels)
        assertEquals(2.86, order.balance)
    }

    @Test
    fun `pedido pendente com pix traz o copia e cola`() {
        val json = """
            {"id":1,"transaction_id":"ORD-BUSDR0OZ2K","status":"pending","status_label":"Pendente",
             "totals":{"products":3,"pixels":0,"balance":0,"money":3,"card_fee":0},
             "items":[],
             "pix":{"qr_code":"00020126580014br.gov.bcb.pix","qr_code_base64":"iVBORw0K","ticket_url":null,
                    "expires_at":"2026-09-05T18:00:00+00:00"},
             "is_cancelable":true}
        """.trimIndent()

        val order = apiJson.decodeFromString<OrderDto>(json).toDomain()

        assertTrue(order.isPending)
        assertNotNull(order.pix)
        assertEquals("00020126580014br.gov.bcb.pix", order.pix?.code)
        assertNotNull(order.pix?.expiresAt)
    }

    @Test
    fun `pix sem codigo e o mesmo que nao ter pix`() {
        val json = """
            {"id":3,"status":"pending","totals":{"products":3},"items":[],
             "pix":{"qr_code":"","qr_code_base64":null}}
        """.trimIndent()

        assertNull(apiJson.decodeFromString<OrderDto>(json).toDomain().pix)
    }

    @Test
    fun `status desconhecido conta como pendente`() {
        val json = """{"id":4,"status":"aguardando_conferencia","totals":{},"items":[]}"""

        assertEquals(OrderStatus.Pending, apiJson.decodeFromString<OrderDto>(json).toDomain().status)
    }

    @Test
    fun `cartao so e oferecido com gateway e chave publica`() {
        fun checkout(gateway: Boolean, key: String?) = apiJson.decodeFromString<CheckoutDto>(
            """{"items":[],"totals":{"products":6},"wallet":{"pixels":0,"balance":0},
                "checkout":{"gateway_available":$gateway,"mp_public_key":${key?.let { "\"$it\"" } ?: "null"}},
                "saved_cards":[]}""",
        ).toDomain()

        assertFalse(checkout(gateway = false, key = "APP_USR-x").cardTokenizationAvailable)
        assertFalse(checkout(gateway = true, key = "").cardTokenizationAvailable, "chave vazia não tokeniza")
        assertFalse(checkout(gateway = true, key = null).cardTokenizationAvailable)
        assertTrue(checkout(gateway = true, key = "APP_USR-x").cardTokenizationAvailable)
    }
}
