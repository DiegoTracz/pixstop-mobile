package com.pixstop.mobile.data.repository

import com.pixstop.mobile.data.remote.dto.Page
import com.pixstop.mobile.domain.model.Cart
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.Product
import com.pixstop.mobile.support.FakeApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * O estoque que a vitrine mostra é o `available` — já descontado das reservas
 * de outras pessoas. Cair no `stock` cheio faria a loja oferecer o que o
 * checkout vai recusar.
 */
class ShopRepositoryTest {

    private val productsBody = """
        {"success":true,"data":[
          {"id":3,"name":"Água Mineral 500ml","description":"Item de demonstração.","image_url":null,
           "category":{"id":3,"name":"Bebidas"},
           "price_money":3,"price_money_discounted":3,"price_pixels":300,"price_pixels_discounted":300,
           "discount_percentage":0,"stock":49,"is_active":true,"available":40},
          {"id":9,"name":"Chocolate","description":null,"image_url":null,
           "category":{"id":4,"name":"Doces"},
           "price_money":10,"price_money_discounted":8,"price_pixels":1000,"price_pixels_discounted":800,
           "discount_percentage":20,"stock":5,"is_active":true,"available":0}
        ],
        "meta":{"current_page":1,"last_page":3,"per_page":20,"total":50}}
    """.trimIndent()

    @Test
    fun `usa o estoque disponivel, nao o cheio`() = runTest {
        val result = ShopRepository(FakeApi().clientReturning(productsBody)).products()

        assertIs<Outcome.Success<Page<Product>>>(result)
        assertEquals(40, result.value.items.first().available)
    }

    @Test
    fun `sem estoque disponivel o produto fica esgotado`() = runTest {
        val result = ShopRepository(FakeApi().clientReturning(productsBody)).products()

        assertIs<Outcome.Success<Page<Product>>>(result)

        val esgotado = result.value.items.last()
        assertTrue(esgotado.isSoldOut)
        assertTrue(esgotado.hasDiscount)
        assertEquals(8.0, esgotado.priceMoneyDiscounted)
        assertEquals(800, esgotado.pricePixelsDiscounted)
    }

    @Test
    fun `busca e categoria vao para o servidor`() = runTest {
        val api = FakeApi()

        ShopRepository(api.clientReturning(productsBody)).products(page = 2, query = "  água ", categoryId = 3)

        val url = api.lastRequest?.url.toString()
        assertTrue(url.contains("page=2"), url)
        assertTrue(url.contains("q=") && url.contains("gua"), url)
        assertTrue(url.contains("category=3"), url)
    }

    @Test
    fun `busca em branco nao vira filtro vazio`() = runTest {
        val api = FakeApi()

        ShopRepository(api.clientReturning(productsBody)).products(query = "   ")

        assertTrue(!api.lastRequest?.url.toString().contains("q="), api.lastRequest?.url.toString().orEmpty())
    }

    @Test
    fun `carrinho traz o prazo da reserva como instante`() = runTest {
        val body = """
            {"success":true,"data":{
              "items":[{"id":4,"quantity":2,"unit_price":3,"subtotal":6,
                        "reserved_until":"2026-09-05T13:34:32+00:00",
                        "product":{"id":3,"name":"Água Mineral 500ml","price_money":3,
                                   "price_money_discounted":3,"price_pixels":300,
                                   "price_pixels_discounted":300,"discount_percentage":0,
                                   "stock":49,"is_active":true}}],
              "total_items":2,"total":6,"reservation_minutes":5}}
        """.trimIndent()

        val result = CartRepository(FakeApi().clientReturning(body)).cart()

        assertIs<Outcome.Success<Cart>>(result)
        assertEquals(2, result.value.totalItems)
        assertEquals(5, result.value.reservationMinutes)
        assertEquals(1_788_615_272_000L, result.value.items.first().reservedUntil)
    }
}
