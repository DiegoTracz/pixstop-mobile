package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** O convite com pixels como o servidor o descreve antes do aceite. */
@Serializable
data class PixelInviteDto(
    val code: String,
    val pixels: Int = 0,
    val message: String? = null,
    @SerialName("recipient_name") val recipientName: String? = null,
    @SerialName("needs_phone") val needsPhone: Boolean = false,
    val status: String = "sent",
    @SerialName("status_label") val statusLabel: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    val company: PixelInviteCompanyDto,
)

@Serializable
data class PixelInviteCompanyDto(
    val id: String,
    val name: String,
    @SerialName("logo_url") val logoUrl: String? = null,
)

@Serializable
data class AcceptInviteRequest(val phone: String? = null)

/** O que volta do aceite: a empresa em que a pessoa entrou e o que ganhou. */
@Serializable
data class AcceptedInviteDto(
    val id: String,
    val name: String,
    val pixels: Int = 0,
)
