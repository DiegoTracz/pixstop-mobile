package com.pixstop.mobile.domain.checkout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Esta conta aparece na tela antes de o pedido existir. Se ela divergir da do
 * servidor, a pessoa autoriza um valor e é cobrada outro — o pior tipo de
 * surpresa numa tela de pagamento. Os casos abaixo seguem o
 * `OrderTotalsCalculator` do backend.
 */
class CheckoutTotalsTest {

    @Test
    fun `sem pixels nem saldo, tudo vira dinheiro`() {
        val total = CheckoutTotals.calculate(products = 6.0, method = PaymentMethod.Money)

        assertEquals(6.0, total.money)
        assertEquals(0.0, total.balance)
        assertEquals(0.0, total.pixelsDiscount)
        assertEquals(6.0, total.charged)
        assertTrue(total.needsGateway)
    }

    @Test
    fun `pixels viram desconto a cem por real`() {
        val total = CheckoutTotals.calculate(products = 6.0, method = PaymentMethod.Money, pixelsToRedeem = 300)

        assertEquals(3.0, total.pixelsDiscount)
        assertEquals(3.0, total.money)
    }

    @Test
    fun `desconto de pixels nao passa do total dos produtos`() {
        val total = CheckoutTotals.calculate(products = 2.0, method = PaymentMethod.Money, pixelsToRedeem = 1_000)

        assertEquals(2.0, total.pixelsDiscount)
        assertEquals(0.0, total.money)
        assertFalse(total.needsGateway)
    }

    @Test
    fun `no metodo saldo o saldo cobre o que sobrou`() {
        val total = CheckoutTotals.calculate(products = 9.0, method = PaymentMethod.Balance, pixelsToRedeem = 200)

        assertEquals(2.0, total.pixelsDiscount)
        assertEquals(7.0, total.balance)
        assertEquals(0.0, total.money)
        assertFalse(total.needsGateway)
    }

    @Test
    fun `saldo escolhido nunca cobre mais do que falta`() {
        val total = CheckoutTotals.calculate(
            products = 6.0,
            method = PaymentMethod.Mixed,
            pixelsToRedeem = 200,
            balanceToUse = 50.0,
        )

        assertEquals(4.0, total.balance, "só sobravam R$ 4,00 depois dos pixels")
        assertEquals(0.0, total.money)
    }

    @Test
    fun `taxa de cartao entra so quando ha dinheiro a cobrar`() {
        val semDinheiro = CheckoutTotals.calculate(
            products = 6.0,
            method = PaymentMethod.Card,
            balanceToUse = 6.0,
            cardFeePercentage = 4.99,
            cardFeeFixed = 0.39,
        )

        assertEquals(0.0, semDinheiro.cardFee)

        val comDinheiro = CheckoutTotals.calculate(
            products = 100.0,
            method = PaymentMethod.Card,
            cardFeePercentage = 4.99,
            cardFeeFixed = 0.39,
        )

        assertEquals(5.38, comDinheiro.cardFee)
        assertEquals(105.38, comDinheiro.charged)
    }

    @Test
    fun `pix nao leva taxa de cartao`() {
        val total = CheckoutTotals.calculate(
            products = 100.0,
            method = PaymentMethod.Money,
            cardFeePercentage = 4.99,
            cardFeeFixed = 0.39,
        )

        assertEquals(0.0, total.cardFee)
        assertEquals(100.0, total.charged)
    }

    @Test
    fun `teto de pixels respeita o desconto maximo da empresa`() {
        // R$ 6,00 com teto de 50% = R$ 3,00 = 300 pixels, mas só há 250.
        val comSaldoCurto = CheckoutTotals.maxPixelsFor(6.0, 250, 100, 50.0, 10, enabled = true)
        assertEquals(250, comSaldoCurto)

        val comSaldoSobrando = CheckoutTotals.maxPixelsFor(6.0, 5_000, 100, 50.0, 10, enabled = true)
        assertEquals(300, comSaldoSobrando)
    }

    @Test
    fun `abaixo do minimo o teto e zero`() {
        assertEquals(0, CheckoutTotals.maxPixelsFor(6.0, 5, 100, 50.0, 10, enabled = true))
    }

    @Test
    fun `pixels desligados na empresa zeram o teto`() {
        assertEquals(0, CheckoutTotals.maxPixelsFor(6.0, 5_000, 100, 50.0, 10, enabled = false))
    }

    @Test
    fun `metodo desconhecido cai em dinheiro, nunca em cartao`() {
        assertEquals(PaymentMethod.Money, PaymentMethod.from("carteira-nova"))
        assertEquals(PaymentMethod.Card, PaymentMethod.from("card"))
    }
}
