package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.core.network.safeCallPaged
import com.pixstop.mobile.data.mapper.toDomain
import com.pixstop.mobile.data.remote.dto.ApplianceChoiceDto
import com.pixstop.mobile.data.remote.dto.CategoryDto
import com.pixstop.mobile.data.remote.dto.Page
import com.pixstop.mobile.data.remote.dto.PixelBalanceDto
import com.pixstop.mobile.data.remote.dto.ProductDto
import com.pixstop.mobile.data.remote.dto.WalletTopupDto
import com.pixstop.mobile.data.remote.dto.WalletTopupRequest
import com.pixstop.mobile.domain.model.ApplianceChoice
import com.pixstop.mobile.domain.model.Category
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.PixelWallet
import com.pixstop.mobile.domain.model.Product
import com.pixstop.mobile.domain.model.TopupMethod
import com.pixstop.mobile.domain.model.WalletTopup
import com.pixstop.mobile.domain.model.map
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody

private const val TAG = "Shop"

/**
 * Vitrine da empresa.
 *
 * A busca e o filtro de categoria são do servidor, não do app: a lista é
 * paginada, e filtrar só o que já chegou esconderia o resto do catálogo.
 */
class ShopRepository(private val client: HttpClient) {

    suspend fun categories(): Outcome<List<Category>> =
        safeCall<List<CategoryDto>>(TAG) {
            client.get(ApiConfig.Endpoints.SHOP_CATEGORIES)
        }.map { list -> list.map { it.toDomain() } }

    suspend fun products(
        page: Int = 1,
        query: String? = null,
        categoryId: Long? = null,
        perPage: Int = 20,
        onlyDiscounted: Boolean = false,
    ): Outcome<Page<Product>> =
        safeCallPaged<ProductDto>(TAG) {
            client.get(ApiConfig.Endpoints.SHOP_PRODUCTS) {
                parameter("page", page)
                parameter("per_page", perPage)
                query?.takeIf { it.isNotBlank() }?.let { parameter("q", it.trim()) }
                categoryId?.let { parameter("category", it) }
                if (onlyDiscounted) parameter("promo", 1)
            }
        }.map { result -> Page(result.items.map { it.toDomain() }, result.meta) }

    suspend fun product(id: Long): Outcome<Product> =
        safeCall<ProductDto>(TAG) { client.get(ApiConfig.Endpoints.shopProduct(id)) }.map { it.toDomain() }

    /**
     * De qual geladeira é a compra (Fase 9.5).
     *
     * Com uma porta só, `mustChoose` vem falso e nenhuma tela muda. Com mais
     * de uma, a pessoa precisa dizer em frente a qual está antes de comprar:
     * o estoque e a porta que abre são de uma geladeira, não da empresa.
     */
    suspend fun appliances(): Outcome<ApplianceChoice> =
        safeCall<ApplianceChoiceDto>(TAG) {
            client.get(ApiConfig.Endpoints.SHOP_APPLIANCES)
        }.map { it.toDomain() }

    /**
     * Compra pixels para a carteira. Pix volta pendente, com o código; cartão
     * aprovado já volta creditado. O dinheiro vai para quem vende na empresa.
     */
    suspend fun buyPixels(
        reais: Int,
        method: TopupMethod,
        cardToken: String? = null,
        documentType: String? = null,
        documentNumber: String? = null,
    ): Outcome<WalletTopup> =
        safeCall<WalletTopupDto>(TAG) {
            client.post(ApiConfig.Endpoints.PIXELS_TOPUPS) {
                setBody(WalletTopupRequest(reais, method.apiValue, cardToken, documentType, documentNumber))
            }
        }.map { it.toDomain() }

    /** Em que pé está a compra; o servidor pergunta ao gateway e credita se já caiu. */
    suspend fun pixelTopup(id: Long): Outcome<WalletTopup> =
        safeCall<WalletTopupDto>(TAG) { client.get(ApiConfig.Endpoints.pixelTopup(id)) }.map { it.toDomain() }

    suspend fun pixelWallet(): Outcome<PixelWallet> =
        safeCall<PixelBalanceDto>(TAG) { client.get(ApiConfig.Endpoints.PIXELS_BALANCE) }.map { it.toDomain() }
}
