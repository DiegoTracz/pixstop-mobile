package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.data.remote.dto.ApplyPixelAvatarDto
import com.pixstop.mobile.data.remote.dto.PixelAvatarPreviewDto
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.map
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

private const val TAG = "Avatar"

/**
 * O avatar em pixel art (Fase 8).
 *
 * Duas etapas de propósito: gerar não troca a foto de ninguém. Só o `apply`
 * mexe no perfil, e é isso que deixa a pessoa tirar quantas fotos quiser sem
 * medo de perder o avatar que já tinha.
 */
class AvatarRepository(private val client: HttpClient) {

    /** Manda a foto e recebe a prévia em pixel art. */
    suspend fun generatePixel(photo: ByteArray): Outcome<String> =
        safeCall<PixelAvatarPreviewDto>(TAG) {
            client.post(ApiConfig.Endpoints.GENERATE_PIXEL_AVATAR) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                key = "photo",
                                value = photo,
                                headers = Headers.build {
                                    append(HttpHeaders.ContentType, ContentType.Image.JPEG.toString())
                                    append(HttpHeaders.ContentDisposition, "filename=\"selfie.jpg\"")
                                },
                            )
                        },
                    ),
                )
            }
        }.map { it.preview }

    /** "Gostei deste": a prévia vira a foto do perfil. */
    suspend fun applyPixel(): Outcome<String> =
        safeCall<ApplyPixelAvatarDto>(TAG) {
            client.post(ApiConfig.Endpoints.APPLY_PIXEL_AVATAR)
        }.map { it.avatarUrl }
}
