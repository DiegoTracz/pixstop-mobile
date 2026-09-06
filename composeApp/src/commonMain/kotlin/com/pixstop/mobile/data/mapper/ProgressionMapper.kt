package com.pixstop.mobile.data.mapper

import com.pixstop.mobile.data.remote.dto.OrderXpDto
import com.pixstop.mobile.data.remote.dto.ProgressionDto
import com.pixstop.mobile.data.remote.dto.ProgressionLevelDto
import com.pixstop.mobile.data.remote.dto.XpEventDto
import com.pixstop.mobile.domain.model.OrderXp
import com.pixstop.mobile.domain.model.Progression
import com.pixstop.mobile.domain.model.ProgressionLevel
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
    )
}

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
