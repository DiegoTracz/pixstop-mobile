package com.pixstop.mobile.domain.model

/**
 * A lista de compras da semana (docs/plans/COMPRAS_MOBILE.md, passo 1).
 *
 * Vem pronta do servidor; o app só deixa a pessoa mexer na quantidade antes
 * de compartilhar ou de abrir a compra.
 */
data class ShoppingList(
    val company: String,
    val appliances: List<Named>,
    val applianceId: Long?,
    val weekday: Int?,
    val rows: List<ShoppingRow>,
    val covered: List<ShoppingRow>,
    val coverDays: Int,
    val safetyPercent: Int,
    val sellingDaysPerWeek: Double,
)

data class Named(val id: Long, val name: String)

data class ShoppingRow(
    val productId: Long,
    val name: String,
    val category: String?,
    val imageUrl: String?,
    val stock: Int,
    val weeklyRate: Double,
    /** `AAAA-MM-DD` de quando zerou; nulo se ainda tem. */
    val zeroedSince: String?,
    val suggested: Int,
    val unitCost: Double,
)

/** Uma linha da lista do operador: o mesmo item somado entre as empresas. */
data class OperatorShoppingLine(
    val key: String,
    val name: String,
    val suggested: Int,
    val zeroed: Int,
    /** Nulo para quem não vê dinheiro. */
    val estimatedCost: Double?,
    val companies: List<OperatorShoppingCompany>,
)

data class OperatorShoppingCompany(val name: String, val suggested: Int, val stock: Int)

data class OperatorShopping(val lines: List<OperatorShoppingLine>, val totalCost: Double?)
