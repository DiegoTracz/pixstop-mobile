package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeBytes
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.core.network.safeCallUnit
import com.pixstop.mobile.data.remote.dto.PurchaseConfirmDto
import com.pixstop.mobile.data.remote.dto.PurchaseCreatedDto
import com.pixstop.mobile.data.remote.dto.PurchaseIndexDto
import com.pixstop.mobile.data.remote.dto.PurchaseLineSaveDto
import com.pixstop.mobile.data.remote.dto.PurchaseSaveDto
import com.pixstop.mobile.data.remote.dto.PurchaseSheetDto
import com.pixstop.mobile.domain.model.CatalogProduct
import com.pixstop.mobile.domain.model.Named
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.PurchaseIndex
import com.pixstop.mobile.domain.model.PurchaseLine
import com.pixstop.mobile.domain.model.PurchaseSheet
import com.pixstop.mobile.domain.model.PurchaseStatus
import com.pixstop.mobile.domain.model.PurchaseSummary
import com.pixstop.mobile.domain.model.map
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

private const val TAG = "Purchase"

/**
 * A compra da volta do mercado (docs/plans/COMPRAS_MOBILE.md, A1).
 *
 * O rascunho mora no servidor desde o envio da foto: nada aqui guarda estado
 * no aparelho. A folha vai inteira a cada salvamento, com os campos crus da
 * nota de volta — é com eles que a confirmação aprende o apelido de cada linha.
 */
class PurchaseRepository(private val client: HttpClient) {

    /** A foto da nota. Responde na hora com o id; a IA lê na fila do servidor. */
    suspend fun uploadReceipt(jpeg: ByteArray): Outcome<Long> =
        safeCall<PurchaseCreatedDto>(TAG) {
            client.post(ApiConfig.Endpoints.COMPANY_PURCHASE_RECEIPT_UPLOAD) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                key = "receipt",
                                value = jpeg,
                                headers = Headers.build {
                                    append(HttpHeaders.ContentType, ContentType.Image.JPEG.toString())
                                    // O Laravel valida pela extensão: sem nome, recusa antes de olhar.
                                    append(HttpHeaders.ContentDisposition, "filename=\"nota.jpg\"")
                                },
                            )
                        },
                    ),
                )
            }
        }.map { it.id }

    suspend fun index(): Outcome<PurchaseIndex> =
        safeCall<PurchaseIndexDto>(TAG) {
            client.get(ApiConfig.Endpoints.COMPANY_PURCHASES)
        }.map { dto ->
            PurchaseIndex(
                purchases = dto.purchases.data.map {
                    PurchaseSummary(
                        id = it.id,
                        label = it.label,
                        date = it.issuedAt ?: it.createdAt?.take(10),
                        status = PurchaseStatus.fromApi(it.status),
                        statusLabel = it.statusLabel,
                        total = it.total ?: it.itemsTotal,
                        items = it.items,
                    )
                },
                canReadReceipts = dto.canReadReceipts,
            )
        }

    suspend fun show(id: Long): Outcome<PurchaseSheet> =
        safeCall<PurchaseSheetDto>(TAG) {
            client.get(ApiConfig.Endpoints.companyPurchase(id))
        }.map { dto ->
            val purchase = dto.purchase
            PurchaseSheet(
                id = purchase.id,
                label = purchase.label,
                status = PurchaseStatus.fromApi(purchase.status),
                editable = purchase.editable,
                reading = dto.reading,
                supplierName = purchase.supplierName.orEmpty(),
                documentNumber = purchase.documentNumber.orEmpty(),
                issuedAt = purchase.issuedAt.orEmpty(),
                total = purchase.total,
                accessKey = purchase.accessKey.orEmpty(),
                applianceId = purchase.applianceId,
                lines = purchase.items.map {
                    PurchaseLine(
                        rawDescription = it.rawDescription,
                        rawCode = it.rawCode,
                        rawQuantity = it.rawQuantity,
                        rawUnit = it.rawUnit,
                        rawUnitPrice = it.rawUnitPrice,
                        rawDiscount = it.rawDiscount,
                        rawTotal = it.rawTotal,
                        productId = it.productId,
                        productName = it.productName,
                        ignored = it.status == "ignored",
                        quantityUnits = it.quantityUnits,
                        packSize = it.packSize,
                        unitCost = it.unitCost,
                        matchSource = it.matchSource,
                        confidence = it.confidence,
                        expectedQuantity = it.expectedQuantity,
                    )
                },
                products = dto.products.map { CatalogProduct(it.id, it.name, it.barcode, it.priceCost, it.stock) },
                appliances = dto.appliances.map { Named(it.id, it.name) },
                duplicate = dto.duplicate?.let { Named(it.id, it.label) },
                receiptUrl = dto.receipt?.url,
                readingError = dto.ai?.error,
                warnings = dto.ai?.warnings.orEmpty(),
            )
        }

    suspend fun save(sheet: PurchaseSheet): Outcome<Unit> =
        safeCallUnit(TAG) {
            client.patch(ApiConfig.Endpoints.companyPurchase(sheet.id)) {
                setBody(
                    PurchaseSaveDto(
                        supplierName = sheet.supplierName.ifBlank { null },
                        documentNumber = sheet.documentNumber.ifBlank { null },
                        issuedAt = sheet.issuedAt.ifBlank { null },
                        total = sheet.total,
                        accessKey = sheet.accessKey.ifBlank { null },
                        applianceId = sheet.applianceId,
                        items = sheet.lines.map {
                            PurchaseLineSaveDto(
                                rawDescription = it.rawDescription,
                                rawCode = it.rawCode,
                                rawQuantity = it.rawQuantity,
                                rawUnit = it.rawUnit,
                                rawUnitPrice = it.rawUnitPrice,
                                rawDiscount = it.rawDiscount,
                                rawTotal = it.rawTotal,
                                productId = if (it.ignored) null else it.productId,
                                ignored = it.ignored,
                                quantityUnits = it.quantityUnits,
                                packSize = it.packSize,
                                unitCost = it.unitCost,
                                matchSource = it.matchSource,
                                confidence = it.confidence,
                                expectedQuantity = it.expectedQuantity,
                            )
                        },
                    ),
                )
            }
        }

    suspend fun confirm(id: Long, allowDuplicate: Boolean = false): Outcome<Unit> =
        safeCallUnit(TAG) {
            client.post(ApiConfig.Endpoints.companyPurchaseConfirm(id)) {
                setBody(PurchaseConfirmDto(allowDuplicate))
            }
        }

    suspend fun discard(id: Long): Outcome<Unit> =
        safeCallUnit(TAG) { client.post(ApiConfig.Endpoints.companyPurchaseDiscard(id)) }

    /** A nota original, para conferir lado a lado com o que a IA leu. */
    suspend fun receipt(id: Long): Outcome<ByteArray> =
        safeBytes(TAG) { client.get(ApiConfig.Endpoints.companyPurchaseReceipt(id)) }
}
