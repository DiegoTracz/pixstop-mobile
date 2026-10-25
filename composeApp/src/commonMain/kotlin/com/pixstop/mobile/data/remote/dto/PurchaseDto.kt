package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A compra da volta do mercado (docs/plans/COMPRAS.md, N0–N3, no servidor).
 *
 * O rascunho mora no servidor desde o envio da foto. A folha vai e volta
 * inteira a cada salvamento, com os campos crus da nota (`raw_*`): é o que a
 * confirmação usa para aprender o apelido de cada linha.
 */
@Serializable
data class PurchaseCreatedDto(val id: Long, val status: String)

@Serializable
data class PurchaseIndexDto(
    val purchases: PurchasePageDto,
    val canReadReceipts: Boolean = false,
)

@Serializable
data class PurchasePageDto(
    val data: List<PurchaseSummaryDto> = emptyList(),
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("last_page") val lastPage: Int = 1,
)

@Serializable
data class PurchaseSummaryDto(
    val id: Long,
    val label: String,
    @SerialName("issued_at") val issuedAt: String? = null,
    val status: String,
    @SerialName("status_label") val statusLabel: String = "",
    val total: Double? = null,
    @SerialName("items_total") val itemsTotal: Double = 0.0,
    val items: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class PurchaseSheetDto(
    val purchase: PurchaseDto,
    val destination: String = "Depósito",
    val duplicate: PurchaseTwinDto? = null,
    val products: List<CatalogProductDto> = emptyList(),
    val appliances: List<NamedDto> = emptyList(),
    val reading: Boolean = false,
    val receipt: ReceiptDto? = null,
    val ai: ReadingDto? = null,
)

@Serializable
data class PurchaseDto(
    val id: Long,
    val label: String,
    val status: String,
    @SerialName("status_label") val statusLabel: String = "",
    val source: String = "manual",
    val editable: Boolean = false,
    @SerialName("supplier_name") val supplierName: String? = null,
    @SerialName("issued_at") val issuedAt: String? = null,
    @SerialName("document_number") val documentNumber: String? = null,
    @SerialName("access_key") val accessKey: String? = null,
    val total: Double? = null,
    @SerialName("appliance_id") val applianceId: Long? = null,
    @SerialName("confirmed_at") val confirmedAt: String? = null,
    val items: List<PurchaseItemDto> = emptyList(),
)

@Serializable
data class PurchaseItemDto(
    @SerialName("line_no") val lineNo: Int = 0,
    @SerialName("raw_description") val rawDescription: String? = null,
    @SerialName("raw_code") val rawCode: String? = null,
    @SerialName("raw_quantity") val rawQuantity: Double? = null,
    @SerialName("raw_unit") val rawUnit: String? = null,
    @SerialName("raw_unit_price") val rawUnitPrice: Double? = null,
    @SerialName("raw_discount") val rawDiscount: Double? = null,
    @SerialName("raw_total") val rawTotal: Double? = null,
    @SerialName("product_id") val productId: Long? = null,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("quantity_units") val quantityUnits: Int = 0,
    @SerialName("pack_size") val packSize: Int = 1,
    @SerialName("unit_cost") val unitCost: Double? = null,
    val status: String = "unmatched",
    @SerialName("match_source") val matchSource: String? = null,
    val confidence: Double? = null,
    @SerialName("expected_quantity") val expectedQuantity: Int? = null,
)

@Serializable
data class PurchaseTwinDto(val id: Long, val label: String)

@Serializable
data class CatalogProductDto(
    val id: Long,
    val name: String,
    val barcode: String? = null,
    @SerialName("price_cost") val priceCost: Double = 0.0,
    val stock: Int = 0,
)

@Serializable
data class ReceiptDto(val url: String, val kind: String = "image")

@Serializable
data class ReadingDto(
    val error: String? = null,
    val warnings: List<String> = emptyList(),
    val model: String? = null,
)

/** A folha inteira, como o `PATCH` espera: ausente no cabeçalho não apaga. */
@Serializable
data class PurchaseSaveDto(
    @SerialName("supplier_name") val supplierName: String?,
    @SerialName("document_number") val documentNumber: String?,
    @SerialName("issued_at") val issuedAt: String?,
    val total: Double?,
    @SerialName("access_key") val accessKey: String?,
    @SerialName("appliance_id") val applianceId: Long?,
    val items: List<PurchaseLineSaveDto>,
)

@Serializable
data class PurchaseLineSaveDto(
    @SerialName("raw_description") val rawDescription: String?,
    @SerialName("raw_code") val rawCode: String?,
    @SerialName("raw_quantity") val rawQuantity: Double?,
    @SerialName("raw_unit") val rawUnit: String?,
    @SerialName("raw_unit_price") val rawUnitPrice: Double?,
    @SerialName("raw_discount") val rawDiscount: Double?,
    @SerialName("raw_total") val rawTotal: Double?,
    @SerialName("product_id") val productId: Long?,
    val ignored: Boolean,
    @SerialName("quantity_units") val quantityUnits: Int,
    @SerialName("pack_size") val packSize: Int,
    @SerialName("unit_cost") val unitCost: Double?,
    @SerialName("match_source") val matchSource: String?,
    val confidence: Double?,
    @SerialName("expected_quantity") val expectedQuantity: Int?,
)

/**
 * Abrir a compra a partir da lista (o "Vou comprar").
 *
 * `source` sem valor padrão de propósito: o JSON do app não envia o que é
 * igual ao padrão, e a compra chegaria ao servidor como "manual".
 */
@Serializable
data class PurchaseFromListDto(
    val source: String,
    val items: List<PurchaseFromListItemDto>,
)

@Serializable
data class PurchaseFromListItemDto(
    @SerialName("product_id") val productId: Long,
    @SerialName("quantity_units") val quantityUnits: Int,
    @SerialName("unit_cost") val unitCost: Double,
    @SerialName("expected_quantity") val expectedQuantity: Int,
)

@Serializable
data class PurchaseConfirmDto(@SerialName("allow_duplicate") val allowDuplicate: Boolean)
