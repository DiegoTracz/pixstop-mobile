package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.core.network.safeCallUnit
import com.pixstop.mobile.data.remote.dto.ExpiryOverviewDto
import com.pixstop.mobile.data.remote.dto.ExpiryRemoveDto
import com.pixstop.mobile.data.remote.dto.ProductExpiryDto
import com.pixstop.mobile.data.remote.dto.ProductExpiryListDto
import com.pixstop.mobile.domain.model.ExpiryLot
import com.pixstop.mobile.domain.model.ExpiryOverview
import com.pixstop.mobile.domain.model.ExpiryPlace
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.ProductExpiry
import com.pixstop.mobile.domain.model.UndatedLot
import com.pixstop.mobile.domain.model.map
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val TAG = "Expiry"

/**
 * A validade no app (VALIDADE.md, V5 c): a mesma tela da web, e a retirada
 * dos vencidos de um lugar.
 */
class ExpiryRepository(private val client: HttpClient) {

    suspend fun overview(): Outcome<ExpiryOverview> =
        safeCall<ExpiryOverviewDto>(TAG) { client.get(ApiConfig.Endpoints.COMPANY_EXPIRY) }
            .map { dto ->
                ExpiryOverview(
                    places = dto.places.map { place ->
                        ExpiryPlace(
                            applianceId = place.applianceId,
                            name = place.name,
                            expiredUnits = place.expiredUnits,
                            lots = place.lots.map {
                                ExpiryLot(it.productId, it.product, it.expiresOn, it.quantity, it.daysLeft, it.expired, it.warning)
                            },
                            undated = place.undated.map { UndatedLot(it.productId, it.product, it.quantity) },
                        )
                    },
                    expiredUnits = dto.summary.expiredUnits,
                    warningUnits = dto.summary.warningUnits,
                    undatedUnits = dto.summary.undatedUnits,
                )
            }

    /** Os produtos ativos com a validade de cada um (V5 d). */
    suspend fun products(): Outcome<List<ProductExpiry>> =
        safeCall<ProductExpiryListDto>(TAG) { client.get(ApiConfig.Endpoints.COMPANY_PRODUCTS_EXPIRY) }
            .map { dto -> dto.products.map { it.toDomain() } }

    /**
     * Grava a validade de um produto. `tracksExpiry` nulo volta a herdar da
     * categoria, e por isso vai escrito no corpo: o JSON do app omite nulos.
     */
    suspend fun update(id: Long, tracksExpiry: Boolean?, shelfLifeDays: Int?, warningDays: Int?): Outcome<ProductExpiry> =
        safeCall<ProductExpiryDto>(TAG) {
            client.put(ApiConfig.Endpoints.companyProductExpiry(id)) {
                setBody(
                    buildJsonObject {
                        put("tracks_expiry", tracksExpiry?.let { JsonPrimitive(it) } ?: JsonNull)
                        put("shelf_life_days", shelfLifeDays?.let { JsonPrimitive(it) } ?: JsonNull)
                        put("expiry_warning_days", warningDays?.let { JsonPrimitive(it) } ?: JsonNull)
                    },
                )
            }
        }.map { it.toDomain() }

    /** Dá baixa, com motivo "vencido", só das unidades de lote vencido do lugar. */
    suspend fun removeExpired(applianceId: Long?): Outcome<Unit> =
        safeCallUnit(TAG) {
            client.post(ApiConfig.Endpoints.COMPANY_EXPIRY_REMOVE) {
                setBody(ExpiryRemoveDto(applianceId))
            }
        }
}

private fun ProductExpiryDto.toDomain() = ProductExpiry(id, name, category, categoryTracks, tracksExpiry, tracks, shelfLifeDays, warningDays)
