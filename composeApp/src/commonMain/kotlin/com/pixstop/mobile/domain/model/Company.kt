package com.pixstop.mobile.domain.model

/**
 * Resumo da empresa: vendas, pessoas e as reservas de pixels e saldo.
 */
data class CompanyDashboard(
    val periodDays: Int,
    val orders: Int,
    val revenue: Double,
    val fees: Double,
    val netRevenue: Double,
    val profit: Double,
    val pixelsRedeemed: Int,
    val topProducts: List<TopProduct>,
    val lowStock: List<LowStockProduct>,
    val membersTotal: Int,
    val membersActive: Int,
    val corporatePixels: Int,
    val departmentPixels: Int,
    val companyBalance: Double,
)

data class TopProduct(val name: String, val quantity: Int, val revenue: Double)

data class LowStockProduct(val id: Long, val name: String, val stock: Int)

/**
 * Um pedido como o administrador da empresa o vê: quem comprou e o que sobra
 * para a empresa depois das taxas.
 */
data class CompanyOrder(
    val id: Long,
    val transactionId: String?,
    val buyerName: String?,
    val status: OrderStatus,
    val statusLabel: String,
    val paymentMethodLabel: String?,
    val itemsCount: Int,
    val totalMoney: Double,
    val totalBalance: Double,
    val totalPixels: Int,
    val netRevenue: Double,
    val createdAt: String?,
) {
    /** Só faz sentido agir sobre o que ainda não foi encerrado. */
    val isOpen: Boolean get() = status == OrderStatus.Pending || status == OrderStatus.InReview
}

data class CompanyMember(
    val id: Long,
    val name: String,
    val email: String?,
    val role: CompanyRole,
    val isActive: Boolean,
    val balance: Double,
    val pixelAvailable: Int,
)
