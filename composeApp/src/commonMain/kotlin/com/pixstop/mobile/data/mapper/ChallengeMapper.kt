package com.pixstop.mobile.data.mapper

import com.pixstop.mobile.data.remote.dto.AchievementDto
import com.pixstop.mobile.data.remote.dto.ChallengeDto
import com.pixstop.mobile.data.remote.dto.ChallengePageDto
import com.pixstop.mobile.data.remote.dto.OrderChallengeDto
import com.pixstop.mobile.domain.model.Achievement
import com.pixstop.mobile.domain.model.Challenge
import com.pixstop.mobile.domain.model.ChallengeNextQuota
import com.pixstop.mobile.domain.model.ChallengePage
import com.pixstop.mobile.domain.model.ChallengeParticipant
import com.pixstop.mobile.domain.model.ChallengeQuota
import com.pixstop.mobile.domain.model.OrderChallenge

fun ChallengePageDto.toDomain() = ChallengePage(
    challenge = challenge?.toDomain(),
    achievements = achievements.map { it.toDomain() },
)

fun ChallengeDto.toDomain() = Challenge(
    id = id,
    name = name,
    description = description,
    unitLabel = unitLabel,
    unitsTotal = unitsTotal,
    goal = goal.coerceAtLeast(1),
    endsAt = endsAt,
    hasEnded = hasEnded,
    xpEnabled = xpEnabled,
    quotas = quotas.map { ChallengeQuota(it.unitsRequired, it.rewardXp, it.title, it.reached, it.earned) },
    nextQuota = nextQuota?.let { ChallengeNextQuota(it.unitsRequired, it.remaining, it.rewardXp, it.title, it.inStretch) },
    myUnits = me.units,
    isHidden = me.hidden,
    participantsCount = participants.count,
    participantNames = participants.names.map { ChallengeParticipant(it.name, it.isMe) },
    moreParticipants = participants.more,
)

fun AchievementDto.toDomain() = Achievement(
    code = code,
    title = title,
    description = description,
    xp = xp,
    earnedAt = earnedAt,
    challenge = challenge,
)

fun OrderChallengeDto.toDomain() = OrderChallenge(
    challenge = challenge,
    units = units,
    unitsTotal = unitsTotal,
    goal = goal.coerceAtLeast(1),
    unitLabel = unitLabel,
)
