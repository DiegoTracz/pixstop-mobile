package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCallPaged
import com.pixstop.mobile.core.network.safeCallUnit
import com.pixstop.mobile.data.remote.dto.NotificationDto
import com.pixstop.mobile.data.remote.dto.Page
import com.pixstop.mobile.domain.model.AppNotification
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.map
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody

private const val TAG = "Notifications"

/**
 * Caixa de avisos.
 *
 * A contagem de não lidas vem no rodapé da própria listagem — o servidor a
 * inclui de propósito para o badge não custar uma segunda chamada.
 */
class NotificationRepository(private val client: HttpClient) {

    suspend fun list(page: Int = 1, perPage: Int = 20): Outcome<Page<AppNotification>> =
        safeCallPaged<NotificationDto>(TAG) {
            client.get(ApiConfig.Endpoints.NOTIFICATIONS) {
                parameter("page", page)
                parameter("per_page", perPage)
            }
        }.map { page -> Page(page.items.map { it.toDomain() }, page.meta) }

    suspend fun markRead(id: String): Outcome<Unit> =
        safeCallUnit(TAG) { client.post(ApiConfig.Endpoints.notificationRead(id)) }

    suspend fun markAllRead(): Outcome<Unit> =
        safeCallUnit(TAG) { client.post(ApiConfig.Endpoints.NOTIFICATIONS_READ_ALL) }

    /** Liga o aparelho à conta para receber push. O servidor troca o dono se o token já for de outra. */
    suspend fun registerDevice(token: String, platform: String, deviceName: String?, appVersion: String?): Outcome<Unit> =
        safeCallUnit(TAG) {
            client.post(ApiConfig.Endpoints.DEVICES) {
                setBody(
                    mapOf(
                        "token" to token,
                        "platform" to platform,
                        "device_name" to deviceName,
                        "app_version" to appVersion,
                    ),
                )
            }
        }

    suspend fun unregisterDevice(token: String): Outcome<Unit> =
        safeCallUnit(TAG) { client.delete(ApiConfig.Endpoints.device(token)) }
}

private fun NotificationDto.toDomain(): AppNotification {
    // Sem título a linha ficaria vazia; o corpo costuma bastar para entender.
    val resolvedTitle = title ?: body ?: "Aviso"

    return AppNotification(
        id = id,
        type = type,
        title = resolvedTitle,
        // Se o título veio do corpo, repeti-lo logo abaixo não acrescenta nada.
        body = body.takeIf { it != resolvedTitle },
        actionUrl = actionUrl,
        read = read,
        createdAt = createdAt,
    )
}
