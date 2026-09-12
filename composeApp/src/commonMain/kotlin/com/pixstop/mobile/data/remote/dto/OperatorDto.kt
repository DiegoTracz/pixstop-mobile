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
