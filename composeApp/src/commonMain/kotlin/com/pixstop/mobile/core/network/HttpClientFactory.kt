package com.pixstop.mobile.core.network

import com.pixstop.mobile.BuildKonfig
import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.logging.AppLogger
import com.pixstop.mobile.core.storage.SessionStore
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json

/**
 * O cliente HTTP do app, criado uma vez e injetado por toda parte.
 *
 * O token vai pelo plugin `Auth` em vez de um cabeçalho montado à mão: assim
 * o Ktor sabe reagir a um 401 sozinho — hoje encerrando a sessão, e no futuro
 * renovando pelo `/auth/refresh` sem que nenhuma tela perceba.
 */
object HttpClientFactory {

    fun create(session: SessionStore): HttpClient = HttpClient {
        expectSuccess = false

        install(ContentNegotiation) {
            json(apiJson)
        }

        install(Auth) {
            bearer {
                loadTokens {
                    session.currentToken()?.let { BearerTokens(it, "") }
                }

                // Um 401 significa token vencido ou revogado. Encerrar a sessão
                // aqui evita que cada tela precise adivinhar o que houve.
                refreshTokens {
                    AppLogger.w("Token recusado pelo servidor; encerrando a sessão.", tag = "Http")
                    session.expire()
                    null
                }
            }
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) = AppLogger.d(message, tag = "Http")
            }
            // Corpo completo só em debug: em release ele levaria token e senha
            // para o log do aparelho.
            level = if (BuildKonfig.DEBUG) LogLevel.BODY else LogLevel.NONE
        }

        install(HttpTimeout) {
            connectTimeoutMillis = ApiConfig.CONNECTION_TIMEOUT_MS
            requestTimeoutMillis = ApiConfig.REQUEST_TIMEOUT_MS
            socketTimeoutMillis = ApiConfig.REQUEST_TIMEOUT_MS
        }

        defaultRequest {
            url(ApiConfig.baseUrl.trimEnd('/') + "/")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }
    }
}
