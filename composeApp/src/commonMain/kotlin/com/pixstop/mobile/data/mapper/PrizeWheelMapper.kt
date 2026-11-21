package com.pixstop.mobile.data.mapper

import com.pixstop.mobile.data.remote.dto.PrizeSpinResultDto
import com.pixstop.mobile.data.remote.dto.PrizeWheelDto
import com.pixstop.mobile.data.remote.dto.PrizeWheelSliceDto
import com.pixstop.mobile.domain.model.PrizeSpinKind
import com.pixstop.mobile.domain.model.PrizeSpinResult
import com.pixstop.mobile.domain.model.PrizeWheel
import com.pixstop.mobile.domain.model.PrizeWheelRecent
import com.pixstop.mobile.domain.model.PrizeWheelSlice

fun PrizeWheelDto.toDomain() = PrizeWheel(
    id = id,
    name = name,
    message = message,
    endsAt = endsAt,
    goldSpins = available.gold,
    purchaseSpins = available.purchase,
    guaranteedJackpot = guaranteedJackpot,
    wonPixels = wonPixels,
    purchaseMinTotal = purchaseMinTotal,
    goldSlices = slices.gold.map { it.toDomain() },
    purchaseSlices = slices.purchase.map { it.toDomain() },
    recent = recent.map { PrizeWheelRecent(PrizeSpinKind.fromApi(it.kind), it.label, it.pixels, it.isJackpot) },
)

fun PrizeWheelSliceDto.toDomain() = PrizeWheelSlice(label = label, pixels = pixels, isJackpot = isJackpot)

fun PrizeSpinResultDto.toDomain() = PrizeSpinResult(
    kind = PrizeSpinKind.fromApi(kind),
    sliceIndex = sliceIndex,
    label = label,
    pixels = pixels,
    isJackpot = isJackpot,
    forced = forced,
    wheel = state?.toDomain(),
)
