package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    val id: Long,
    val name: String,
    @SerialName("products_count") val productsCount: Int = 0,
)

/**
 * Produto da vitrine.
 *
 * O `available` é o estoque já descontado das reservas de outras pessoas; o
 * `stock` é o número cheio. A tela precisa do primeiro — oferecer o segundo
 * faria o checkout recusar o que a vitrine prometeu.
 */
@Serializable
data class ProductDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val category: CategoryRefDto? = null,
    @SerialName("price_money") val priceMoney: Double = 0.0,
    @SerialName("price_money_discounted") val priceMoneyDiscounted: Double = 0.0,
    @SerialName("price_pixels") val pricePixels: Int = 0,
    @SerialName("price_pixels_discounted") val pricePixelsDiscounted: Int = 0,
    @SerialName("discount_percentage") val discountPercentage: Int = 0,
    val stock: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
    val available: Int? = null,
)

@Serializable
data class CategoryRefDto(val id: Long, val name: String)

@Serializable
data class CartDto(
    val items: List<CartItemDto> = emptyList(),
    @SerialName("total_items") val totalItems: Int = 0,
    val total: Double = 0.0,
    @SerialName("reservation_minutes") val reservationMinutes: Int = 0,
)

@Serializable
data class CartItemDto(
    val id: Long,
    val quantity: Int,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    val subtotal: Double = 0.0,
    @SerialName("reserved_until") val reservedUntil: String? = null,
    val product: ProductDto,
)

@Serializable
data class CartAddRequest(
    @SerialName("product_id") val productId: Long,
    val quantity: Int = 1,
    /**
     * De qual geladeira. Nulo deixa o servidor resolver — é o que acontece em
     * toda empresa que tem uma porta só.
     */
    @SerialName("appliance_id") val applianceId: Long? = null,
)

@Serializable
data class CartUpdateRequest(val quantity: Int)

/**
 * Carteira de pixels e as regras de resgate que a tela precisa saber.
 */
@Serializable
data class PixelBalanceDto(
    val balance: Int = 0,
    val reserved: Int = 0,
    val available: Int = 0,
    @SerialName("expiring_soon") val expiringSoon: Int = 0,
    @SerialName("pixels_per_real") val pixelsPerReal: Int = 100,
    @SerialName("min_redeem") val minRedeem: Int = 0,
    @SerialName("max_discount_percentage") val maxDiscountPercentage: Double = 0.0,
    @SerialName("cashback_percentage") val cashbackPercentage: Double = 0.0,
    val enabled: Boolean = true,
    val topup: TopupOfferDto? = null,
)

/**
 * A compra de pixels oferecida nesta empresa, e por que não quando não dá.
 */
@Serializable
data class TopupOfferDto(
    val enabled: Boolean = false,
    val reason: String? = null,
    val min: Int = 10,
    val max: Int = 500,
    val presets: List<Int> = emptyList(),
    @SerialName("pixels_per_real") val pixelsPerReal: Int = 100,
    @SerialName("card_fee") val cardFee: TopupFeeDto = TopupFeeDto(),
)

@Serializable
data class TopupFeeDto(val percentage: Double = 0.0, val fixed: Double = 0.0)

/**
 * Uma compra de pixels: quanto, quanto custou e em que pé está.
 */
@Serializable
data class WalletTopupDto(
    val id: Long,
    val pixels: Int,
    val amount: Double,
    @SerialName("card_fee") val cardFee: Double = 0.0,
    val charged: Double,
    @SerialName("payment_method") val paymentMethod: String,
    val status: String,
    val credited: Boolean = false,
    val refused: Boolean = false,
    @SerialName("status_detail") val statusDetail: String? = null,
    val pix: TopupPixDto? = null,
)

@Serializable
data class TopupPixDto(
    @SerialName("qr_code") val qrCode: String,
    @SerialName("qr_code_base64") val qrCodeBase64: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class WalletTopupRequest(
    val amount: Int,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("card_token") val cardToken: String? = null,
    @SerialName("doc_type") val docType: String? = null,
    @SerialName("doc_number") val docNumber: String? = null,
)

/**
 * Uma geladeira entre as quais escolher (Fase 9.5).
 *
 * O estoque, a reserva e a porta que abre são de **uma** geladeira. Com mais
 * de uma na empresa, a pessoa precisa dizer em frente a qual está.
 *
 * Não se confunde com o `ApplianceDto` do checkout: aquele é o **estado** da
 * geladeira do pedido (está de pé? abre por Bluetooth?), este é a identidade
 * de uma porta entre várias.
 */
@Serializable
data class ShopApplianceDto(
    val id: Long,
    val name: String,
    val location: String? = null,
    val type: String = "refrigerator",
    val presence: String = "unknown",
)

@Serializable
data class ApplianceChoiceDto(
    @SerialName("must_choose") val mustChoose: Boolean = false,
    @SerialName("current_id") val currentId: Long? = null,
    val appliances: List<ShopApplianceDto> = emptyList(),
)
