package com.pixstop.mobile.data.mapper

import com.pixstop.mobile.core.text.IsoInstant
import com.pixstop.mobile.data.remote.dto.CheckoutDto
import com.pixstop.mobile.data.remote.dto.OrderDto
import com.pixstop.mobile.data.remote.dto.OrderItemDto
import com.pixstop.mobile.data.remote.dto.ApplianceDto
import com.pixstop.mobile.data.remote.dto.PickupDto
import com.pixstop.mobile.data.remote.dto.PixDto
import com.pixstop.mobile.data.remote.dto.SavedCardDto
import com.pixstop.mobile.domain.model.Checkout
import com.pixstop.mobile.domain.model.FridgeLight
import com.pixstop.mobile.domain.model.Order
import com.pixstop.mobile.domain.model.OrderXp
import com.pixstop.mobile.domain.model.OrderLine
import com.pixstop.mobile.domain.model.OrderStatus
import com.pixstop.mobile.domain.model.ApplianceStatus
import com.pixstop.mobile.domain.model.Pickup
import com.pixstop.mobile.domain.model.PickupReason
import com.pixstop.mobile.domain.model.PickupStatus
import com.pixstop.mobile.domain.model.PixPayment
import com.pixstop.mobile.domain.model.Presence
import com.pixstop.mobile.domain.model.SavedCard
import com.pixstop.mobile.domain.model.UnlockTicket

fun CheckoutDto.toDomain() = Checkout(
    productsTotal = totals.products,
    walletPixels = wallet.pixels,
    walletPixelsAsMoney = wallet.pixelsAsMoney,
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
    // Mesma regra do site: em ambiente de demonstração o token nasce local, e
    // é por isso que o formulário aparece mesmo sem chave pública.
    cardTokenizationAvailable = checkout.gatewayAvailable &&
        (checkout.isSandbox || !checkout.mpPublicKey.isNullOrBlank()),
    savedCards = savedCards.map { it.toDomain() },
    itemCount = items.sumOf { it.quantity },
    appliance = appliance?.toDomain() ?: ApplianceStatus.Unknown,
)

fun SavedCardDto.toDomain() = SavedCard(
    id = id,
    lastFour = lastFour,
    brand = brand,
    isDefault = isDefault,
    expires = expires,
)

/** A geladeira no pagamento (Fase 9.7). */
fun ApplianceDto.toDomain() = ApplianceStatus(
    presence = Presence.from(presence),
    label = label,
    applianceId = applianceId,
    bleAvailable = bleAvailable,
    supportPhone = supportPhone?.takeIf { it.isNotBlank() },
    restingLight = FridgeLight.from(ledColor),
)

fun OrderDto.toDomain() = Order(
    id = id,
    transactionId = transactionId,
    status = OrderStatus.from(status),
    statusLabel = statusLabel ?: OrderStatus.from(status).name,
    paymentMethodLabel = paymentMethodLabel,
    productsTotal = totals.products,
    pixels = totals.pixels,
    // Os pedidos antigos foram pagos com saldo, e sem esta linha a soma do
    // pedido não fecha (docs/plans/CARTEIRA_PIXELS.md, P4).
    balance = totals.balance,
    money = totals.money,
    cardFee = totals.cardFee,
    items = items.map { it.toDomain() },
    pix = pix?.toDomain(),
    cancellationReason = cancellationReason,
    isCancelable = isCancelable,
    createdAt = createdAt,
    xp = xp?.toDomain() ?: OrderXp.None,
    pickup = pickup?.toDomain() ?: Pickup.None,
    ticket = UnlockTicket.parse(unlockTicket),
)

/**
 * A retirada (Fase 9.8). Um status desconhecido vira `None`, que é o único
 * que não promete porta nenhuma.
 */
fun PickupDto.toDomain() = Pickup(
    status = PickupStatus.from(status),
    canUnlock = canUnlock,
    reason = PickupReason.from(reason),
    attempts = attempts,
    maxAttempts = maxAttempts,
    windowUntil = IsoInstant.toEpochMillis(windowUntil),
    doorOpenedAt = IsoInstant.toEpochMillis(doorOpenedAt),
    pickedUpAt = IsoInstant.toEpochMillis(pickedUpAt),
    hasTicket = hasTicket,
    restingLight = FridgeLight.from(ledColor),
)

fun OrderItemDto.toDomain() = OrderLine(
    id = id,
    productId = productId,
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
