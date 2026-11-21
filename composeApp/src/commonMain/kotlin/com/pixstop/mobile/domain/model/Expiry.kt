package com.pixstop.mobile.domain.model

/**
 * A validade da empresa por lugar (docs/plans/VALIDADE.md, V5 c): o depósito
 * e cada geladeira, os lotes com data do que vence antes ao que vence
 * depois, e o que ainda está "sem data". Só unidades: nada de R$ aqui.
 */
data class ExpiryOverview(
    val places: List<ExpiryPlace>,
    val expiredUnits: Int,
    val warningUnits: Int,
    val undatedUnits: Int,
)

data class ExpiryPlace(
    /** Nulo é o depósito. */
    val applianceId: Long?,
    val name: String,
    val expiredUnits: Int,
    val lots: List<ExpiryLot>,
    val undated: List<UndatedLot>,
)

data class ExpiryLot(
    val productId: Long,
    val product: String,
    /** AAAA-MM-DD: o último dia de venda. */
    val expiresOn: String,
    val quantity: Int,
    val daysLeft: Int,
    val expired: Boolean,
    /** Dentro da antecedência do aviso do produto. */
    val warning: Boolean,
)

data class UndatedLot(val productId: Long, val product: String, val quantity: Int)

/**
 * A validade de um produto (VALIDADE.md, V5 d). `tracksExpiry` nulo herda
 * da categoria; `tracks` é o que vale de fato.
 */
data class ProductExpiry(
    val id: Long,
    val name: String,
    val category: String?,
    val categoryTracks: Boolean,
    val tracksExpiry: Boolean?,
    val tracks: Boolean,
    val shelfLifeDays: Int?,
    val warningDays: Int?,
)
