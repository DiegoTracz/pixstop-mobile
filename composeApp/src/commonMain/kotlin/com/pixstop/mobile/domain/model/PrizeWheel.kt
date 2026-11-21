package com.pixstop.mobile.domain.model

/**
 * A roleta de prêmios (docs/plans/LANCAMENTO_LOCARMAIS.md no servidor).
 *
 * Os giros de ouro vêm de uma vez, para quem comprou de verdade; o giro de
 * compra nasce de um pedido pago com moedas, um por dia. O sorteio é do
 * servidor: o app só anima até a fatia que ele disse.
 */
data class PrizeWheel(
    val id: Long,
    val name: String,
    val message: String?,
    val endsAt: String?,
    val goldSpins: Int,
    val purchaseSpins: Int,
    val guaranteedJackpot: Boolean,
    val wonPixels: Int,
    val purchaseMinTotal: Double,
    val goldSlices: List<PrizeWheelSlice>,
    val purchaseSlices: List<PrizeWheelSlice>,
    val recent: List<PrizeWheelRecent>,
) {
    val spinsLeft: Int get() = goldSpins + purchaseSpins

    /** O próximo giro é de ouro enquanto houver; depois, os de compra. */
    val nextKind: PrizeSpinKind get() = if (goldSpins > 0) PrizeSpinKind.Gold else PrizeSpinKind.Purchase

    fun slicesOf(kind: PrizeSpinKind): List<PrizeWheelSlice> =
        if (kind == PrizeSpinKind.Gold) goldSlices else purchaseSlices
}

enum class PrizeSpinKind {
    Gold, Purchase;

    companion object {
        fun fromApi(value: String?): PrizeSpinKind = if (value == "purchase") Purchase else Gold
    }
}

/** Um prêmio da roda: `weight` reparte as 16 fatias desenhadas, `chance` vai no "i". */
data class PrizeWheelSlice(
    val label: String,
    val pixels: Int,
    val isJackpot: Boolean,
    val weight: Double = 1.0,
    val chance: Double = 0.0,
)

data class PrizeWheelRecent(val kind: PrizeSpinKind, val label: String?, val pixels: Int, val isJackpot: Boolean)

data class PrizeSpinResult(
    val kind: PrizeSpinKind,
    val sliceIndex: Int,
    val label: String,
    val pixels: Int,
    val isJackpot: Boolean,
    val forced: Boolean,
    val wheel: PrizeWheel?,
) {
    /** Prêmio que merece o baú das recompensas, e não só a linha de texto. */
    val isBig: Boolean get() = isJackpot || pixels >= BIG_PRIZE_PIXELS

    private companion object {
        const val BIG_PRIZE_PIXELS = 300
    }
}
