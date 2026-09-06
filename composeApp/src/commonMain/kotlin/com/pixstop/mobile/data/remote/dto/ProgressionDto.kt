package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Onde a pessoa está na progressão por XP, como o servidor descreve.
 *
 * `enabled = false` quer dizer que a empresa não tem progressão ligada; o
 * resto dos campos então não tem significado.
 */
@Serializable
data class ProgressionDto(
    val enabled: Boolean = false,
    val season: Int = 1,
    @SerialName("season_ends_at") val seasonEndsAt: String? = null,
    @SerialName("xp_total") val xpTotal: Int = 0,
    val level: Int = 0,
    val title: String? = null,
    @SerialName("xp_into_level") val xpIntoLevel: Int = 0,
    @SerialName("xp_to_next") val xpToNext: Int = 0,
    @SerialName("progress_percent") val progressPercent: Int = 0,
    @SerialName("next_level") val nextLevel: ProgressionLevelDto? = null,
    @SerialName("streak_days") val streakDays: Int = 0,
    @SerialName("last_earned_on") val lastEarnedOn: String? = null,
    val levels: List<ProgressionLevelDto> = emptyList(),
    val missions: List<ProgressionMissionDto> = emptyList(),
    val campaign: ProgressionCampaignDto? = null,
    @SerialName("previous_season") val previousSeason: PreviousSeasonDto? = null,
)

/** Uma missão da temporada e o quanto já andou. */
@Serializable
data class ProgressionMissionDto(
    val code: String,
    val label: String,
    val xp: Int = 0,
    val progress: Int = 0,
    val target: Int = 1,
    val done: Boolean = false,
)

/** A campanha da plataforma valendo agora: XP multiplicado até uma data. */
@Serializable
data class ProgressionCampaignDto(
    val name: String,
    val multiplier: Double = 1.0,
    @SerialName("ends_at") val endsAt: String? = null,
)

/** Onde a pessoa parou na temporada passada. */
@Serializable
data class PreviousSeasonDto(
    val season: Int,
    val level: Int = 0,
    val title: String? = null,
    @SerialName("xp_total") val xpTotal: Int = 0,
)

@Serializable
data class ProgressionLevelDto(
    val level: Int,
    @SerialName("xp_required") val xpRequired: Int = 0,
    @SerialName("reward_pixels") val rewardPixels: Int = 0,
    val title: String? = null,
    val reached: Boolean = false,
)

@Serializable
data class XpEventDto(
    val id: Long,
    @SerialName("rule_code") val ruleCode: String,
    val xp: Int = 0,
    val description: String? = null,
    val level: Int? = null,
    @SerialName("granted_pixels") val grantedPixels: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

/** O que um pedido rendeu de XP, para a tela dizer na hora. */
@Serializable
data class OrderXpDto(
    val earned: Int = 0,
    @SerialName("leveled_up_to") val leveledUpTo: Int? = null,
)
