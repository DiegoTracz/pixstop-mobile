package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * O que um pedido rendeu a quem comprou, para o app comemorar: cashback, o
 * nível alcançado, a cota do desafio que a compra fechou e as medalhas.
 * Tudo com padrão: um servidor antigo não manda o bloco, e nada toca.
 */
@Serializable
data class OrderRewardsDto(
    @SerialName("cashback_pixels") val cashbackPixels: Int = 0,
    val level: LevelRewardDto? = null,
    val quota: QuotaRewardDto? = null,
    val medals: List<MedalRewardDto> = emptyList(),
)

@Serializable
data class LevelRewardDto(val level: Int, val title: String? = null, val pixels: Int = 0)

@Serializable
data class QuotaRewardDto(
    val challenge: String = "",
    @SerialName("units_required") val unitsRequired: Int = 0,
    @SerialName("unit_label") val unitLabel: String = "unidades",
    val xp: Int = 0,
)

@Serializable
data class MedalRewardDto(val code: String, val title: String, val xp: Int = 0)
