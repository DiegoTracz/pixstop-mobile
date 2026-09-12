package com.pixstop.mobile.domain.model

/**
 * A ronda de quem repõe geladeiras (Fase 16).
 *
 * O que serve no carro é uma lista: empresa, porta, produto, quanto resta —
 * e, antes de tudo, quais portas estão fora do ar, porque chegar na empresa e
 * achar a geladeira muda é a pior viagem.
 */
data class OperatorRound(
    val operatorName: String,
    val offline: List<OperatorAlert>,
    val stock: List<OperatorStockRow>,
    val checkedAt: String?,
) {
    val lowCount: Int get() = stock.count { it.low }
}

data class OperatorAlert(
    val companyName: String,
    val companyId: String,
    val appliance: String?,
    val detail: String?,
)

data class OperatorStockRow(
    val companyName: String,
    val companyId: String,
    val appliance: String,
    val product: String,
    val quantity: Int,
    val low: Boolean,
)

/**
 * As empresas do operador.
 *
 * `seesMoney` decide o que a tela pode mostrar; os números de dinheiro vêm
 * nulos para o repositor porque o servidor não os envia.
 */
data class OperatorCompanies(
    val operatorName: String,
    val seesMoney: Boolean,
    val companies: List<OperatorCompany>,
)

data class OperatorCompany(
    val id: String,
    val name: String,
    val appliances: Int,
    val offline: Int,
    val lowStock: Int,
    val revenueToday: Double?,
    val revenueMonth: Double?,
    val profitMonth: Double?,
)
