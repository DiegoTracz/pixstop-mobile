package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * O desafio coletivo e as medalhas, como o servidor descreve
 * (docs/plans/CONQUISTAS.md no servidor). `challenge` nulo quer dizer que a
 * empresa não tem desafio agora; as medalhas vêm de qualquer jeito.
 */
@Serializable
data class ChallengePageDto(
    val challenge: ChallengeDto? = null,
    val achievements: List<AchievementDto> = emptyList(),
)

@Serializable
data class ChallengeDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    @SerialName("unit_label") val unitLabel: String = "unidades",
    @SerialName("units_total") val unitsTotal: Int = 0,
    val goal: Int = 1,
    @SerialName("progress_percent") val progressPercent: Int = 0,
    @SerialName("ends_at") val endsAt: String? = null,
    @SerialName("has_ended") val hasEnded: Boolean = false,
    @SerialName("xp_enabled") val xpEnabled: Boolean = true,
    val quotas: List<ChallengeQuotaDto> = emptyList(),
    @SerialName("next_quota") val nextQuota: ChallengeNextQuotaDto? = null,
    val me: ChallengeMeDto = ChallengeMeDto(),
    val participants: ChallengeParticipantsDto = ChallengeParticipantsDto(),
)

@Serializable
data class ChallengeQuotaDto(
    @SerialName("units_required") val unitsRequired: Int,
    @SerialName("reward_xp") val rewardXp: Int = 0,
    val title: String? = null,
    val reached: Boolean = false,
    val earned: Boolean = false,
)

@Serializable
data class ChallengeNextQuotaDto(
    @SerialName("units_required") val unitsRequired: Int,
    val remaining: Int = 0,
    @SerialName("reward_xp") val rewardXp: Int = 0,
    val title: String? = null,
    @SerialName("in_stretch") val inStretch: Boolean = false,
)

@Serializable
data class ChallengeMeDto(
    val units: Int = 0,
    val joined: Boolean = false,
    val hidden: Boolean = false,
)

@Serializable
data class ChallengeParticipantsDto(
    val count: Int = 0,
    val names: List<ChallengeParticipantDto> = emptyList(),
    val more: Int = 0,
)

@Serializable
data class ChallengeParticipantDto(
    val name: String,
    @SerialName("is_me") val isMe: Boolean = false,
)

@Serializable
data class AchievementDto(
    val code: String,
    val title: String,
    val description: String = "",
    val xp: Int = 0,
    @SerialName("earned_at") val earnedAt: String? = null,
    val challenge: String? = null,
)

/** O que um pedido somou no desafio, para a tela do pedido. */
@Serializable
data class OrderChallengeDto(
    val challenge: String,
    val units: Int = 0,
    @SerialName("units_total") val unitsTotal: Int = 0,
    val goal: Int = 1,
    @SerialName("unit_label") val unitLabel: String = "unidades",
)

@Serializable
data class ChallengeVisibilityRequest(val hidden: Boolean)
