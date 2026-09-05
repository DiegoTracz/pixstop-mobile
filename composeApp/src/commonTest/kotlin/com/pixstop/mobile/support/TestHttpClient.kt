package com.pixstop.mobile.support

import com.pixstop.mobile.core.network.apiJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json

/**
 * Cliente de teste com a mesma configuração do real.
 *
 * Sem o ContentNegotiation e o content-type padrão o corpo de um POST nem
 * chega a ser serializado, e o teste passaria a medir outra coisa.
 */
class FakeApi(status: HttpStatusCode = HttpStatusCode.OK) {

    /**
     * Trocável entre chamadas: às vezes o teste precisa carregar com sucesso e
     * só então fazer a chamada seguinte falhar.
     */
    var status: HttpStatusCode = status

    var lastRequest: HttpRequestData? = null
        private set

    /** O corpo enviado na última chamada, para conferir o que o app mandou. */
    val lastBody: String get() = (lastRequest?.body as? TextContent)?.text.orEmpty()

    fun clientReturning(body: String): HttpClient = HttpClient(
        MockEngine { request ->
            lastRequest = request
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        },
    ) {
        install(ContentNegotiation) { json(apiJson) }

        defaultRequest {
            url("https://exemplo.test/api/")
            contentType(ContentType.Application.Json)
        }
    }
}
