package com.pixstop.mobile.domain.model

/**
 * Onde a pessoa está na progressão por XP.
 *
 * XP não é dinheiro e não se gasta; é só o quanto falta para o próximo nível,
 * que é quem paga pixels. A barra mede o trecho entre o nível de agora e o
 * próximo, não o XP desde o zero.
 */
data class Progression(
    val season: Int,
    val seasonEndsAt: String?,
    val xpTotal: Int,
    val level: Int,
    val title: String?,
    val xpIntoLevel: Int,
    val xpToNext: Int,
    val progressPercent: Int,
    val nextLevel: ProgressionLevel?,
    val streakDays: Int,
    val levels: List<ProgressionLevel>,
) {
    /** A fração da barra, de 0 a 1, para desenhar. */
    val progressFraction: Float get() = (progressPercent.coerceIn(0, 100)) / 100f

    val isAtTop: Boolean get() = nextLevel == null

    /** Sequência só vale ser dita a partir do segundo dia. */
    val hasStreak: Boolean get() = streakDays >= 2

    /** O que aparece no lugar do nome quando o nível ainda não tem um. */
    val displayTitle: String get() = title ?: if (level == 0) "Novato" else "Nível $level"
}

data class ProgressionLevel(
    val level: Int,
    val xpRequired: Int,
    val rewardPixels: Int,
    val title: String?,
    val reached: Boolean,
)

/** Uma linha do histórico de XP. `xp == 0` é uma subida de nível. */
data class XpEntry(
    val id: Long,
    val ruleCode: String,
    val xp: Int,
    val description: String?,
    val level: Int?,
    val grantedPixels: Int?,
    val createdAt: String?,
) {
    val isLevelUp: Boolean get() = ruleCode == "level_up"
}

/** O que um pedido rendeu. */
data class OrderXp(val earned: Int, val leveledUpTo: Int?) {
    val isEmpty: Boolean get() = earned <= 0 && leveledUpTo == null

    companion object {
        val None = OrderXp(0, null)
    }
}
