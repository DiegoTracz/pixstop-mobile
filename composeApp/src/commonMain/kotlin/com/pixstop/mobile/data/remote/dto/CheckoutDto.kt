package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Tudo que a tela de fechamento precisa, numa chamada só: o carrinho, a
 * carteira e as regras em vigor na empresa.
 */
@Serializable
data class CheckoutDto(
    val items: List<CartItemDto> = emptyList(),
    val totals: CheckoutTotalsDto = CheckoutTotalsDto(),
    val wallet: CheckoutWalletDto = CheckoutWalletDto(),
    val checkout: CheckoutRulesDto = CheckoutRulesDto(),
    @SerialName("saved_cards") val savedCards: List<SavedCardDto> = emptyList(),
    @SerialName("reservation_minutes") val reservationMinutes: Int = 0,
    /** A geladeira desta compra (Fase 9.7): vai abrir, e de que cor está. */
    val appliance: ApplianceDto? = null,
)

/**
 * A geladeira no pagamento (Fase 9.7). Tudo com padrão: um servidor que não
 * conhece o bloco continua servindo o fechamento.
 */
@Serializable
data class ApplianceDto(
    val presence: String = "unknown",
    val label: String = "",
    @SerialName("appliance_id") val applianceId: Long? = null,
    @SerialName("ble_available") val bleAvailable: Boolean = false,
    @SerialName("support_phone") val supportPhone: String? = null,
    @SerialName("led_color") val ledColor: String? = null,
)

@Serializable
data class CheckoutTotalsDto(val products: Double = 0.0, val cost: Double = 0.0)

@Serializable
data class CheckoutWalletDto(
    val pixels: Int = 0,
    /** Quanto da carteira é pixel comprado: paga a compra inteira, sem o teto. */
    @SerialName("pixels_as_money") val pixelsAsMoney: Int = 0,
    val balance: Double = 0.0,
)

/**
 * Regras da empresa. Vêm do servidor porque cada uma configura as suas — o app
 * adivinhar qualquer uma daria uma conta diferente da que o pedido vai usar.
 */
@Serializable
data class CheckoutRulesDto(
    @SerialName("max_pixels") val maxPixels: Int = 0,
    @SerialName("pixels_per_real") val pixelsPerReal: Int = 100,
    @SerialName("min_pixels_redeem") val minPixelsRedeem: Int = 0,
    @SerialName("max_discount_percentage") val maxDiscountPercentage: Double = 0.0,
    @SerialName("pixels_enabled") val pixelsEnabled: Boolean = false,
    @SerialName("cashback_percentage") val cashbackPercentage: Double = 0.0,
    val fees: CheckoutFeesDto = CheckoutFeesDto(),
    @SerialName("gateway_available") val gatewayAvailable: Boolean = false,
    @SerialName("mp_public_key") val mpPublicKey: String? = null,
    @SerialName("is_sandbox") val isSandbox: Boolean = false,
)

@Serializable
data class CheckoutFeesDto(
    val card: FeeDto = FeeDto(),
    val split: FeeDto = FeeDto(),
)

@Serializable
data class FeeDto(val percentage: Double = 0.0, val fixed: Double = 0.0)

@Serializable
data class SavedCardDto(
    val id: Long,
    @SerialName("last_four") val lastFour: String,
    val brand: String? = null,
    @SerialName("is_default") val isDefault: Boolean = false,
    val expires: String? = null,
)

/**
 * Fechamento do pedido.
 *
 * Os campos de cartão só vão quando o método é cartão; o servidor exige o
 * token só nesse caso.
 */
@Serializable
data class OrderStoreRequest(
    @SerialName("payment_method") val paymentMethod: String,
    val pixels: Int? = null,
    val balance: Double? = null,
    /** Cartão já guardado, quando a escolha foi por um deles. */
    @SerialName("saved_card_id") val savedCardId: Long? = null,
    val installments: Int? = null,
    /** Token de uso único do cartão novo; o número nunca chega aqui. */
    @SerialName("card_token") val cardToken: String? = null,
    @SerialName("doc_type") val documentType: String? = null,
    @SerialName("doc_number") val documentNumber: String? = null,
    @SerialName("save_card") val saveCard: Boolean? = null,
)

@Serializable
data class OrderDto(
    val id: Long,
    @SerialName("transaction_id") val transactionId: String? = null,
    val status: String,
    @SerialName("status_label") val statusLabel: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("payment_method_label") val paymentMethodLabel: String? = null,
    val totals: OrderTotalsDto = OrderTotalsDto(),
    val items: List<OrderItemDto> = emptyList(),
    val pix: PixDto? = null,
    @SerialName("cancellation_reason") val cancellationReason: String? = null,
    @SerialName("is_cancelable") val isCancelable: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("paid_at") val paidAt: String? = null,
    /** O que o pedido rendeu de XP; só vem no pedido criado e no aberto. */
    val xp: OrderXpDto? = null,
    /** A retirada (Fase 9.8): em que pé está e se dá para abrir agora. */
    val pickup: PickupDto? = null,
    /** O bilhete de abertura por Bluetooth (Fase 9.7), quando há. */
    @SerialName("unlock_ticket") val unlockTicket: String? = null,
    @SerialName("picked_up_at") val pickedUpAt: String? = null,
)

/**
 * O bloco da retirada (Fase 9.8).
 *
 * Tudo com padrão: um servidor antigo, que não conhece a retirada, continua
 * respondendo pedidos — e o app mostra o que sempre mostrou.
 */
@Serializable
data class PickupDto(
    val status: String = "none",
    @SerialName("can_unlock") val canUnlock: Boolean = false,
    val reason: String? = null,
    val attempts: Int = 0,
    @SerialName("max_attempts") val maxAttempts: Int = 0,
    @SerialName("window_until") val windowUntil: String? = null,
    @SerialName("door_opened_at") val doorOpenedAt: String? = null,
    @SerialName("picked_up_at") val pickedUpAt: String? = null,
    @SerialName("has_ticket") val hasTicket: Boolean = false,
    /** A cor de repouso da fita LED (Fase 9.6). */
    @SerialName("led_color") val ledColor: String? = null,
)

@Serializable
data class OrderTotalsDto(
    val products: Double = 0.0,
    val pixels: Int = 0,
    val balance: Double = 0.0,
    val money: Double = 0.0,
    @SerialName("card_fee") val cardFee: Double = 0.0,
)

@Serializable
data class OrderItemDto(
    val id: Long,
    @SerialName("product_id") val productId: Long? = null,
    @SerialName("product_name") val productName: String,
    val quantity: Int,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    val subtotal: Double = 0.0,
)

@Serializable
data class PixDto(
    @SerialName("qr_code") val qrCode: String? = null,
    @SerialName("qr_code_base64") val qrCodeBase64: String? = null,
    @SerialName("ticket_url") val ticketUrl: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class OrderStatusDto(
    val status: String,
    @SerialName("status_label") val statusLabel: String? = null,
    @SerialName("paid_at") val paidAt: String? = null,
    @SerialName("picked_up_at") val pickedUpAt: String? = null,
    /** A retirada (Fase 9.8): é o que a tela acompanha enquanto a porta não abre. */
    val pickup: PickupDto? = null,
    /** O bilhete de abertura por Bluetooth (Fase 9.7), enquanto valer. */
    @SerialName("unlock_ticket") val unlockTicket: String? = null,
)

/**
 * A resposta do "Abrir a geladeira" (Fase 9.8). A recusa vem com o mesmo
 * bloco: a tela não precisa de outra chamada para saber o que dizer.
 */
@Serializable
data class UnlockResponseDto(
    val pickup: PickupDto? = null,
    @SerialName("unlock_ticket") val unlockTicket: String? = null,
)

/** Um bilhete novo para um pedido que ainda não abriu a porta (Fase 9.7). */
@Serializable
data class UnlockTicketDto(
    @SerialName("unlock_ticket") val unlockTicket: String? = null,
    @SerialName("unlock_ticket_expires_at") val expiresAt: String? = null,
)
