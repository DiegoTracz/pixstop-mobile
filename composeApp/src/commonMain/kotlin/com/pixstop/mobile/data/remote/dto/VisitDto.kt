package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A visita do repositor (docs/plans/CONCILIACAO_MOBILE.md, no servidor).
 *
 * A lista de contagem **não traz o saldo esperado**, e isso não é descuido:
 * contagem que mostra o esperado antes vira "confirmar", e confirmar não
 * concilia nada. O esperado só chega na resposta do envio — se um dia
 * aparecer aqui, o teste do repositório quebra de propósito.
 */
@Serializable
data class CountSheetDto(
    val appliance: CountSheetApplianceDto,
    @SerialName("client_id") val clientId: String,
    val products: List<CountSheetProductDto> = emptyList(),
    val warehouse: List<WarehouseProductDto> = emptyList(),
    @SerialName("last_count") val lastCount: LastCountDto? = null,
)

@Serializable
data class CountSheetApplianceDto(val id: Long, val name: String)

@Serializable
data class CountSheetProductDto(
    val id: Long,
    val name: String,
    val category: String? = null,
    val barcode: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
)

@Serializable
data class WarehouseProductDto(
    val id: Long,
    val name: String,
    val barcode: String? = null,
    @SerialName("in_warehouse") val inWarehouse: Int = 0,
    @SerialName("low_stock_threshold") val lowStockThreshold: Int? = null,
)

@Serializable
data class LastCountDto(
    val id: Long,
    val at: String,
    val by: String? = null,
    @SerialName("days_ago") val daysAgo: Int = 0,
)

/**
 * O envio da contagem. A **origem não vai aqui**: quem carimba `app` ou `web`
 * é o servidor, no endpoint que recebeu — um cliente dizendo de onde fala não
 * é evidência, e o documento precisa de evidência.
 */
@Serializable
data class CountSubmitDto(
    @SerialName("client_id") val clientId: String,
    @SerialName("app_version") val appVersion: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    val counts: List<CountEntryDto> = emptyList(),
    val skipped: List<Long> = emptyList(),
    val note: String? = null,
)

@Serializable
data class CountEntryDto(@SerialName("product_id") val productId: Long, val counted: Int)

@Serializable
data class CountResultDto(
    val id: Long,
    val at: String,
    @SerialName("counted_items") val countedItems: Int = 0,
    @SerialName("skipped_items") val skippedItems: Int = 0,
    @SerialName("difference_value") val differenceValue: Double = 0.0,
    @SerialName("missing_value") val missingValue: Double = 0.0,
    val alert: Boolean = false,
    val items: List<CountResultItemDto> = emptyList(),
)

@Serializable
data class CountResultItemDto(
    @SerialName("product_id") val productId: Long,
    val product: String? = null,
    val expected: Int = 0,
    val counted: Int? = null,
    val difference: Int = 0,
    @SerialName("difference_value") val differenceValue: Double = 0.0,
    @SerialName("suggested_reason") val suggestedReason: String? = null,
)

@Serializable
data class CountItemUpdateDto(
    @SerialName("product_id") val productId: Long,
    @SerialName("suggested_reason") val suggestedReason: String? = null,
    val recounted: Boolean = false,
)
