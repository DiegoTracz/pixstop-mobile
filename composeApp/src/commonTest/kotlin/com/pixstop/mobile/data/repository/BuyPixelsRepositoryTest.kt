package com.pixstop.mobile.data.repository

import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.TopupMethod
import com.pixstop.mobile.support.FakeApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Comprar pixels: o corpo é o que o servidor valida, e a oferta é o que decide
 * se a tela deixa comprar.
 */
class BuyPixelsRepositoryTest {

    private val pixBody = """
        {"success":true,"data":{"id":5,"pixels":5000,"amount":50.0,"card_fee":0.0,"charged":50.0,
         "payment_method":"pix","status":"pending","credited":false,"refused":false,"status_detail":null,
         "pix":{"qr_code":"000201-pix","qr_code_base64":"aGVsbG8=","expires_at":"2026-09-19T15:30:00-03:00"},
         "created_at":null}}
    """.trimIndent()

    @Test
    fun `um pix manda so valor e forma de pagamento`() = runTest {
        val api = FakeApi()

        val result = ShopRepository(api.clientReturning(pixBody)).buyPixels(reais = 50, method = TopupMethod.Pix)

        assertContains(api.lastBody, "\"amount\":50")
        assertContains(api.lastBody, "\"payment_method\":\"pix\"")
        assertFalse(api.lastBody.contains("card_token"))

        val topup = assertIs<Outcome.Success<com.pixstop.mobile.domain.model.WalletTopup>>(result).value
        assertEquals(5000, topup.pixels)
        assertEquals("000201-pix", topup.pixCode)
        assertEquals(TopupMethod.Pix, topup.method)
        assertTrue(topup.pixExpiresAt != null)
    }

    @Test
    fun `o cartao vai tokenizado com o documento`() = runTest {
        val api = FakeApi()

        ShopRepository(api.clientReturning(pixBody)).buyPixels(
            reais = 20,
            method = TopupMethod.Card,
            cardToken = "tok-9",
            documentType = "CPF",
            documentNumber = "12345678909",
        )

        assertContains(api.lastBody, "\"card_token\":\"tok-9\"")
        assertContains(api.lastBody, "\"doc_number\":\"12345678909\"")
    }

    @Test
    fun `a carteira traz a oferta de compra e o motivo quando nao da`() = runTest {
        val body = """
            {"success":true,"data":{"balance":0,"reserved":0,"available":0,"enabled":true,
             "topup":{"enabled":false,"reason":"Esta empresa ainda não recebe pagamentos.","min":10,"max":500,
                      "presets":[20,50,100],"pixels_per_real":100,"card_fee":{"percentage":4.0,"fixed":0.0}}}}
        """.trimIndent()

        val wallet = assertIs<Outcome.Success<com.pixstop.mobile.domain.model.PixelWallet>>(
            ShopRepository(FakeApi().clientReturning(body)).pixelWallet(),
        ).value

        assertFalse(wallet.topup.enabled)
        assertEquals("Esta empresa ainda não recebe pagamentos.", wallet.topup.reason)
        assertEquals(listOf(20, 50, 100), wallet.topup.presets)
        assertEquals(4.0, wallet.topup.cardFeePercentage)
    }
}
