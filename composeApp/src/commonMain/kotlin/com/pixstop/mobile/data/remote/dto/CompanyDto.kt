package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Resumo da empresa no período.
 */
@Serializable
data class CompanyDashboardDto(
    @SerialName("period_days") val periodDays: Int = 30,
    val sales: CompanySalesDto = CompanySalesDto(),
    @SerialName("top_products") val topProducts: List<TopProductDto> = emptyList(),
    @SerialName("low_stock") val lowStock: List<LowStockDto> = emptyList(),
    val members: CompanyMembersDto = CompanyMembersDto(),
    val wallet: CompanyWalletDto = CompanyWalletDto(),
)

@Serializable
data class CompanySalesDto(
    val orders: Int = 0,
    val revenue: Double = 0.0,
    val fees: Double = 0.0,
    @SerialName("net_revenue") val netRevenue: Double = 0.0,
    val profit: Double = 0.0,
    val pixels: Int = 0,
)

@Serializable
data class TopProductDto(val product: String, val quantity: Int = 0, val revenue: Double = 0.0)

@Serializable
data class LowStockDto(val id: Long, val name: String, val stock: Int = 0)

@Serializable
data class CompanyMembersDto(val total: Int = 0, val active: Int = 0)

/**
 * As três reservas da empresa: pixels no cofre, pixels já nos times e o saldo
 * em reais.
 */
@Serializable
data class CompanyWalletDto(
    @SerialName("corporate_pixels") val corporatePixels: Int = 0,
    @SerialName("department_pixels") val departmentPixels: Int = 0,
    @SerialName("company_balance") val companyBalance: Double = 0.0,
)

@Serializable
data class CompanyOrderDto(
    val id: Long,
    @SerialName("transaction_id") val transactionId: String? = null,
    @SerialName("user_name") val userName: String? = null,
    val status: String,
    @SerialName("status_label") val statusLabel: String? = null,
    @SerialName("payment_method_label") val paymentMethodLabel: String? = null,
    @SerialName("items_count") val itemsCount: Int = 0,
    @SerialName("total_money") val totalMoney: Double = 0.0,
    @SerialName("total_balance") val totalBalance: Double = 0.0,
    @SerialName("total_pixels") val totalPixels: Int = 0,
    @SerialName("net_revenue") val netRevenue: Double = 0.0,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class CompanyUserDto(
    val id: Long,
    val name: String,
    val email: String? = null,
    val role: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    val balance: Double = 0.0,
    @SerialName("pixel_available") val pixelAvailable: Int = 0,
)

@Serializable
data class OrderActionRequest(val action: String, val reason: String)

@Serializable
data class BalanceRequest(
    @SerialName("user_ids") val userIds: List<Long>,
    val action: String,
    val amount: Double,
    val reason: String? = null,
)

@Serializable
data class AllocateRequest(val amount: Int, val reason: String? = null)

@Serializable
data class ToggleRequest(@SerialName("return_pixels") val returnPixels: Boolean = true)
