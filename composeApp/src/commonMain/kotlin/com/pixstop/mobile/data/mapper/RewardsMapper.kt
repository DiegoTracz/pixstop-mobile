package com.pixstop.mobile.data.mapper

import com.pixstop.mobile.data.remote.dto.OrderRewardsDto
import com.pixstop.mobile.domain.model.LevelReward
import com.pixstop.mobile.domain.model.MedalReward
import com.pixstop.mobile.domain.model.OrderRewards
import com.pixstop.mobile.domain.model.QuotaReward

fun OrderRewardsDto.toDomain() = OrderRewards(
    cashbackPixels = cashbackPixels,
    level = level?.let { LevelReward(it.level, it.title, it.pixels) },
    quota = quota?.let { QuotaReward(it.challenge, it.unitsRequired, it.unitLabel, it.xp) },
    medals = medals.map { MedalReward(it.code, it.title, it.xp) },
)
