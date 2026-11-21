package com.pixstop.mobile.domain.model

/**
 * A visão geral de quem opera: a tela inicial dele (PAINEL_ESTOQUE.md, D1).
 *
 * Todas as empresas numa tela. A lista de atenção já vem na ordem do
 * servidor: porta fora do ar, vencido na prateleira e então o que custa mais
 * por dia. Os campos em R$ chegam nulos para o repositor.
 */
data class OperatorInsights(
    val operatorName: String,
    val seesMoney: Boolean,
    val totals: OperatorTotals,
    val attention: List<OperatorAttention>,
    val companies: List<OperatorInsightCompany>,
    val salesByHour: List<HourSales>,
)

data class OperatorTotals(
    val companies: Int,
    val appliances: Int,
    val offline: Int,
    val attention: Int,
    val soldToday: Int,
    val revenueToday: Double?,
    val revenueMonth: Double?,
    val profitMonth: Double?,
    val stockValue: Double?,
)

data class OperatorAttention(
    val kind: String,
    val title: String,
    val detail: String?,
    val companyName: String,
    val companyId: String,
    val costPerDay: Double?,
)

data class OperatorInsightCompany(
    val id: String,
    val name: String,
    val appliances: Int,
    val offline: Int,
    val attention: Int,
    val zeroed: Int,
    val expired: Int,
    val soldToday: Int,
    val revenueToday: Double?,
)

data class HourSales(val hour: Int, val today: Int, val average: Double)
