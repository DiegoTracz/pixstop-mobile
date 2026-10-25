package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.core.network.safeCallUnit
import com.pixstop.mobile.data.remote.dto.PurchaseCreatedDto
import com.pixstop.mobile.data.remote.dto.PurchaseFromListDto
import com.pixstop.mobile.data.remote.dto.PurchaseFromListItemDto
import com.pixstop.mobile.data.remote.dto.ShoppingListPageDto
import com.pixstop.mobile.data.remote.dto.ShoppingRowDto
import com.pixstop.mobile.domain.model.Named
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.ShoppingList
import com.pixstop.mobile.domain.model.ShoppingRow
import com.pixstop.mobile.domain.model.map
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.math.roundToInt

private const val TAG = "ShoppingList"

/**
 * A lista de compras da semana (docs/plans/COMPRAS_MOBILE.md, A2).
 */
class ShoppingListRepository(private val client: HttpClient) {

    suspend fun list(applianceId: Long? = null): Outcome<ShoppingList> =
        safeCall<ShoppingListPageDto>(TAG) {
            client.get(ApiConfig.Endpoints.COMPANY_SHOPPING_LIST) {
                applianceId?.let { parameter("appliance", it) }
            }
        }.map { dto ->
            ShoppingList(
                company = dto.company.orEmpty(),
                appliances = dto.appliances.map { Named(it.id, it.name) },
                applianceId = dto.appliance?.id,
                weekday = dto.weekday,
                rows = dto.list.rows.map(::row),
                covered = dto.list.covered.map(::row),
                coverDays = dto.list.coverDays,
                safetyPercent = (dto.list.safetyMargin * 100).roundToInt(),
                sellingDaysPerWeek = dto.list.sellingDaysPerWeek,
            )
        }

    /**
     * O ritmo da empresa. O corpo é montado à mão porque o JSON do app omite
     * nulos, e aqui `weekday` nulo quer dizer "não avisar" — omitido, o
     * servidor entenderia "não mexer no dia".
     */
    suspend fun saveSettings(coverDays: Int, safetyPercent: Int, weekday: Int?): Outcome<Unit> =
        safeCallUnit(TAG) {
            client.patch(ApiConfig.Endpoints.COMPANY_SHOPPING_LIST_SETTINGS) {
                setBody(
                    buildJsonObject {
                        put("cover_days", JsonPrimitive(coverDays))
                        put("safety_percent", JsonPrimitive(safetyPercent))
                        put("weekday", weekday?.let(::JsonPrimitive) ?: JsonNull)
                    },
                )
            }
        }

    /**
     * "Vou comprar": abre a compra com o que a lista pede, e cada linha
     * guarda a quantidade esperada para a conferência mostrar o que mudou.
     */
    suspend fun startPurchase(quantities: Map<ShoppingRow, Int>): Outcome<Long> =
        safeCall<PurchaseCreatedDto>(TAG) {
            client.post(ApiConfig.Endpoints.COMPANY_PURCHASES) {
                setBody(
                    PurchaseFromListDto(
                        source = "list",
                        items = quantities.filterValues { it > 0 }.map { (row, quantity) ->
                            PurchaseFromListItemDto(row.productId, quantity, row.unitCost, quantity)
                        },
                    ),
                )
            }
        }.map { it.id }

    private fun row(dto: ShoppingRowDto) = ShoppingRow(
        productId = dto.productId,
        name = dto.name,
        category = dto.category,
        imageUrl = dto.imageUrl,
        stock = dto.stock,
        weeklyRate = dto.weeklyRate,
        zeroedSince = dto.zeroedSince,
        suggested = dto.suggested,
        unitCost = dto.unitCost,
    )
}
