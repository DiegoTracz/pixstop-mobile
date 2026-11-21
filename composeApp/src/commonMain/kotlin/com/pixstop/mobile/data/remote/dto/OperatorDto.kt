package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * O que o painel do operador recebe (Fase 16, O7 e O8).
 *
 * Dinheiro só aparece para quem o vê: o servidor simplesmente não envia
 * receita nem margem ao repositor, e por isso os campos são anuláveis aqui.
 */
@Serializable
data class OperatorRoundDto(
    val operator: OperatorRefDto,
    val offline: List<OperatorAlertDto> = emptyList(),
    val stock: List<OperatorStockRowDto> = emptyList(),
    @SerialName("checked_at") val checkedAt: String? = null,
)

@Serializable
data class OperatorRefDto(
    val id: Long,
    val name: String,
    @SerialName("sees_money") val seesMoney: Boolean = false,
)

@Serializable
data class OperatorAlertDto(
    val kind: String,
    val tenant: String,
    @SerialName("tenant_id") val tenantId: String,
    val appliance: String? = null,
    val detail: String? = null,
)

@Serializable
data class OperatorStockRowDto(
    val tenant: String,
    @SerialName("tenant_id") val tenantId: String,
    val appliance: String,
    val product: String,
    val quantity: Int = 0,
    val low: Boolean = false,
)

@Serializable
data class OperatorCompaniesDto(
    val operator: OperatorRefDto,
    val companies: List<OperatorCompanyDto> = emptyList(),
    @SerialName("checked_at") val checkedAt: String? = null,
)

@Serializable
data class OperatorCompanyDto(
    val id: String,
    val name: String,
    val appliances: Int = 0,
    val offline: Int = 0,
    @SerialName("low_stock") val lowStock: Int = 0,
    @SerialName("revenue_today") val revenueToday: Double? = null,
    @SerialName("revenue_month") val revenueMonth: Double? = null,
    @SerialName("profit_month") val profitMonth: Double? = null,
)

/** A visão geral do operador (PAINEL_ESTOQUE.md, D0). Os campos em R$ só vêm para o dono. */
@Serializable
data class OperatorInsightsDto(
    val operator: OperatorRefDto,
    val totals: OperatorTotalsDto = OperatorTotalsDto(),
    val attention: List<OperatorAttentionDto> = emptyList(),
    val companies: List<OperatorInsightCompanyDto> = emptyList(),
    @SerialName("sales_by_hour") val salesByHour: List<OperatorHourDto> = emptyList(),
    @SerialName("generated_at") val generatedAt: String? = null,
)

@Serializable
data class OperatorTotalsDto(
    val companies: Int = 0,
    val appliances: Int = 0,
    val offline: Int = 0,
    val attention: Int = 0,
    @SerialName("sold_today") val soldToday: Int = 0,
    @SerialName("revenue_today") val revenueToday: Double? = null,
    @SerialName("revenue_month") val revenueMonth: Double? = null,
    @SerialName("profit_month") val profitMonth: Double? = null,
    @SerialName("stock_value") val stockValue: Double? = null,
)

@Serializable
data class OperatorAttentionDto(
    val kind: String,
    val title: String,
    val detail: String? = null,
    val place: String? = null,
    val tenant: String,
    @SerialName("tenant_id") val tenantId: String,
    @SerialName("cost_per_day") val costPerDay: Double? = null,
)

@Serializable
data class OperatorInsightCompanyDto(
    val id: String,
    val name: String,
    val appliances: Int = 0,
    val offline: Int = 0,
    val attention: Int = 0,
    val zeroed: Int = 0,
    val expired: Int = 0,
    @SerialName("sold_today") val soldToday: Int = 0,
    @SerialName("revenue_today") val revenueToday: Double? = null,
    @SerialName("stock_value") val stockValue: Double? = null,
)

@Serializable
data class OperatorHourDto(
    val hour: Int,
    val today: Int = 0,
    val average: Double = 0.0,
)
