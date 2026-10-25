package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A lista de compras (docs/plans/COMPRAS.md, L1 e L2, no servidor).
 *
 * O app não recalcula nada: giro, cobertura e folga vêm prontos. A
 * quantidade que a pessoa edita vive só na tela e no texto compartilhado.
 */
@Serializable
data class ShoppingListPageDto(
    val appliances: List<NamedDto> = emptyList(),
    val appliance: NamedDto? = null,
    val company: String? = null,
    /** 0 = domingo … 6 = sábado; nulo = não avisar. */
    val weekday: Int? = null,
    val list: ShoppingListDto,
)

@Serializable
data class NamedDto(val id: Long, val name: String)

@Serializable
data class ShoppingListDto(
    val rows: List<ShoppingRowDto> = emptyList(),
    val covered: List<ShoppingRowDto> = emptyList(),
    @SerialName("cover_days") val coverDays: Int = 7,
    @SerialName("safety_margin") val safetyMargin: Double = 0.2,
    @SerialName("selling_days_per_week") val sellingDaysPerWeek: Double = 5.0,
    @SerialName("total_cost") val totalCost: Double = 0.0,
)

@Serializable
data class ShoppingRowDto(
    @SerialName("product_id") val productId: Long,
    val name: String,
    val category: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val stock: Int = 0,
    @SerialName("weekly_rate") val weeklyRate: Double = 0.0,
    @SerialName("zeroed_since") val zeroedSince: String? = null,
    val suggested: Int = 0,
    @SerialName("unit_cost") val unitCost: Double = 0.0,
)

/** O que comprar para todas as empresas do operador (COMPRAS.md, L3). */
@Serializable
data class OperatorShoppingDto(
    val lines: List<OperatorShoppingLineDto> = emptyList(),
    /** Nulo para o repositor: ele não vê dinheiro. */
    @SerialName("total_cost") val totalCost: Double? = null,
)

@Serializable
data class OperatorShoppingLineDto(
    val key: String,
    val name: String,
    val suggested: Int = 0,
    val zeroed: Int = 0,
    @SerialName("estimated_cost") val estimatedCost: Double? = null,
    val companies: List<OperatorShoppingCompanyDto> = emptyList(),
)

@Serializable
data class OperatorShoppingCompanyDto(
    val tenant: String,
    @SerialName("tenant_id") val tenantId: String,
    val suggested: Int = 0,
    val stock: Int = 0,
    @SerialName("zeroed_since") val zeroedSince: String? = null,
)
