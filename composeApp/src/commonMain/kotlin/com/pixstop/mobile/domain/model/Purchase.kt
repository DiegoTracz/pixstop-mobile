package com.pixstop.mobile.domain.model

/**
 * Uma compra (docs/plans/COMPRAS_MOBILE.md, passos 2 a 5).
 */
data class PurchaseSheet(
    val id: Long,
    val label: String,
    val status: PurchaseStatus,
    val editable: Boolean,
    val reading: Boolean,
    val supplierName: String,
    val documentNumber: String,
    val issuedAt: String,
    val total: Double?,
    val accessKey: String,
    val applianceId: Long?,
    val lines: List<PurchaseLine>,
    val products: List<CatalogProduct>,
    val appliances: List<Named>,
    val duplicate: Named?,
    val receiptUrl: String?,
    val readingError: String?,
    val warnings: List<String>,
    /** A pergunta do confirmar (COMPRAS_CORREDOR.md, P0). Nula fora do rascunho. */
    val destinationOptions: DestinationOptions? = null,
)

/**
 * Para onde vai a mercadoria. "Encher" põe na geladeira o que cabe no
 * planograma e guarda o resto; só existe para geladeira com planograma.
 */
enum class Destination(val api: String) {
    Fill("fill"),
    Appliance("appliance"),
    Warehouse("warehouse"),
    ;

    companion object {
        fun fromApi(value: String?): Destination = entries.firstOrNull { it.api == value } ?: Warehouse
    }
}

data class DestinationOptions(
    val default: Destination,
    val applianceId: Long?,
    val warehouseInUse: Boolean,
    val fill: List<FillPreview>,
)

/** A prévia do "encher a geladeira": quanto vai para a porta e quanto fica. */
data class FillPreview(val applianceId: Long, val toAppliance: Int, val toWarehouse: Int)

enum class PurchaseStatus {
    Draft, Reading, Confirmed, Discarded, Reversed;

    companion object {
        fun fromApi(value: String): PurchaseStatus = when (value) {
            "reading" -> Reading
            "confirmed" -> Confirmed
            "discarded" -> Discarded
            "reversed" -> Reversed
            else -> Draft
        }
    }
}

/**
 * Uma linha da nota: como veio impressa (`raw*`) e o que a pessoa conferiu.
 * `quantityUnits` é sempre em unidade de prateleira: duas caixas de seis são
 * doze latas, e `unitCost` é o custo de **uma lata**.
 */
data class PurchaseLine(
    val rawDescription: String? = null,
    val rawCode: String? = null,
    val rawQuantity: Double? = null,
    val rawUnit: String? = null,
    val rawUnitPrice: Double? = null,
    val rawDiscount: Double? = null,
    val rawTotal: Double? = null,
    val productId: Long? = null,
    val productName: String? = null,
    val ignored: Boolean = false,
    val quantityUnits: Int = 0,
    val packSize: Int = 1,
    val unitCost: Double? = null,
    /** barcode, alias, ai ou manual. */
    val matchSource: String? = null,
    val confidence: Double? = null,
    val expectedQuantity: Int? = null,
    /** Validade do lote, AAAA-MM-DD; só vale para produto que controla validade. */
    val expiresOn: String? = null,
)

data class CatalogProduct(
    val id: Long,
    val name: String,
    val barcode: String?,
    val priceCost: Double,
    val stock: Int,
    val tracksExpiry: Boolean = false,
    val shelfLifeDays: Int? = null,
    /** A foto, para reconhecer o produto ao conferir a nota. */
    val imageUrl: String? = null,
)

data class PurchaseSummary(
    val id: Long,
    val label: String,
    val date: String?,
    val status: PurchaseStatus,
    val statusLabel: String,
    val total: Double,
    val items: Int,
)

data class PurchaseIndex(val purchases: List<PurchaseSummary>, val canReadReceipts: Boolean)
