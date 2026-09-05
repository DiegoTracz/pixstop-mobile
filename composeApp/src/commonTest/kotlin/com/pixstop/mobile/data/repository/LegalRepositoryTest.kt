package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.network.apiJson
import com.pixstop.mobile.domain.model.LegalDocument
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.ui.viewmodel.LegalConsentUiState
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * O aceite é o que destrava as rotas de negócio no servidor. Se o app mandar
 * a lista errada, a pessoa aceita e continua barrada — daí a cobertura do
 * corpo enviado, e não só do resultado.
 */
class LegalRepositoryTest {

    private val documentsBody = """
        {"success":true,"data":[
          {"id":1,"type":"terms","type_label":"Termos de Uso","title":"Termos de Uso",
           "version":"1.0","content":"Texto dos termos","published_at":"2026-01-10T00:00:00+00:00","accepted":false},
          {"id":2,"type":"privacy","type_label":"Política de Privacidade","title":"Política de Privacidade",
           "version":"2.0","content":"Texto da política","published_at":"2026-02-01T00:00:00+00:00","accepted":true}
        ]}
    """.trimIndent()

    private var lastRequest: HttpRequestData? = null

    private fun client(body: String) = HttpClient(
        MockEngine { request ->
            lastRequest = request
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        },
    ) {
        // Mesma configuração do cliente real: sem o ContentNegotiation e o
        // content-type padrão o corpo do POST nem chega a ser serializado, e o
        // teste passaria a medir outra coisa.
        install(ContentNegotiation) { json(apiJson) }

        defaultRequest {
            url("https://exemplo.test/api/")
            contentType(ContentType.Application.Json)
        }
    }

    @Test
    fun `lista os documentos em vigor com o que ja foi aceito`() = runTest {
        val result = LegalRepository(client(documentsBody)).documents()

        assertIs<Outcome.Success<List<LegalDocument>>>(result)
        assertEquals(2, result.value.size)
        assertFalse(result.value.first().accepted)
        assertTrue(result.value.last().accepted)
        assertEquals("Termos de Uso", result.value.first().title)
    }

    @Test
    fun `aceite envia os documentos nomeados`() = runTest {
        val body = """{"success":true,"data":{"recorded":1,"pending":0}}"""

        val result = LegalRepository(client(body)).accept(listOf(1L))

        assertIs<Outcome.Success<*>>(result)

        val sent = (lastRequest?.body as? TextContent)?.text.orEmpty()
        assertTrue(sent.contains("document_ids"), "corpo enviado: $sent")
        assertTrue(sent.contains("1"), "corpo enviado: $sent")
    }

    @Test
    fun `so envia o que falta aceitar`() {
        val state = LegalConsentUiState(
            documents = listOf(
                LegalDocument(1, "terms", "Termos de Uso", "Termos de Uso", "1.0", "…", accepted = false),
                LegalDocument(2, "privacy", "Política", "Política", "2.0", "…", accepted = true),
            ),
            isLoading = false,
        )

        assertEquals(listOf(1L), state.pending.map { it.id })
    }

    @Test
    fun `botao so libera com a caixa marcada`() {
        val documents = listOf(
            LegalDocument(1, "terms", "Termos de Uso", "Termos de Uso", "1.0", "…", accepted = false),
        )

        assertFalse(LegalConsentUiState(documents = documents, isLoading = false).canSubmit)
        assertTrue(LegalConsentUiState(documents = documents, isLoading = false, accepted = true).canSubmit)
    }

    @Test
    fun `sem pendencia nao ha o que enviar`() {
        val state = LegalConsentUiState(
            documents = listOf(
                LegalDocument(1, "terms", "Termos de Uso", "Termos de Uso", "1.0", "…", accepted = true),
            ),
            isLoading = false,
            accepted = true,
        )

        assertFalse(state.canSubmit)
    }
}
