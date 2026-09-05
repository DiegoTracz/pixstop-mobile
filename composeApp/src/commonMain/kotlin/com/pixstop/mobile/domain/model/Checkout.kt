package com.pixstop.mobile.domain.model

/**
 * O que a tela de fechamento precisa saber: quanto custa, com o que se pode
 * pagar e quais as regras da empresa.
 */
data class Checkout(
    val productsTotal: Double,
    val walletPixels: Int,
    val walletBalance: Double,
    val maxPixels: Int,
    val pixelsPerReal: Int,
    val minPixelsRedeem: Int,
    val maxDiscountPercentage: Double,
    val pixelsEnabled: Boolean,
    val cashbackPercentage: Double,
    val cardFeePercentage: Double,
    val cardFeeFixed: Double,
    val gatewayAvailable: Boolean,
    /** Sem chave pública não há como tokenizar cartão neste aparelho. */
    val cardTokenizationAvailable: Boolean,
    val savedCards: List<SavedCard>,
    val itemCount: Int,
)

data class SavedCard(
    val id: Long,
    val lastFour: String,
    val brand: String?,
    val isDefault: Boolean,
    val expires: String?,
)

/**
 * Pedido fechado.
 */
data class Order(
    val id: Long,
    val transactionId: String?,
    val status: OrderStatus,
    val statusLabel: String,
    val paymentMethodLabel: String?,
    val productsTotal: Double,
    val pixels: Int,
    val balance: Double,
    val money: Double,
    val cardFee: Double,
    val items: List<OrderLine>,
    val pix: PixPayment?,
    val cancellationReason: String?,
    val isCancelable: Boolean,
    val createdAt: String?,
) {
    val isPaid: Boolean get() = status == OrderStatus.Paid
    val isPending: Boolean get() = status == OrderStatus.Pending || status == OrderStatus.InReview
}

data class OrderLine(
    val id: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double,
)

data class PixPayment(
    /** O "copia e cola" que a pessoa leva para o aplicativo do banco. */
    val code: String,
    val qrCodeBase64: String?,
    val ticketUrl: String?,
    val expiresAt: Long?,
)

/**
 * Situação do pedido.
 *
 * Um status desconhecido conta como pendente: é o único que não promete nada
 * a quem está esperando o produto.
 */
enum class OrderStatus(val apiValue: String) {
    Pending("pending"),
    InReview("in_review"),
    Paid("paid"),
    Delivered("delivered"),
    Canceled("canceled");

    companion object {
        fun from(value: String?): OrderStatus =
            entries.firstOrNull { it.apiValue == value } ?: Pending
    }
}
