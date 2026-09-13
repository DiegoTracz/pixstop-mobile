package com.pixstop.mobile.data.remote

import com.pixstop.mobile.core.logging.AppLogger
import com.pixstop.mobile.core.network.apiJson
import com.pixstop.mobile.data.remote.dto.PortalConfigureRequest
import com.pixstop.mobile.data.remote.dto.PortalMessageDto
import com.pixstop.mobile.data.remote.dto.PortalNetworkDto
import com.pixstop.mobile.data.remote.dto.PortalStatusDto
import com.pixstop.mobile.domain.model.DomainError
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.PortalMode
import com.pixstop.mobile.domain.model.PortalStatus
import com.pixstop.mobile.domain.model.WifiNetwork
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode

private const val TAG = "Portal"

/**
 * Conversa com a geladeira no modo de configuração (Fase 9.3).
 *
 * É a mesma API que a página em `http://10.42.0.1` usa. Não passa pelo
 * servidor Pixelstop: o celular está na rede da geladeira, sem internet, e o
 * cliente HTTP daqui não manda o token de sessão para lugar nenhum.
 *
 * O `host` é configurável para a bancada: num PC, o agente simulado sobe em
 * `192.168.x.x:8080` em vez de `10.42.0.1`.
 */
class SetupPortalClient(private val client: HttpClient) {

    suspend fun status(host: String): Outcome<PortalStatus> = call(host) {
        val body = client.get(url(host, "/api/status")).bodyAsText()
        val dto = apiJson.decodeFromString<PortalStatusDto>(body)

        PortalStatus(PortalMode.fromApi(dto.mode), dto.message, dto.agentVersion)
    }

    suspend fun networks(host: String): Outcome<List<WifiNetwork>> = call(host) {
        val body = client.get(url(host, "/api/networks")).bodyAsText()

        apiJson.decodeFromString<List<PortalNetworkDto>>(body)
            .map { WifiNetwork(it.ssid, it.signal, it.secured) }
    }

    /**
     * Manda WiFi e código. A geladeira responde 202 e passa a trabalhar; o
     * resto se acompanha por `status()`.
     */
    suspend fun configure(host: String, ssid: String, password: String, code: String): Outcome<Unit> = call(host) {
        val response = client.post(url(host, "/api/configure")) {
            setBody(PortalConfigureRequest(ssid = ssid, password = password, code = code))
        }

        if (response.status != HttpStatusCode.Accepted) {
            val message = runCatching { apiJson.decodeFromString<PortalMessageDto>(response.bodyAsText()).message }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: "A geladeira recusou a configuração (${response.status.value})."

            throw PortalException(message)
        }
    }

    private fun url(host: String, path: String): String {
        val base = host.trim().removeSuffix("/")
        val withScheme = if (base.startsWith("http://") || base.startsWith("https://")) base else "http://$base"

        return withScheme + path
    }

    private suspend inline fun <T> call(host: String, crossinline block: suspend () -> T): Outcome<T> =
        try {
            Outcome.Success(block())
        } catch (error: PortalException) {
            Outcome.Failure(DomainError.Rule("portal", error.message ?: "A geladeira recusou."))
        } catch (error: Exception) {
            AppLogger.w("Portal em $host não respondeu: ${error.message}", tag = TAG)
            Outcome.Failure(
                DomainError.Offline(
                    "Não consegui falar com a geladeira em $host. O celular está na rede Pixelstop-Setup?",
                ),
            )
        }
}

private class PortalException(message: String) : Exception(message)
