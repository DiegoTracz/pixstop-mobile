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
    val missions: List<ProgressionMission> = emptyList(),
    val campaign: ProgressionCampaign? = null,
    val previousSeason: PreviousSeason? = null,
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

/** Uma missão da temporada: "semana completa", "nova categoria". */
data class ProgressionMission(
    val code: String,
    val label: String,
    val xp: Int,
    val progress: Int,
    val target: Int,
    val done: Boolean,
) {
    /** A fração da barra, de 0 a 1. */
    val fraction: Float get() = if (target <= 0) 0f else (progress.coerceIn(0, target).toFloat() / target)

    /** O que o "2 de 3" mede, no texto da tela. */
    val unit: String get() = when (code) {
        "week_complete" -> "dias com compra nesta semana"
        "new_category" -> "categorias novas nesta temporada"
        else -> ""
    }
}

/** A campanha da plataforma valendo agora. */
data class ProgressionCampaign(
    val name: String,
    val multiplier: Double,
    val endsAt: String?,
) {
    /** "×2" ou "×2,5": sem casa decimal quando ela é zero. */
    val multiplierLabel: String get() {
        val rounded = (multiplier * 10).toInt()
        return if (rounded % 10 == 0) "×${rounded / 10}" else "×${rounded / 10},${rounded % 10}"
    }
}

/** Onde a pessoa parou na temporada passada. */
data class PreviousSeason(
    val season: Int,
    val level: Int,
    val title: String?,
    val xpTotal: Int,
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
