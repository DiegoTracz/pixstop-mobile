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
)
