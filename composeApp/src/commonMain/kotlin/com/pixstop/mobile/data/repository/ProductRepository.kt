package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.data.remote.dto.CreatedProductDto
import com.pixstop.mobile.data.remote.dto.FridgeProductDto
import com.pixstop.mobile.data.remote.dto.FridgeProductsDto
import com.pixstop.mobile.domain.model.FridgeProduct
import com.pixstop.mobile.domain.model.FridgeProducts
import com.pixstop.mobile.data.remote.dto.PhotoReadingDto
import com.pixstop.mobile.data.remote.dto.ProductCategoryDto
import com.pixstop.mobile.domain.model.BatchItem
import com.pixstop.mobile.domain.model.BatchReading
import com.pixstop.mobile.domain.model.CatalogSuggestion
import com.pixstop.mobile.domain.model.CreatedProduct
import com.pixstop.mobile.domain.model.NewProduct
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.PhotoReading
import com.pixstop.mobile.domain.model.ProductCategory
import com.pixstop.mobile.domain.model.SimilarProduct
import com.pixstop.mobile.domain.model.map
import com.pixstop.mobile.data.remote.dto.BatchReadingDto
import com.pixstop.mobile.data.remote.dto.CatalogMatchDto
import com.pixstop.mobile.data.remote.dto.EnhancedPhotoDto
import com.pixstop.mobile.data.remote.dto.ProductSuggestionsDto
import com.pixstop.mobile.domain.model.PhotoBackground
import com.pixstop.mobile.domain.model.PhotoStyle
import com.pixstop.mobile.domain.model.ProductSuggestions
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.request.parameter
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

private const val TAG = "Product"

/**
 * Cadastrar produto pelo app (APP_OPERADOR_PRODUTOS.md, G4).
 *
 * Ler a foto e cadastrar são duas chamadas de propósito: a leitura só
 * sugere, e nada é criado antes de a pessoa dizer o custo e conferir o resto.
 */
class ProductRepository(private val client: HttpClient) {

    suspend fun categories(): Outcome<List<ProductCategory>> =
        safeCall<List<ProductCategoryDto>>(TAG) {
            client.get(ApiConfig.Endpoints.COMPANY_CATEGORIES)
        }.map { list -> list.map { ProductCategory(it.id, it.name) } }

    /** Os produtos de uma geladeira, com o saldo de cada lugar (G2). */
    suspend fun applianceProducts(applianceId: Long): Outcome<FridgeProducts> =
        safeCall<FridgeProductsDto>(TAG) {
            client.get(ApiConfig.Endpoints.companyApplianceProducts(applianceId))
        }.map { dto ->
            FridgeProducts(
                applianceId = dto.appliance.id,
                applianceName = dto.appliance.name,
                inAppliance = dto.inAppliance.map { it.toDomain() },
                others = dto.others.map { it.toDomain() },
            )
        }

    suspend fun readPhoto(jpeg: ByteArray): Outcome<PhotoReading> =
        safeCall<PhotoReadingDto>(TAG) {
            client.post(ApiConfig.Endpoints.COMPANY_PRODUCT_PHOTO) {
                setBody(MultiPartFormDataContent(formData { appendPhoto(jpeg) }))
            }
        }.map { dto ->
            PhotoReading(
                name = dto.name,
                description = dto.description,
                category = dto.category?.let { ProductCategory(it.id, it.name) },
                suggestedCategory = dto.suggestedCategory,
                estimatedPrice = dto.estimatedPrice,
                similar = dto.similarProducts.map { SimilarProduct(it.productId, it.name, it.price, it.totalStock) },
                catalog = dto.catalogMatches.map { it.toDomain() },
            )
        }

    /**
     * Vários produtos numa foto (G4.1). A IA olha a foto em alta resolução:
     * leva mais que a foto de um produto só.
     */
    suspend fun analyzeBatch(jpeg: ByteArray): Outcome<BatchReading> =
        safeCall<BatchReadingDto>(TAG) {
            client.post(ApiConfig.Endpoints.COMPANY_PRODUCT_BATCH) {
                timeout { requestTimeoutMillis = 120_000; socketTimeoutMillis = 120_000 }
                setBody(MultiPartFormDataContent(formData { appendPhoto(jpeg) }))
            }
        }.map { dto ->
            BatchReading(
                items = dto.products.map { item ->
                    BatchItem(
                        name = item.name,
                        description = item.description,
                        category = item.category?.let { ProductCategory(it.id, it.name) },
                        estimatedPrice = item.estimatedPrice,
                        quantity = item.quantity,
                        photo = item.photo?.let { runCatching { decodeDataUri(it) }.getOrNull() },
                        similar = item.similarProducts.map { SimilarProduct(it.productId, it.name, it.price, it.totalStock) },
                    )
                },
                truncated = dto.truncated,
                limit = dto.limit,
            )
        }

    /** O que já existe para o nome do item da nota: parecidos e catálogo com foto. */
    suspend fun suggestions(name: String, barcode: String?): Outcome<ProductSuggestions> =
        safeCall<ProductSuggestionsDto>(TAG) {
            client.get(ApiConfig.Endpoints.COMPANY_PRODUCT_SUGGESTIONS) {
                parameter("name", name)
                barcode?.takeIf { it.isNotBlank() }?.let { parameter("barcode", it) }
            }
        }.map { dto ->
            ProductSuggestions(
                similar = dto.similarProducts.map { SimilarProduct(it.productId, it.name, it.price, it.totalStock) },
                catalog = dto.catalogMatches.map { it.toDomain() },
            )
        }

    /**
     * A foto melhorada pela IA. Demora: o modelo leva de meio minuto a um
     * minuto, por isso o tempo desta chamada é o do avatar, e não o padrão.
     * Devolve os bytes do PNG da prévia; nada fica gravado no servidor.
     */
    suspend fun enhance(photo: ByteArray?, catalogProductId: Long?, style: PhotoStyle): Outcome<ByteArray> =
        safeCall<EnhancedPhotoDto>(TAG) {
            client.post(ApiConfig.Endpoints.COMPANY_PRODUCT_ENHANCE) {
                timeout { requestTimeoutMillis = 150_000; socketTimeoutMillis = 150_000 }
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("background", style.background.api)
                            if (style.background == PhotoBackground.Scene) {
                                append("scene", style.scene.api)
                                style.sceneHint.trim().takeIf { it.isNotEmpty() }?.let { append("scene_hint", it.take(200)) }
                            }
                            if (photo != null) appendPhoto(photo) else catalogProductId?.let { append("catalog_product_id", it.toString()) }
                        },
                    ),
                )
            }
        }.map { decodeDataUri(it.preview) }

    suspend fun create(product: NewProduct, photo: ByteArray?, originalPhoto: ByteArray? = null): Outcome<CreatedProduct> =
        safeCall<CreatedProductDto>(TAG) {
            client.post(ApiConfig.Endpoints.COMPANY_PRODUCTS) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("name", product.name)
                            append("price_money", product.price.toString())
                            append("price_cost", product.cost.toString())
                            append("stock_quantity", product.initialStock.toString())
                            product.categoryId?.let { append("category_id", it.toString()) }
                            product.description?.takeIf { it.isNotBlank() }?.let { append("description", it) }
                            product.barcode?.takeIf { it.isNotBlank() }?.let { append("barcode", it) }
                            product.catalogProductId?.let { append("central_product_id", it.toString()) }
                            // Quem decide qual foto vai é a tela: a do celular, a
                            // melhorada pela IA ou nenhuma, e aí vale a do catálogo.
                            photo?.let { appendPhoto(it) }
                            // A melhorada vai para a vitrine e a do celular fica guardada.
                            originalPhoto?.let { appendPhoto(it, field = "image_original") }
                        },
                    ),
                )
            }
        }.map { CreatedProduct(it.id, it.name, it.imageUrl, it.price, it.inWarehouse, it.cost) }
}

private fun FridgeProductDto.toDomain() = FridgeProduct(
    id = id,
    name = name,
    category = category,
    barcode = barcode,
    imageUrl = imageUrl,
    price = price,
    quantity = quantity,
    inWarehouse = inWarehouse,
    lowStockThreshold = lowStockThreshold,
)

private fun CatalogMatchDto.toDomain() = CatalogSuggestion(catalogProductId, name, category, hasPhoto, imageUrl)

/** "data:image/png;base64,AAAA" vira os bytes da imagem. */
@OptIn(ExperimentalEncodingApi::class)
internal fun decodeDataUri(uri: String): ByteArray = Base64.decode(uri.substringAfter("base64,"))

/** A foto melhorada volta em PNG; a do celular é JPEG. O nome tem de dizer qual. */
internal fun isPng(bytes: ByteArray): Boolean =
    bytes.size > 4 && bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() && bytes[2] == 'N'.code.toByte() && bytes[3] == 'G'.code.toByte()

/** O Laravel valida pela extensão: sem nome de arquivo, recusa antes de olhar. */
private fun FormBuilder.appendPhoto(bytes: ByteArray, field: String = "image") = append(
    key = field,
    value = bytes,
    headers = Headers.build {
        val png = isPng(bytes)
        append(HttpHeaders.ContentType, if (png) ContentType.Image.PNG.toString() else ContentType.Image.JPEG.toString())
        append(HttpHeaders.ContentDisposition, if (png) "filename=\"produto.png\"" else "filename=\"produto.jpg\"")
    },
)
