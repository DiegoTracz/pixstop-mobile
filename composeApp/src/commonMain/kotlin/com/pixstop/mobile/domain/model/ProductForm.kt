package com.pixstop.mobile.domain.model

/** Cadastrar produto pelo app (APP_OPERADOR_PRODUTOS.md, G4). */
data class ProductCategory(val id: Long, val name: String)

/** O que a foto mostrou. O custo nunca vem daqui: só a pessoa sabe quanto pagou. */
data class PhotoReading(
    val name: String?,
    val description: String?,
    val category: ProductCategory?,
    val suggestedCategory: String?,
    val estimatedPrice: Double?,
    val similar: List<SimilarProduct>,
    val catalog: List<CatalogSuggestion>,
)

/** Um produto da empresa que parece o mesmo item. */
data class SimilarProduct(val id: Long, val name: String, val price: Double, val totalStock: Int)

/** Um item do catálogo da plataforma, com foto e descrição prontas. */
data class CatalogSuggestion(
    val id: Long,
    val name: String,
    val category: String?,
    val hasPhoto: Boolean,
    val imageUrl: String? = null,
)

/** Os parecidos e o catálogo para um nome, antes de qualquer foto. */
data class ProductSuggestions(val similar: List<SimilarProduct>, val catalog: List<CatalogSuggestion>)

/**
 * Como a IA deve deixar a foto (APP_OPERADOR_PRODUTOS.md, G6): o fundo
 * limpo da vitrine ou um cenário de propaganda. O rótulo nunca muda.
 */
data class PhotoStyle(
    val background: PhotoBackground = PhotoBackground.Transparent,
    val scene: PhotoScene = PhotoScene.Gelado,
    val sceneHint: String = "",
)

enum class PhotoBackground(val api: String, val label: String) {
    Transparent("transparent", "Sem fundo"),
    White("white", "Fundo branco"),
    Scene("scene", "Cenário"),
}

enum class PhotoScene(val api: String, val label: String) {
    Gelado("gelado", "Gelado"),
    Lanche("lanche", "Lanche"),
    Escritorio("escritorio", "Escritório"),
    Vitrine("vitrine", "Vitrine"),
}

/** O que vai para o servidor. */
data class NewProduct(
    val name: String,
    val categoryId: Long?,
    val description: String?,
    val barcode: String?,
    val price: Double,
    val cost: Double,
    val initialStock: Int,
    val catalogProductId: Long?,
)

/** Os produtos de uma geladeira: o que mora nela e o resto do cadastro (G2). */
data class FridgeProducts(
    val applianceId: Long,
    val applianceName: String,
    val inAppliance: List<FridgeProduct>,
    val others: List<FridgeProduct>,
)

data class FridgeProduct(
    val id: Long,
    val name: String,
    val category: String?,
    val barcode: String?,
    val imageUrl: String?,
    val price: Double,
    /** Quanto há nesta geladeira agora. */
    val quantity: Int,
    /** Quanto há no depósito, que é o teto do que dá para pôr. */
    val inWarehouse: Int,
    val lowStockThreshold: Int?,
)

data class CreatedProduct(
    val id: Long,
    val name: String,
    val imageUrl: String?,
    val price: Double,
    val inWarehouse: Int,
    val cost: Double = 0.0,
)
