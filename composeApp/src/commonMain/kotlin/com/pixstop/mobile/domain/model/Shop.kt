package com.pixstop.mobile.domain.model

/**
 * Categoria da vitrine.
 */
data class Category(val id: Long, val name: String, val productsCount: Int)

/**
 * Produto como a vitrine precisa dele.
 */
data class Product(
    val id: Long,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val categoryName: String?,
    val priceMoney: Double,
    val priceMoneyDiscounted: Double,
    val pricePixels: Int,
    val pricePixelsDiscounted: Int,
    val discountPercentage: Int,
    /** Estoque já descontado das reservas alheias. */
    val available: Int,
) {
    val hasDiscount: Boolean get() = discountPercentage > 0

    val isSoldOut: Boolean get() = available <= 0
}

/**
 * Carrinho.
 *
 * Os itens ficam reservados por alguns minutos; passado o prazo, o estoque
 * volta para a vitrine mesmo sem ninguém mexer.
 */
data class Cart(
    val items: List<CartLine>,
    val totalItems: Int,
    val total: Double,
    val reservationMinutes: Int,
) {
    val isEmpty: Boolean get() = items.isEmpty()

    companion object {
        val Empty = Cart(items = emptyList(), totalItems = 0, total = 0.0, reservationMinutes = 0)
    }
}

data class CartLine(
    val id: Long,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double,
    /** Instante em que a reserva vence, em epoch de milissegundos. */
    val reservedUntil: Long?,
    val product: Product,
)

/**
 * Carteira de pixels e as regras de resgate em vigor.
 */
data class PixelWallet(
    val balance: Int,
    val reserved: Int,
    val available: Int,
    val expiringSoon: Int,
    val pixelsPerReal: Int,
    val minRedeem: Int,
    val maxDiscountPercentage: Double,
    val cashbackPercentage: Double,
    val enabled: Boolean,
) {
    companion object {
        val Empty = PixelWallet(0, 0, 0, 0, 100, 0, 0.0, 0.0, enabled = false)
    }
}

/**
 * A geladeira de onde a pessoa vai tirar o produto (Fase 9.5).
 *
 * O estoque, a reserva e a porta que abre são de uma só. Quem tem uma
 * geladeira na empresa nunca escolhe nada: ela é a resposta.
 */
data class Appliance(
    val id: Long,
    val name: String,
    val location: String?,
    /** Como o servidor a viu por último. O mesmo `Presence` do checkout. */
    val presence: Presence,
) {
    /** O que aparece embaixo do nome na lista: o andar, a sala, o corredor. */
    val subtitle: String? get() = location?.takeIf { it.isNotBlank() }
}

/**
 * O que o app precisa saber para perguntar (ou não) de qual geladeira é a
 * compra.
 */
data class ApplianceChoice(
    val mustChoose: Boolean,
    val currentId: Long?,
    val appliances: List<Appliance>,
) {
    val current: Appliance? get() = appliances.firstOrNull { it.id == currentId }

    /** Com uma porta só não há pergunta a fazer, e nenhuma tela muda. */
    val hasChoice: Boolean get() = appliances.size > 1

    companion object {
        val Empty = ApplianceChoice(mustChoose = false, currentId = null, appliances = emptyList())
    }
}
