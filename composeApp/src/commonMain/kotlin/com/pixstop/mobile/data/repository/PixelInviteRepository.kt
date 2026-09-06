package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.data.remote.dto.AcceptInviteRequest
import com.pixstop.mobile.data.remote.dto.AcceptedInviteDto
import com.pixstop.mobile.data.remote.dto.PixelInviteDto
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.PixelInvite
import com.pixstop.mobile.domain.model.map
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/**
 * O convite com pixels: ler antes de aceitar, aceitar uma vez.
 */
class PixelInviteRepository(private val client: HttpClient) {

    suspend fun show(code: String): Outcome<PixelInvite> =
        safeCall<PixelInviteDto>(TAG) { client.get(ApiConfig.Endpoints.pixelInvite(code)) }
            .map { it.toDomain() }

    suspend fun accept(code: String, phone: String?): Outcome<AcceptedInviteDto> =
        safeCall(TAG) {
            client.post(ApiConfig.Endpoints.pixelInviteAccept(code)) {
                setBody(AcceptInviteRequest(phone?.filter(Char::isDigit)?.takeIf { it.isNotBlank() }))
            }
        }

    private companion object {
        const val TAG = "PixelInvite"
    }
}

fun PixelInviteDto.toDomain() = PixelInvite(
    code = code,
    pixels = pixels,
    message = message,
    recipientName = recipientName,
    needsPhone = needsPhone,
    status = status,
    statusLabel = statusLabel,
    companyId = company.id,
    companyName = company.name,
)
