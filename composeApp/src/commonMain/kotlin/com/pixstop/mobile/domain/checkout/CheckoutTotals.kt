package com.pixstop.mobile.domain.checkout

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Como o total se divide entre pixels e dinheiro.
 */
data class CheckoutBreakdown(
    val products: Double,
    val pixels: Int,
    val pixelsDiscount: Double,
    val money: Double,
    val cardFee: Double,
    val charged: Double,
) {
    /** Cobrado no cartão ou no PIX, já com a taxa que o gateway repassa. */
    val needsGateway: Boolean get() = charged > 0
}

/**
 * Espelho da conta que o servidor faz ao criar o pedido.
 *
 * Existe porque a tela precisa mostrar o desfecho antes de enviar, e uma conta
 * própria daria um número diferente do cobrado — o pior tipo de surpresa numa
 * tela de pagamento. As regras são as mesmas do `OrderTotalsCalculator`:
 * pixels viram desconto, o que sobrou é cobrado, a taxa de cartão entra só
 * quando há dinheiro a cobrar, e a taxa de split é retida da empresa e nunca
 * somada a quem compra.
 */
object CheckoutTotals {

    fun calculate(
        products: Double,
        method: PaymentMethod,
        pixelsToRedeem: Int = 0,
        pixelsPerReal: Int = 100,
        cardFeePercentage: Double = 0.0,
        cardFeeFixed: Double = 0.0,
    ): CheckoutBreakdown {
        val productsTotal = round2(products)
        val pixelsDiscount = min(pixelsToMoney(pixelsToRedeem, pixelsPerReal), productsTotal)
        val remaining = round2(productsTotal - pixelsDiscount)

        // O saldo em reais saiu (docs/plans/CARTEIRA_PIXELS.md, P4): a
        // carteira é só de pixels, e o que os pixels não cobrem é cobrado.
        val money = remaining

        val cardFee = if (money > 0 && method == PaymentMethod.Card) {
            round2(money * cardFeePercentage / 100 + cardFeeFixed)
        } else {
            0.0
        }

        return CheckoutBreakdown(
            products = productsTotal,
            pixels = pixelsToRedeem,
            pixelsDiscount = pixelsDiscount,
            money = money,
            cardFee = cardFee,
            charged = round2(money + cardFee),
        )
    }

    /**
     * Teto de pixels para este total, respeitando mínimo e desconto máximo.
     *
     * Abaixo do mínimo o resultado é zero: oferecer um resgate que o servidor
     * vai recusar só gastaria uma ida e volta.
     */
    fun maxPixelsFor(
        productsTotal: Double,
        availablePixels: Int,
        pixelsPerReal: Int,
        maxDiscountPercentage: Double,
        minRedeem: Int,
        enabled: Boolean,
    ): Int {
        if (!enabled || productsTotal <= 0) {
            return 0
        }

        val cap = floor(productsTotal * pixelsPerReal * (maxDiscountPercentage / 100)).toInt()
        val maximum = min(cap, availablePixels)

        return if (maximum >= minRedeem) maximum else 0
    }

    fun pixelsToMoney(pixels: Int, pixelsPerReal: Int): Double =
        round2(pixels.toDouble() / pixelsPerReal)

    /**
     * Duas casas com arredondamento para cima no meio, como o `round()` do PHP.
     * O padrão do Kotlin arredonda para o par mais próximo e divergiria em um
     * centavo justamente nos valores de meio.
     */
    private fun round2(value: Double): Double = (value * 100).roundToLong() / 100.0
}

/**
 * Formas de pagamento aceitas pelo servidor.
 */
enum class PaymentMethod(val apiValue: String) {
    Pixels("pixels"),
    Money("money"),
    Card("card"),
    Mixed("mixed");

    companion object {
        fun from(value: String?): PaymentMethod =
            entries.firstOrNull { it.apiValue == value } ?: Money
    }
}
