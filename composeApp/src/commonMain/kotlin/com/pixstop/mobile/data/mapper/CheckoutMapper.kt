package com.pixstop.mobile.data.mapper

import com.pixstop.mobile.core.text.IsoInstant
import com.pixstop.mobile.data.remote.dto.CheckoutDto
import com.pixstop.mobile.data.remote.dto.OrderDto
import com.pixstop.mobile.data.remote.dto.OrderItemDto
import com.pixstop.mobile.data.remote.dto.PixDto
import com.pixstop.mobile.data.remote.dto.SavedCardDto
import com.pixstop.mobile.domain.model.Checkout
import com.pixstop.mobile.domain.model.Order
import com.pixstop.mobile.domain.model.OrderLine
import com.pixstop.mobile.domain.model.OrderStatus
import com.pixstop.mobile.domain.model.PixPayment
import com.pixstop.mobile.domain.model.SavedCard

fun CheckoutDto.toDomain() = Checkout(
    productsTotal = totals.products,
    walletPixels = wallet.pixels,
    walletBalance = wallet.balance,
    maxPixels = checkout.maxPixels,
    pixelsPerReal = checkout.pixelsPerReal,
    minPixelsRedeem = checkout.minPixelsRedeem,
    maxDiscountPercentage = checkout.maxDiscountPercentage,
    pixelsEnabled = checkout.pixelsEnabled,
    cashbackPercentage = checkout.cashbackPercentage,
    cardFeePercentage = checkout.fees.card.percentage,
    cardFeeFixed = checkout.fees.card.fixed,
    gatewayAvailable = checkout.gatewayAvailable,
    // Sem chave pública não há como tokenizar o cartão no aparelho, mesmo com
    // o gateway conectado — oferecer a opção só levaria a um erro no envio.
    cardTokenizationAvailable = checkout.gatewayAvailable && !checkout.mpPublicKey.isNullOrBlank(),
    savedCards = savedCards.map { it.toDomain() },
    itemCount = items.sumOf { it.quantity },
)

fun SavedCardDto.toDomain() = SavedCard(
    id = id,
    lastFour = lastFour,
    brand = brand,
    isDefault = isDefault,
    expires = expires,
)

fun OrderDto.toDomain() = Order(
    id = id,
    transactionId = transactionId,
    status = OrderStatus.from(status),
    statusLabel = statusLabel ?: OrderStatus.from(status).name,
    paymentMethodLabel = paymentMethodLabel,
    productsTotal = totals.products,
    pixels = totals.pixels,
    balance = totals.balance,
    money = totals.money,
    cardFee = totals.cardFee,
    items = items.map { it.toDomain() },
    pix = pix?.toDomain(),
    cancellationReason = cancellationReason,
    isCancelable = isCancelable,
    createdAt = createdAt,
)

fun OrderItemDto.toDomain() = OrderLine(
    id = id,
    productName = productName,
    quantity = quantity,
    unitPrice = unitPrice,
    subtotal = subtotal,
)

/**
 * Sem o código "copia e cola" não há PIX que se pague — nesse caso é como se
 * não houvesse PIX nenhum.
 */
fun PixDto.toDomain(): PixPayment? = qrCode?.takeIf { it.isNotBlank() }?.let {
    PixPayment(
        code = it,
        qrCodeBase64 = qrCodeBase64,
        ticketUrl = ticketUrl,
        expiresAt = IsoInstant.toEpochMillis(expiresAt),
    )
}
