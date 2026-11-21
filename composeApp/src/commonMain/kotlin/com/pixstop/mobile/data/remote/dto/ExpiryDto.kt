package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A tela "Validade" do servidor, em JSON (VALIDADE.md, V5 c). */
@Serializable
data class ExpiryOverviewDto(
    val places: List<ExpiryPlaceDto> = emptyList(),
    val summary: ExpirySummaryDto = ExpirySummaryDto(),
)

@Serializable
data class ExpiryPlaceDto(
    @SerialName("appliance_id") val applianceId: Long? = null,
    val name: String = "",
    @SerialName("expired_units") val expiredUnits: Int = 0,
    val lots: List<ExpiryLotDto> = emptyList(),
    val undated: List<UndatedLotDto> = emptyList(),
)

@Serializable
data class ExpiryLotDto(
    @SerialName("product_id") val productId: Long,
    val product: String = "",
    @SerialName("expires_on") val expiresOn: String = "",
    val quantity: Int = 0,
    @SerialName("days_left") val daysLeft: Int = 0,
    val expired: Boolean = false,
    val warning: Boolean = false,
)

@Serializable
data class UndatedLotDto(
    @SerialName("product_id") val productId: Long,
    val product: String = "",
    val quantity: Int = 0,
)

@Serializable
data class ExpirySummaryDto(
    @SerialName("expired_units") val expiredUnits: Int = 0,
    @SerialName("warning_units") val warningUnits: Int = 0,
    @SerialName("undated_units") val undatedUnits: Int = 0,
)

@Serializable
data class ProductExpiryListDto(val products: List<ProductExpiryDto> = emptyList())

@Serializable
data class ProductExpiryDto(
    val id: Long,
    val name: String = "",
    val category: String? = null,
    @SerialName("category_tracks") val categoryTracks: Boolean = false,
    @SerialName("tracks_expiry") val tracksExpiry: Boolean? = null,
    val tracks: Boolean = false,
    @SerialName("shelf_life_days") val shelfLifeDays: Int? = null,
    @SerialName("expiry_warning_days") val warningDays: Int? = null,
)

/** "Retirar vencidos" de um lugar; sem geladeira é o depósito. */
@Serializable
data class ExpiryRemoveDto(@SerialName("appliance_id") val applianceId: Long? = null)
