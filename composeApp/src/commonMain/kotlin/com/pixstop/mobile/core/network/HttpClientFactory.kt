package com.pixstop.mobile.core.network

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.storage.TokenManager

/**
 * Factory para criar HttpClient configurado para a API Laravel
 */
object HttpClientFactory {

    fun create(tokenManager: TokenManager): HttpClient {
        return HttpClient {
            // JSON Serialization
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                    prettyPrint = false
                })
            }

            // Logging (apenas em debug)
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.BODY
            }

            // Timeout
            install(HttpTimeout) {
                connectTimeoutMillis = ApiConfig.CONNECTION_TIMEOUT_MS
                requestTimeoutMillis = ApiConfig.REQUEST_TIMEOUT_MS
                socketTimeoutMillis = ApiConfig.REQUEST_TIMEOUT_MS
            }

            // Default headers
            defaultRequest {
                // Garante que a URL base termina com /
                val baseUrl = ApiConfig.baseUrl.trimEnd('/') + "/"
                url(baseUrl)
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)

                // Adiciona token se disponível
                tokenManager.getToken()?.let { token ->
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        }
    }
}
