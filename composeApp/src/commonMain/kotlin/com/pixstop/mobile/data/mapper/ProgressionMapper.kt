package com.pixstop.mobile.data.mapper

import com.pixstop.mobile.data.remote.dto.OrderXpDto
import com.pixstop.mobile.data.remote.dto.PreviousSeasonDto
import com.pixstop.mobile.data.remote.dto.ProgressionCampaignDto
import com.pixstop.mobile.data.remote.dto.ProgressionDto
import com.pixstop.mobile.data.remote.dto.ProgressionLevelDto
import com.pixstop.mobile.data.remote.dto.ProgressionMissionDto
import com.pixstop.mobile.data.remote.dto.XpEventDto
import com.pixstop.mobile.domain.model.OrderXp
import com.pixstop.mobile.domain.model.PreviousSeason
import com.pixstop.mobile.domain.model.Progression
import com.pixstop.mobile.domain.model.ProgressionCampaign
import com.pixstop.mobile.domain.model.ProgressionLevel
import com.pixstop.mobile.domain.model.ProgressionMission
import com.pixstop.mobile.domain.model.XpEntry

/** Progressão desligada vira `null`: a tela não desenha barra nenhuma. */
fun ProgressionDto.toDomain(): Progression? {
    if (!enabled) {
        return null
    }

    return Progression(
        season = season,
        seasonEndsAt = seasonEndsAt,
        xpTotal = xpTotal,
        level = level,
        title = title,
        xpIntoLevel = xpIntoLevel,
        xpToNext = xpToNext,
        progressPercent = progressPercent,
        nextLevel = nextLevel?.toDomain(),
        streakDays = streakDays,
        levels = levels.map { it.toDomain() },
        missions = missions.map { it.toDomain() },
        campaign = campaign?.toDomain(),
        previousSeason = previousSeason?.toDomain(),
    )
}

fun ProgressionMissionDto.toDomain() = ProgressionMission(
    code = code,
    label = label,
    xp = xp,
    progress = progress,
    target = target,
    done = done,
)

fun ProgressionCampaignDto.toDomain() = ProgressionCampaign(name = name, multiplier = multiplier, endsAt = endsAt)

fun PreviousSeasonDto.toDomain() = PreviousSeason(season = season, level = level, title = title, xpTotal = xpTotal)

fun ProgressionLevelDto.toDomain() = ProgressionLevel(
    level = level,
    xpRequired = xpRequired,
    rewardPixels = rewardPixels,
    title = title,
    reached = reached,
)

fun XpEventDto.toDomain() = XpEntry(
    id = id,
    ruleCode = ruleCode,
    xp = xp,
    description = description,
    level = level,
    grantedPixels = grantedPixels,
    createdAt = createdAt,
)

fun OrderXpDto.toDomain() = OrderXp(earned = earned, leveledUpTo = leveledUpTo)
