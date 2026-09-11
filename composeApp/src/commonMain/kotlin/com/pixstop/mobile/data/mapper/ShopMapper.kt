package com.pixstop.mobile.data.mapper

import com.pixstop.mobile.core.text.IsoInstant
import com.pixstop.mobile.data.remote.dto.ApplianceChoiceDto
import com.pixstop.mobile.data.remote.dto.CartDto
import com.pixstop.mobile.data.remote.dto.CartItemDto
import com.pixstop.mobile.data.remote.dto.CategoryDto
import com.pixstop.mobile.data.remote.dto.PixelBalanceDto
import com.pixstop.mobile.data.remote.dto.ProductDto
import com.pixstop.mobile.data.remote.dto.ShopApplianceDto
import com.pixstop.mobile.domain.model.Appliance
import com.pixstop.mobile.domain.model.ApplianceChoice
import com.pixstop.mobile.domain.model.Cart
import com.pixstop.mobile.domain.model.CartLine
import com.pixstop.mobile.domain.model.Category
import com.pixstop.mobile.domain.model.PixelWallet
import com.pixstop.mobile.domain.model.Presence
import com.pixstop.mobile.domain.model.Product

fun CategoryDto.toDomain() = Category(id = id, name = name, productsCount = productsCount)

/**
 * Sem `available` na resposta, o estoque cheio é o melhor palpite — é o que
 * acontece nas rotas que não calculam reserva, como o item do carrinho.
 */
fun ProductDto.toDomain() = Product(
    id = id,
    name = name,
    description = description,
    imageUrl = imageUrl,
    categoryName = category?.name,
    priceMoney = priceMoney,
    priceMoneyDiscounted = priceMoneyDiscounted,
    pricePixels = pricePixels,
    pricePixelsDiscounted = pricePixelsDiscounted,
    discountPercentage = discountPercentage,
    available = available ?: stock,
)

fun CartDto.toDomain() = Cart(
    items = items.map { it.toDomain() },
    totalItems = totalItems,
    total = total,
    reservationMinutes = reservationMinutes,
)

fun CartItemDto.toDomain() = CartLine(
    id = id,
    quantity = quantity,
    unitPrice = unitPrice,
    subtotal = subtotal,
    reservedUntil = IsoInstant.toEpochMillis(reservedUntil),
    product = product.toDomain(),
)

fun PixelBalanceDto.toDomain() = PixelWallet(
    balance = balance,
    reserved = reserved,
    available = available,
    expiringSoon = expiringSoon,
    pixelsPerReal = pixelsPerReal,
    minRedeem = minRedeem,
    maxDiscountPercentage = maxDiscountPercentage,
    cashbackPercentage = cashbackPercentage,
    enabled = enabled,
)

/**
 * A geladeira como a tela de escolha precisa dela (Fase 9.5).
 */
fun ShopApplianceDto.toDomain() = Appliance(
    id = id,
    name = name,
    location = location,
    presence = Presence.from(presence),
)

fun ApplianceChoiceDto.toDomain() = ApplianceChoice(
    mustChoose = mustChoose,
    currentId = currentId,
    appliances = appliances.map { it.toDomain() },
)
