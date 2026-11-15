package com.pixstop.mobile.domain.model

/**
 * O desafio coletivo (docs/plans/CONQUISTAS.md no servidor): um placar comum
 * com cotas. Bater uma cota rende XP igual para quem ajudou no trecho, e o
 * nível paga os pixels. A quantidade de cada colega nunca chega aqui — só a
 * da própria pessoa.
 */
data class Challenge(
    val id: Long,
    val name: String,
    val description: String?,
    val unitLabel: String,
    val unitsTotal: Int,
    val goal: Int,
    val endsAt: String?,
    val hasEnded: Boolean,
    val xpEnabled: Boolean,
    val quotas: List<ChallengeQuota>,
    val nextQuota: ChallengeNextQuota?,
    val myUnits: Int,
    val isHidden: Boolean,
    val participantsCount: Int,
    val participantNames: List<ChallengeParticipant>,
    val moreParticipants: Int,
) {
    /** O placar inteiro, de 0 a 1, para a barra. */
    val fraction: Float
        get() = if (goal <= 0) 0f else (unitsTotal.toFloat() / goal).coerceIn(0f, 1f)

    val hasJoined: Boolean get() = myUnits > 0

    val allQuotasReached: Boolean get() = quotas.isNotEmpty() && quotas.all { it.reached }

    /** A frase de baixo da barra: o que falta, ou como terminou. */
    val headline: String
        get() = when {
            hasEnded -> "Encerrado com $unitsTotal $unitLabel."
            nextQuota != null -> "Faltam ${nextQuota.remaining} $unitLabel para a cota de ${nextQuota.unitsRequired}, " +
                "que rende ${nextQuota.rewardXp} XP para quem ajudar."
            allQuotasReached -> "Todas as cotas batidas!"
            else -> "$unitsTotal de $goal $unitLabel."
        }
}

data class ChallengeQuota(
    val unitsRequired: Int,
    val rewardXp: Int,
    val title: String?,
    val reached: Boolean,
    val earned: Boolean,
) {
    val label: String get() = title?.let { "$unitsRequired · $it" } ?: unitsRequired.toString()
}

data class ChallengeNextQuota(
    val unitsRequired: Int,
    val remaining: Int,
    val rewardXp: Int,
    val title: String?,
    /** A pessoa já tem compra no trecho: o XP desta cota é dela quando bater. */
    val inStretch: Boolean,
)

data class ChallengeParticipant(val name: String, val isMe: Boolean)

/** Uma medalha ganha: presença e variedade, nunca volume. */
data class Achievement(
    val code: String,
    val title: String,
    val description: String,
    val xp: Int,
    val earnedAt: String?,
    val challenge: String?,
)

data class ChallengePage(val challenge: Challenge?, val achievements: List<Achievement>) {
    companion object {
        val Empty = ChallengePage(null, emptyList())
    }
}

/** O que um pedido somou no desafio. */
data class OrderChallenge(
    val challenge: String,
    val units: Int,
    val unitsTotal: Int,
    val goal: Int,
    val unitLabel: String,
) {
    val label: String get() = "+$units no desafio · $unitsTotal/$goal $unitLabel"
}
