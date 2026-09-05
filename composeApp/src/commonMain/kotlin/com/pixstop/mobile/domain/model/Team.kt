package com.pixstop.mobile.domain.model

/**
 * Um lançamento no extrato de pixels.
 */
data class PixelEntry(
    val id: Long,
    val amount: Int,
    val balanceAfter: Int,
    val sourceLabel: String,
    val description: String?,
    val expiresAt: Long?,
    val createdAt: Long?,
) {
    val isCredit: Boolean get() = amount > 0
}

/**
 * Origem do lançamento.
 *
 * O rótulo é do app, não do servidor: o `source` é um código estável, e
 * traduzi-lo aqui evita depender de um texto que pode mudar do outro lado.
 * Origem desconhecida vira "Movimentação", que não promete nada de errado.
 */
enum class PixelSource(val apiValue: String, val label: String) {
    Cashback("purchase_cashback", "Cashback de compra"),
    OrderRedeem("order_redeem", "Desconto em pedido"),
    OrderRefund("order_refund", "Estorno de pedido"),
    CompanyDistribution("company_distribution", "Distribuição da empresa"),
    DepartmentDistribution("department_distribution", "Distribuição do time"),
    AdminAdjustment("admin_adjustment", "Ajuste do administrador"),
    Expiration("expiration", "Pixels expirados");

    companion object {
        fun labelFor(value: String?): String =
            entries.firstOrNull { it.apiValue == value }?.label ?: "Movimentação"
    }
}

/**
 * Departamento sob gestão, com a verba de pixels disponível.
 */
data class Department(
    val id: Long,
    val name: String,
    val description: String?,
    val pixelBalance: Int,
    val membersCount: Int,
    val isActive: Boolean,
)

data class TeamMember(
    val userId: Long,
    val name: String,
    val email: String?,
    val isManager: Boolean,
    val isActive: Boolean,
    val pixelAvailable: Int,
)
