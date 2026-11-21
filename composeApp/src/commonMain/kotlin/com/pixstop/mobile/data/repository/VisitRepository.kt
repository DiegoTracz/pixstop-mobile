package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.core.network.safeCallUnit
import com.pixstop.mobile.data.remote.dto.CountEntryDto
import com.pixstop.mobile.data.remote.dto.CountItemUpdateDto
import com.pixstop.mobile.data.remote.dto.CountResultDto
import com.pixstop.mobile.data.remote.dto.CountSheetDto
import com.pixstop.mobile.data.remote.dto.CountSubmitDto
import com.pixstop.mobile.data.remote.dto.RestockEntryDto
import com.pixstop.mobile.data.remote.dto.RestockResultDto
import com.pixstop.mobile.data.remote.dto.RestockSubmitDto
import com.pixstop.mobile.domain.model.CountProduct
import com.pixstop.mobile.domain.model.CountResult
import com.pixstop.mobile.domain.model.CountResultItem
import com.pixstop.mobile.domain.model.CountSheet
import com.pixstop.mobile.domain.model.LastCount
import com.pixstop.mobile.domain.model.LossSuggestion
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.RestockResult
import com.pixstop.mobile.domain.model.RestockSuggestion
import com.pixstop.mobile.domain.model.WarehouseProduct
import com.pixstop.mobile.domain.model.map
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

private const val TAG = "Visit"

/**
 * A visita do repositor: contar, ver as diferenças e sugerir um motivo.
 *
 * O `clientId` vem do servidor com a lista e viaja com o envio: reenviar
 * depois de uma queda de rede devolve o mesmo documento, sem ajustar o
 * estoque duas vezes. É o que permite guardar a contagem no aparelho sem
 * medo de mandar de novo.
 */
class VisitRepository(private val client: HttpClient) {

    suspend fun sheet(applianceId: Long): Outcome<CountSheet> =
        safeCall<CountSheetDto>(TAG) {
            client.get(ApiConfig.Endpoints.companyCountSheet(applianceId))
        }.map { dto ->
            CountSheet(
                applianceId = dto.appliance.id,
                applianceName = dto.appliance.name,
                clientId = dto.clientId,
                products = dto.products.map { CountProduct(it.id, it.name, it.category, it.barcode, it.imageUrl, it.hasExpired) },
                warehouse = dto.warehouse.map {
                    WarehouseProduct(it.id, it.name, it.barcode, it.inWarehouse, it.lowStockThreshold)
                },
                lastCount = dto.lastCount?.let { LastCount(it.id, it.at, it.by, it.daysAgo) },
            )
        }

    /**
     * Manda o que foi contado e recebe, pela primeira vez, o que o sistema
     * esperava. `skipped` não é detalhe: quem ficou sem contagem precisa
     * constar como não contado, porque cobertura é fato, não silêncio.
     */
    suspend fun submit(
        applianceId: Long,
        clientId: String,
        counts: Map<Long, Int>,
        skipped: List<Long>,
        appVersion: String?,
        startedAt: String?,
        note: String? = null,
    ): Outcome<CountResult> =
        safeCall<CountResultDto>(TAG) {
            client.post(ApiConfig.Endpoints.companyStockCounts(applianceId)) {
                setBody(
                    CountSubmitDto(
                        clientId = clientId,
                        appVersion = appVersion,
                        startedAt = startedAt,
                        counts = counts.map { (productId, counted) -> CountEntryDto(productId, counted) },
                        skipped = skipped,
                        note = note,
                    ),
                )
            }
        }.map { dto ->
            CountResult(
                id = dto.id,
                countedItems = dto.countedItems,
                skippedItems = dto.skippedItems,
                missingValue = dto.missingValue,
                alert = dto.alert,
                items = dto.items.map {
                    CountResultItem(
                        productId = it.productId,
                        product = it.product.orEmpty(),
                        expected = it.expected,
                        counted = it.counted,
                        difference = it.difference,
                        differenceValue = it.differenceValue,
                        suggestedReason = LossSuggestion.fromApi(it.suggestedReason),
                    )
                },
                restock = dto.restock.map { RestockSuggestion(it.productId, it.product, it.warehouse, it.suggested, it.place) },
                restockClientId = dto.restockClientId,
            )
        }

    /**
     * "Tirei os vencidos": o servidor dá baixa, com motivo "vencido", só das
     * unidades de lote vencido desta geladeira. Não devolve quantas: a
     * contagem continua às cegas.
     */
    suspend fun removeExpired(applianceId: Long): Outcome<Unit> =
        safeCallUnit(TAG) {
            client.post(ApiConfig.Endpoints.COMPANY_EXPIRY_REMOVE) {
                setBody(mapOf("appliance_id" to applianceId))
            }
        }

    /**
     * Leva do depósito para a geladeira. Só o que tem quantidade vai; o
     * servidor recusa tudo se faltar algum item, e nada se move pela metade.
     */
    suspend fun restock(applianceId: Long, clientId: String, quantities: Map<Long, Int>): Outcome<RestockResult> =
        safeCall<RestockResultDto>(TAG) {
            client.post(ApiConfig.Endpoints.companyRestocks(applianceId)) {
                setBody(
                    RestockSubmitDto(
                        clientId = clientId,
                        items = quantities.filterValues { it > 0 }.map { (productId, quantity) -> RestockEntryDto(productId, quantity) },
                    ),
                )
            }
        }.map { RestockResult(it.id, it.units) }

    /** O palpite de quem contou. Um toque, e o próximo toque desfaz. */
    suspend fun suggest(countId: Long, productId: Long, reason: LossSuggestion?): Outcome<CountItemUpdateDto> =
        safeCall(TAG) {
            client.patch(ApiConfig.Endpoints.companyStockCountItem(countId, productId)) {
                setBody(mapOf("suggested_reason" to reason?.api))
            }
        }

    /**
     * "Contei errado": o servidor estorna o ajuste daquela linha, apontando o
     * lançamento original. Nada é apagado — a trilha mostra o erro e o conserto.
     */
    suspend fun recount(countId: Long, productId: Long): Outcome<CountItemUpdateDto> =
        safeCall(TAG) {
            client.patch(ApiConfig.Endpoints.companyStockCountItem(countId, productId)) {
                setBody(mapOf("recount" to true))
            }
        }
}
