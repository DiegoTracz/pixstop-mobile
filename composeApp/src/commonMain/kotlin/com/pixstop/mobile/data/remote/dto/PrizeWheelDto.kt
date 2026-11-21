package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A roleta de prêmios, como o servidor descreve
 * (docs/plans/LANCAMENTO_LOCARMAIS.md no servidor). `wheel` nula quer dizer
 * que não há roleta girando agora.
 */
@Serializable
data class PrizeWheelPageDto(
    val wheel: PrizeWheelDto? = null,
)

@Serializable
data class PrizeWheelDto(
    val id: Long,
    val name: String,
    val message: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    val available: PrizeWheelAvailableDto = PrizeWheelAvailableDto(),
    @SerialName("guaranteed_jackpot") val guaranteedJackpot: Boolean = false,
    @SerialName("won_pixels") val wonPixels: Int = 0,
    @SerialName("purchase_min_total") val purchaseMinTotal: Double = 0.0,
    val slices: PrizeWheelSlicesDto = PrizeWheelSlicesDto(),
    val recent: List<PrizeWheelRecentDto> = emptyList(),
)

@Serializable
data class PrizeWheelAvailableDto(
    val gold: Int = 0,
    val purchase: Int = 0,
)

@Serializable
data class PrizeWheelSlicesDto(
    val gold: List<PrizeWheelSliceDto> = emptyList(),
    val purchase: List<PrizeWheelSliceDto> = emptyList(),
)

@Serializable
data class PrizeWheelSliceDto(
    val label: String,
    val pixels: Int = 0,
    @SerialName("is_jackpot") val isJackpot: Boolean = false,
    val weight: Double = 1.0,
    val chance: Double = 0.0,
)

@Serializable
data class PrizeWheelRecentDto(
    val kind: String = "gold",
    val label: String? = null,
    val pixels: Int = 0,
    @SerialName("is_jackpot") val isJackpot: Boolean = false,
    @SerialName("spun_at") val spunAt: String? = null,
)

/** O resultado de um giro: a animação para na fatia `slice_index`. */
@Serializable
data class PrizeSpinResultDto(
    val kind: String = "gold",
    @SerialName("slice_index") val sliceIndex: Int = 0,
    val label: String = "",
    val pixels: Int = 0,
    @SerialName("is_jackpot") val isJackpot: Boolean = false,
    val forced: Boolean = false,
    val state: PrizeWheelDto? = null,
)
