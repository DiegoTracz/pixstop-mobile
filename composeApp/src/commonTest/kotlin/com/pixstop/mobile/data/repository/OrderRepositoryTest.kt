package com.pixstop.mobile.data.repository

import com.pixstop.mobile.domain.checkout.PaymentMethod
import com.pixstop.mobile.support.FakeApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

/**
 * O corpo do pedido é o que o servidor valida. Mandar um campo de cartão numa
 * compra em PIX faria o servidor guardar parcelas de um pagamento à vista;
 * deixar de mandar num pagamento com cartão dá 422 depois de a pessoa já ter
 * escolhido tudo.
 */
class OrderRepositoryTest {

    private val orderBody = """
        {"success":true,"data":{"id":9,"transaction_id":"ORD-X","status":"paid","status_label":"Pago",
         "payment_method":"card","totals":{"products":10.0,"pixels":0,"balance":0.0,"money":10.0,"card_fee":0.5},
         "items":[],"cancellation_reason":null,"is_cancelable":false,"created_at":null}}
    """.trimIndent()

    @Test
    fun `o cartao guardado e as parcelas vao no corpo`() = runTest {
        val api = FakeApi()

        OrderRepository(api.clientReturning(orderBody)).place(
            method = PaymentMethod.Card,
            pixels = 0,
            balance = 0.0,
            savedCardId = 4,
            installments = 3,
        )

        assertContains(api.lastBody, "\"saved_card_id\":4")
        assertContains(api.lastBody, "\"installments\":3")
    }

    @Test
    fun `parcela unica nao e enviada`() = runTest {
        // À vista é o padrão do servidor; mandar `1` só ocuparia o corpo.
        val api = FakeApi()

        OrderRepository(api.clientReturning(orderBody)).place(
            method = PaymentMethod.Card,
            pixels = 0,
            balance = 0.0,
            savedCardId = 4,
            installments = 1,
        )

        assertFalse(api.lastBody.contains("\"installments\":"), api.lastBody)
    }

    @Test
    fun `pagar em PIX nao leva nada de cartao`() = runTest {
        val api = FakeApi()

        OrderRepository(api.clientReturning(orderBody)).place(
            method = PaymentMethod.Money,
            pixels = 0,
            balance = 0.0,
            savedCardId = 4,
            installments = 6,
        )

        assertFalse(api.lastBody.contains("\"saved_card_id\":"), api.lastBody)
        assertFalse(api.lastBody.contains("\"installments\":"), api.lastBody)
    }

    @Test
    fun `pixels e saldo zerados nao entram no corpo`() = runTest {
        val api = FakeApi()

        OrderRepository(api.clientReturning(orderBody)).place(
            method = PaymentMethod.Balance,
            pixels = 0,
            balance = 0.0,
        )

        // Repare no dois-pontos: `balance` sozinho também é o valor de
        // `payment_method`, e a busca crua acusaria um campo que não existe.
        assertFalse(api.lastBody.contains("\"pixels\":"), api.lastBody)
        assertFalse(api.lastBody.contains("\"balance\":"), api.lastBody)
    }
}
