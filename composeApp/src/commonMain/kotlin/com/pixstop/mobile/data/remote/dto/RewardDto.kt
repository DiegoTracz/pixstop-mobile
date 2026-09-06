package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** O catálogo de recompensas e os vouchers da pessoa, como o servidor manda. */
@Serializable
data class RewardsPageDto(
    val available: Int = 0,
    val rewards: List<RewardDto> = emptyList(),
    val vouchers: List<VoucherDto> = emptyList(),
)

@Serializable
data class RewardDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    val pixels: Int = 0,
    val stock: Int? = null,
    @SerialName("validity_days") val validityDays: Int = 30,
    val available: Boolean = true,
    val affordable: Boolean = false,
    val missing: Int = 0,
)

@Serializable
data class VoucherDto(
    val id: Long,
    val code: String,
    val qr: String? = null,
    @SerialName("reward_name") val rewardName: String,
    val pixels: Int = 0,
    val status: String = "open",
    @SerialName("status_label") val statusLabel: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("validated_at") val validatedAt: String? = null,
    val name: String? = null,
) {
    val isOpen: Boolean get() = status == "open"
}

@Serializable
data class ValidateVoucherRequest(val code: String)

/** O meu link de indicação e quem já veio por ele (Fase 14). */
@Serializable
data class ReferralDto(
    val code: String,
    val url: String,
    val message: String,
    @SerialName("whatsapp_url") val whatsappUrl: String? = null,
    val xp: Int = 0,
    val pending: Int = 0,
    val accepted: Int = 0,
    val remaining: Int? = null,
)
