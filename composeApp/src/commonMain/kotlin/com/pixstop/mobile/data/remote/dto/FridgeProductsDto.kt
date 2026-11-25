package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Os produtos de uma geladeira (APP_OPERADOR_PRODUTOS.md, G2). */
@Serializable
data class FridgeProductsDto(
    val appliance: NamedApplianceDto,
    @SerialName("in_appliance") val inAppliance: List<FridgeProductDto> = emptyList(),
    val others: List<FridgeProductDto> = emptyList(),
)

@Serializable
data class NamedApplianceDto(val id: Long, val name: String)

@Serializable
data class FridgeProductDto(
    val id: Long,
    val name: String,
    val category: String? = null,
    val barcode: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val price: Double = 0.0,
    val quantity: Int = 0,
    @SerialName("in_warehouse") val inWarehouse: Int = 0,
    @SerialName("low_stock_threshold") val lowStockThreshold: Int? = null,
)
