package com.pixstop.mobile.data.repository

import com.pixstop.mobile.domain.model.AppConfig
import com.pixstop.mobile.support.FakeApi
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * O app não pode adivinhar prazo nem limite que o servidor publica — um número
 * escrito no código fica errado no dia em que a configuração muda, e ninguém
 * percebe. Mas depender da rota também não pode travar a abertura: ela é
 * consultada antes do login, e cai como qualquer outra.
 */
class AppConfigRepositoryTest {

    private val body = """
        {"success":true,"data":{
          "app_name":"Pixstop",
          "cart":{"reservation_minutes":7,"pix_expiration_minutes":45,"low_stock_threshold":3},
          "payment":{"max_installments":6,"public_key":"APP_USR-abc","is_sandbox":true},
          "pixels":{"pixels_per_real":50},
          "account":{"purge_after_days":45},
          "legal":{"terms_url":"https://pixstop.com.br/termos","privacy_url":"https://pixstop.com.br/privacidade"},
          "upload":{"max_image_kb":2048},
          "trial_days":14}}
    """.trimIndent()

    @Test
    fun `comeca nos padroes embutidos, antes de qualquer resposta`() {
        val repository = AppConfigRepository(FakeApi().clientReturning(body))

        assertEquals(AppConfig.Defaults, repository.config.value)
    }

    @Test
    fun `substitui os padroes pelo que o servidor publica`() = runTest {
        val repository = AppConfigRepository(FakeApi().clientReturning(body))

        repository.refresh()

        val config = repository.config.value
        assertEquals(7, config.cartReservationMinutes)
        assertEquals(45, config.accountPurgeAfterDays)
        assertEquals(50, config.pixelsPerReal)
        assertEquals(6, config.maxInstallments)
        assertEquals(14, config.trialDays)
        assertEquals("https://pixstop.com.br/termos", config.termsUrl)
    }

    @Test
    fun `rota fora do ar mantem os padroes e nao vira erro de tela`() = runTest {
        val repository = AppConfigRepository(
            FakeApi(HttpStatusCode.ServiceUnavailable).clientReturning("""{"success":false}"""),
        )

        repository.refresh()

        assertEquals(AppConfig.Defaults, repository.config.value)
    }

    @Test
    fun `resposta sem um bloco cai no padrao daquele bloco`() = runTest {
        val repository = AppConfigRepository(
            FakeApi().clientReturning("""{"success":true,"data":{"account":{"purge_after_days":30}}}"""),
        )

        repository.refresh()

        assertEquals(30, repository.config.value.accountPurgeAfterDays)
        assertEquals(AppConfig.Defaults.cartReservationMinutes, repository.config.value.cartReservationMinutes)
    }

    @Test
    fun `sem chave publica nao ha como tokenizar cartao`() = runTest {
        val repository = AppConfigRepository(
            FakeApi().clientReturning("""{"success":true,"data":{"payment":{"public_key":""}}}"""),
        )

        repository.refresh()

        assertFalse(repository.config.value.canTokenizeCard)

        val comChave = AppConfigRepository(FakeApi().clientReturning(body))
        comChave.refresh()

        assertTrue(comChave.config.value.canTokenizeCard)
    }
}
