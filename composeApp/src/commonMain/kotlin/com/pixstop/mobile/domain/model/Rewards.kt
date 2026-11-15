package com.pixstop.mobile.domain.model

/**
 * O que um pedido rendeu, e as cenas que o app toca por causa disso.
 *
 * A ordem das cenas é do menor para o maior: o cashback curto abre, a cota e
 * as medalhas vêm no meio, e o nível fecha — é ele que paga o prêmio grande.
 */
data class OrderRewards(
    val cashbackPixels: Int = 0,
    val level: LevelReward? = null,
    val quota: QuotaReward? = null,
    val medals: List<MedalReward> = emptyList(),
) {
    val isEmpty: Boolean get() = cashbackPixels <= 0 && level == null && quota == null && medals.isEmpty()

    fun scenes(): List<RewardScene> = buildList {
        if (cashbackPixels > 0) {
            add(
                RewardScene(
                    kind = RewardKind.Cashback,
                    tag = "COMPRA PAGA",
                    title = "+$cashbackPixels PIXELS",
                    subtitle = "Cashback da compra",
                    prize = "NA CARTEIRA",
                    currency = RewardCurrency.Pixels,
                    gain = cashbackPixels,
                    coins = 10,
                    chest = RewardChest.None,
                ),
            )
        }

        quota?.let { quota ->
            add(
                RewardScene(
                    kind = RewardKind.Quota,
                    tag = "DESAFIO",
                    title = "COTA BATIDA!",
                    subtitle = "${quota.unitsRequired} ${quota.unitLabel} · ${quota.challenge}",
                    prize = if (quota.xp > 0) "+${quota.xp} XP" else "PLACAR",
                    currency = RewardCurrency.Xp,
                    gain = quota.xp,
                    coins = if (quota.xp > 0) 20 else 0,
                    chest = RewardChest.Green,
                ),
            )
        }

        if (medals.isNotEmpty()) {
            val xp = medals.sumOf { it.xp }

            add(
                RewardScene(
                    kind = RewardKind.Medal,
                    tag = "MEDALHA",
                    title = if (medals.size == 1) "MEDALHA!" else "${medals.size} MEDALHAS!",
                    subtitle = medals.joinToString(" · ") { it.title },
                    prize = if (xp > 0) "+$xp XP" else "NO PERFIL",
                    currency = RewardCurrency.Xp,
                    gain = xp,
                    coins = if (xp > 0) 12 else 0,
                    chest = RewardChest.Blue,
                    trophy = true,
                ),
            )
        }

        level?.let { level ->
            add(
                RewardScene(
                    kind = RewardKind.Level,
                    tag = "LEVEL ${level.level}",
                    title = "LEVEL UP!",
                    subtitle = level.title?.let { "Nível ${level.level} · $it" } ?: "Nível ${level.level}",
                    prize = if (level.pixels > 0) "+${level.pixels} PIXELS" else "NOVO NIVEL",
                    currency = RewardCurrency.Pixels,
                    gain = level.pixels,
                    coins = if (level.pixels > 0) 30 else 0,
                    chest = RewardChest.Blue,
                ),
            )
        }
    }

    companion object {
        val None = OrderRewards()
    }
}

data class LevelReward(val level: Int, val title: String?, val pixels: Int)

data class QuotaReward(val challenge: String, val unitsRequired: Int, val unitLabel: String, val xp: Int)

data class MedalReward(val code: String, val title: String, val xp: Int)

enum class RewardKind { Cashback, Quota, Medal, Level }

enum class RewardChest { None, Blue, Green }

enum class RewardCurrency { Pixels, Xp }

/**
 * Uma cena de recompensa: os textos, o que sobe no contador e o que aparece
 * no palco. Os títulos em fonte pixel não levam acento: a fonte não tem
 * maiúscula acentuada. O subtítulo vai na fonte de texto, e leva.
 */
data class RewardScene(
    val kind: RewardKind,
    val tag: String,
    val title: String,
    val subtitle: String,
    val prize: String,
    val currency: RewardCurrency,
    val gain: Int,
    val coins: Int,
    val chest: RewardChest,
    val trophy: Boolean = false,
) {
    /** Sem baú não há o que abrir: a cena curta começa sozinha. */
    val opensByItself: Boolean get() = chest == RewardChest.None
}
