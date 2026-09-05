package com.pixstop.mobile.core.network

import com.pixstop.mobile.core.config.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json

/**
 * Cliente para falar com o gateway de pagamento, e só com ele.
 *
 * É separado do cliente da API por duas razões, e as duas importam:
 *
 * - o cliente da API manda o nosso token de sessão em todo pedido, e ele não
 *   tem o que fazer no servidor de terceiro;
 * - em debug o cliente da API registra o corpo inteiro no log do aparelho, e
 *   aqui o corpo é o número do cartão.
 */
object GatewayClientFactory {

    fun create(): HttpClient = HttpClient {
        expectSuccess = false

        install(ContentNegotiation) {
            json(apiJson)
        }

        install(HttpTimeout) {
            connectTimeoutMillis = ApiConfig.CONNECTION_TIMEOUT_MS
            requestTimeoutMillis = ApiConfig.REQUEST_TIMEOUT_MS
            socketTimeoutMillis = ApiConfig.REQUEST_TIMEOUT_MS
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }
}
