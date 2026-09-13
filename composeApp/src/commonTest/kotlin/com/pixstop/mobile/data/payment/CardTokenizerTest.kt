package com.pixstop.mobile.data.payment

import com.pixstop.mobile.data.repository.AppConfigRepository
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.payment.CardInput
import com.pixstop.mobile.support.FakeApi
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * O número do cartão vai do aparelho direto ao gateway e nunca passa pelo nosso
 * servidor — é isso que mantém a operação fora do escopo de PCI. O que sai
 * daqui é um token de uso único.
 */
class CardTokenizerTest {

    private val cartao = CardInput(
        number = "4235 6477 2802 5682",
        expiry = "11/30",
        securityCode = "123",
        holderName = "  ana costa  ",
        documentNumber = "529.982.247-25",
    )

    private val configBody = """
        {"success":true,"data":{
          "app_name":"Pixelstop",
          "cart":{"reservation_minutes":5,"pix_expiration_minutes":30,"low_stock_threshold":5},
          "payment":{"max_installments":12,"public_key":"APP_USR-abc","is_sandbox":false},
          "pixels":{"pixels_per_real":100},
          "account":{"purge_after_days":90},
          "legal":{"terms_url":null,"privacy_url":null},
          "upload":{"max_image_kb":5120},
          "trial_days":30}}
    """.trimIndent()

    private val sandboxBody = configBody.replace("\"is_sandbox\":false", "\"is_sandbox\":true")

    private suspend fun configWith(body: String): AppConfigRepository =
        AppConfigRepository(FakeApi().clientReturning(body)).also { it.refresh() }

    @Test
    fun `o cartao vai ao gateway do jeito que ele espera`() = runTest {
        val gateway = FakeApi()
        val tokenizer = CardTokenizer(
            client = gateway.clientReturning("""{"id":"tok-123"}"""),
            config = configWith(configBody),
        )

        val result = tokenizer.tokenize(cartao)

        assertIs<Outcome.Success<String>>(result)
        assertEquals("tok-123", result.value)

        // Só dígitos: a máscara é da tela, o gateway recusa o resto.
        assertContains(gateway.lastBody, "\"card_number\":\"4235647728025682\"")
        assertContains(gateway.lastBody, "\"expiration_month\":11")
        assertContains(gateway.lastBody, "\"expiration_year\":2030")
        assertContains(gateway.lastBody, "\"number\":\"52998224725\"")
        assertContains(gateway.lastBody, "\"name\":\"ana costa\"")
    }

    @Test
    fun `a chave publica autoriza a chamada e vai na url`() = runTest {
        val gateway = FakeApi()

        CardTokenizer(gateway.clientReturning("""{"id":"tok-1"}"""), configWith(configBody)).tokenize(cartao)

        val url = gateway.lastRequest?.url.toString()

        assertContains(url, "api.mercadopago.com/v1/card_tokens")
        assertContains(url, "public_key=APP_USR-abc")
    }

    @Test
    fun `no ambiente de demonstracao o token nasce local`() = runTest {
        // O servidor usa um gateway de mentira; pedir um token real daria erro.
        val gateway = FakeApi()
        val tokenizer = CardTokenizer(
            client = gateway.clientReturning("""{"id":"nunca-usado"}"""),
            config = configWith(sandboxBody),
            randomToken = { "abc" },
        )

        val result = tokenizer.tokenize(cartao)

        assertIs<Outcome.Success<String>>(result)
        assertEquals("sandbox-card-token-abc", result.value)
        assertTrue(gateway.lastRequest == null, "não deveria ter falado com o gateway")
    }

    @Test
    fun `a recusa do gateway vira o campo a consertar`() = runTest {
        val gateway = FakeApi(HttpStatusCode.BadRequest)
        val body = """{"message":"invalid card_number","cause":[{"code":"205","description":"..."}]}"""

        val result = CardTokenizer(gateway.clientReturning(body), configWith(configBody)).tokenize(cartao)

        assertIs<Outcome.Failure>(result)
        assertEquals("Confira o número do cartão.", result.error.message)
    }

    @Test
    fun `codigo de causa desconhecido vira mensagem generica`() = runTest {
        val gateway = FakeApi(HttpStatusCode.BadRequest)
        val body = """{"message":"whatever","cause":[{"code":"99999"}]}"""

        val result = CardTokenizer(gateway.clientReturning(body), configWith(configBody)).tokenize(cartao)

        assertIs<Outcome.Failure>(result)
        // Em inglês e falando de campo interno, a mensagem do gateway não serve
        // para ninguém; a nossa pelo menos diz o que fazer.
        assertContains(result.error.message, "Confira os dados")
    }

    @Test
    fun `resposta sem token nao passa por sucesso`() = runTest {
        val gateway = FakeApi()

        val result = CardTokenizer(gateway.clientReturning("""{"status":"ok"}"""), configWith(configBody))
            .tokenize(cartao)

        assertIs<Outcome.Failure>(result)
    }
}
