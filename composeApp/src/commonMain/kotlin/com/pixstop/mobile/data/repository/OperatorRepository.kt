package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.data.remote.dto.OperatorCompaniesDto
import com.pixstop.mobile.data.remote.dto.OperatorRoundDto
import com.pixstop.mobile.data.remote.dto.OperatorShoppingDto
import com.pixstop.mobile.domain.model.OperatorAlert
import com.pixstop.mobile.domain.model.OperatorCompanies
import com.pixstop.mobile.domain.model.OperatorCompany
import com.pixstop.mobile.domain.model.OperatorRound
import com.pixstop.mobile.domain.model.OperatorShopping
import com.pixstop.mobile.domain.model.OperatorShoppingCompany
import com.pixstop.mobile.domain.model.OperatorShoppingLine
import com.pixstop.mobile.domain.model.OperatorStockRow
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.map
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter

private const val TAG = "Operator"

/**
 * O painel de quem opera geladeiras (Fase 16).
 *
 * Fora do grupo de empresa ativa de propósito: o repositor atravessa várias, e
 * pedir que ele escolha uma antes de ver a lista seria pedir que adivinhasse
 * por onde começar.
 */
class OperatorRepository(private val client: HttpClient) {

    /** `onlyLow` transforma a lista no roteiro do dia. */
    suspend fun round(onlyLow: Boolean = false): Outcome<OperatorRound> =
        safeCall<OperatorRoundDto>(TAG) {
            client.get(ApiConfig.Endpoints.OPERATOR_ROUND) {
                if (onlyLow) {
                    parameter("only_low", 1)
                }
            }
        }.map { dto ->
            OperatorRound(
                operatorName = dto.operator.name,
                offline = dto.offline.map { OperatorAlert(it.tenant, it.tenantId, it.appliance, it.detail) },
                stock = dto.stock.map {
                    OperatorStockRow(it.tenant, it.tenantId, it.appliance, it.product, it.quantity, it.low)
                },
                checkedAt = dto.checkedAt,
            )
        }

    suspend fun companies(): Outcome<OperatorCompanies> =
        safeCall<OperatorCompaniesDto>(TAG) {
            client.get(ApiConfig.Endpoints.OPERATOR_COMPANIES)
        }.map { dto ->
            OperatorCompanies(
                operatorName = dto.operator.name,
                seesMoney = dto.operator.seesMoney,
                companies = dto.companies.map {
                    OperatorCompany(
                        id = it.id,
                        name = it.name,
                        appliances = it.appliances,
                        offline = it.offline,
                        lowStock = it.lowStock,
                        revenueToday = it.revenueToday,
                        revenueMonth = it.revenueMonth,
                        profitMonth = it.profitMonth,
                    )
                },
            )
        }

    /**
     * O que comprar para todas as empresas, numa ida só ao atacado
     * (docs/plans/COMPRAS_MOBILE.md, A3). O custo só vem para o dono.
     */
    suspend fun shoppingList(): Outcome<OperatorShopping> =
        safeCall<OperatorShoppingDto>(TAG) {
            client.get(ApiConfig.Endpoints.OPERATOR_SHOPPING_LIST)
        }.map { dto ->
            OperatorShopping(
                lines = dto.lines.map { line ->
                    OperatorShoppingLine(
                        key = line.key,
                        name = line.name,
                        suggested = line.suggested,
                        zeroed = line.zeroed,
                        estimatedCost = line.estimatedCost,
                        companies = line.companies.map { OperatorShoppingCompany(it.tenant, it.suggested, it.stock) },
                    )
                },
                totalCost = dto.totalCost,
            )
        }
}
