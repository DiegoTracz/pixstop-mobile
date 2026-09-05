package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Limites e textos que o app teria de adivinhar.
 *
 * Cada bloco traz um padrão igual ao do servidor: a rota é pública e consultada
 * antes do login, e uma indisponibilidade dela nunca pode travar o aplicativo.
 */
@Serializable
data class AppConfigDto(
    @SerialName("app_name") val appName: String? = null,
    val cart: CartConfigDto = CartConfigDto(),
    val payment: PaymentConfigDto = PaymentConfigDto(),
    val pixels: PixelsConfigDto = PixelsConfigDto(),
    val account: AccountConfigDto = AccountConfigDto(),
    val legal: LegalConfigDto = LegalConfigDto(),
    val upload: UploadConfigDto = UploadConfigDto(),
    @SerialName("trial_days") val trialDays: Int = 30,
)

@Serializable
data class CartConfigDto(
    @SerialName("reservation_minutes") val reservationMinutes: Int = 5,
    @SerialName("pix_expiration_minutes") val pixExpirationMinutes: Int = 30,
    @SerialName("low_stock_threshold") val lowStockThreshold: Int = 5,
)

@Serializable
data class PaymentConfigDto(
    @SerialName("max_installments") val maxInstallments: Int = 12,
    @SerialName("public_key") val publicKey: String? = null,
    @SerialName("is_sandbox") val isSandbox: Boolean = false,
)

@Serializable
data class PixelsConfigDto(
    @SerialName("pixels_per_real") val pixelsPerReal: Int = 100,
)

@Serializable
data class AccountConfigDto(
    @SerialName("purge_after_days") val purgeAfterDays: Int = 90,
)

@Serializable
data class LegalConfigDto(
    @SerialName("terms_url") val termsUrl: String? = null,
    @SerialName("privacy_url") val privacyUrl: String? = null,
)

@Serializable
data class UploadConfigDto(
    @SerialName("max_image_kb") val maxImageKb: Int = 5_120,
)
