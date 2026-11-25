package com.pixstop.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Cadastrar produto pelo app (APP_OPERADOR_PRODUTOS.md, G4). */
@Serializable
data class ProductCategoryDto(
    val id: Long,
    val name: String,
)

/** O que a foto mostrou: o formulário quase preenchido. */
@Serializable
data class PhotoReadingDto(
    val name: String? = null,
    val description: String? = null,
    val category: ProductCategoryDto? = null,
    @SerialName("suggested_category") val suggestedCategory: String? = null,
    @SerialName("estimated_price") val estimatedPrice: Double? = null,
    @SerialName("similar_products") val similarProducts: List<SimilarProductDto> = emptyList(),
    @SerialName("catalog_matches") val catalogMatches: List<CatalogMatchDto> = emptyList(),
)

@Serializable
data class SimilarProductDto(
    @SerialName("product_id") val productId: Long,
    val name: String,
    val price: Double = 0.0,
    @SerialName("total_stock") val totalStock: Int = 0,
)

@Serializable
data class CatalogMatchDto(
    @SerialName("catalog_product_id") val catalogProductId: Long,
    val name: String,
    val category: String? = null,
    @SerialName("has_photo") val hasPhoto: Boolean = false,
    @SerialName("image_url") val imageUrl: String? = null,
)

/** Os parecidos e o catálogo para um nome, sem foto (o item da nota). */
@Serializable
data class ProductSuggestionsDto(
    @SerialName("similar_products") val similarProducts: List<SimilarProductDto> = emptyList(),
    @SerialName("catalog_matches") val catalogMatches: List<CatalogMatchDto> = emptyList(),
)

/** A prévia da foto melhorada, como data URI. */
@Serializable
data class EnhancedPhotoDto(
    val preview: String,
    val model: String? = null,
    val seconds: Double? = null,
)

@Serializable
data class CreatedProductDto(
    val id: Long,
    val name: String,
    @SerialName("image_url") val imageUrl: String? = null,
    val price: Double = 0.0,
    val cost: Double = 0.0,
    @SerialName("in_warehouse") val inWarehouse: Int = 0,
)

/** Vários produtos numa foto (G4.1): a lista para revisar. */
@Serializable
data class BatchReadingDto(
    val products: List<BatchItemDto> = emptyList(),
    val truncated: Boolean = false,
    val limit: Int = 12,
)

@Serializable
data class BatchItemDto(
    val name: String,
    val description: String? = null,
    val category: ProductCategoryDto? = null,
    @SerialName("estimated_price") val estimatedPrice: Double? = null,
    val quantity: Int = 1,
    /** O recorte do produto na foto, como data URI. */
    val photo: String? = null,
    @SerialName("similar_products") val similarProducts: List<SimilarProductDto> = emptyList(),
)
