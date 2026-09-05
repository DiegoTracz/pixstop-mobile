package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.core.storage.SessionStore
import com.pixstop.mobile.data.repository.OrderRepository
import com.pixstop.mobile.data.repository.ShopRepository
import com.pixstop.mobile.domain.model.Category
import com.pixstop.mobile.domain.model.Order
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.PixelWallet
import com.pixstop.mobile.domain.model.Product
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeFeedUiState(
    val wallet: PixelWallet = PixelWallet.Empty,
    /** Pedido que ainda espera algo de quem comprou: pagar ou retirar. */
    val openOrder: Order? = null,
    val buyAgain: List<Product> = emptyList(),
    val promotions: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
) {
    val hasContent: Boolean
        get() = buyAgain.isNotEmpty() || promotions.isNotEmpty() || categories.isNotEmpty()
}

/**
 * O conteúdo da tela de início.
 *
 * Numa despensa de empresa quase toda compra é repetição — a mesma água, o
 * mesmo café, todo dia. Por isso "comprar de novo" vem antes de qualquer
 * vitrine: o caminho mais curto é o que a pessoa já percorreu.
 */
class HomeFeedViewModel(
    private val shop: ShopRepository,
    private val orders: OrderRepository,
    private val session: SessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeFeedUiState())
    val uiState: StateFlow<HomeFeedUiState> = _uiState.asStateFlow()

    init {
        if (session.isLoggedIn()) {
            load()
        }
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) {
            return
        }

        load(isRefresh = true)
    }

    private fun load(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = !isRefresh && !_uiState.value.hasContent,
                isRefreshing = isRefresh,
                error = null,
            )

            // As quatro chamadas não dependem umas das outras; em série a tela
            // levaria quatro idas e voltas para aparecer.
            coroutineScope {
                val wallet = async { shop.pixelWallet() }
                val recentOrders = async { orders.orders() }
                val promotions = async { shop.products(perPage = PROMO_LIMIT, onlyDiscounted = true) }
                val categories = async { shop.categories() }

                val walletResult = wallet.await()
                val ordersResult = recentOrders.await()
                val promotionsResult = promotions.await()
                val categoriesResult = categories.await()

                val openOrder = (ordersResult as? Outcome.Success)?.value?.items?.firstOrNull { it.isPending }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    wallet = (walletResult as? Outcome.Success)?.value ?: _uiState.value.wallet,
                    openOrder = openOrder,
                    promotions = (promotionsResult as? Outcome.Success)?.value?.items.orEmpty(),
                    categories = (categoriesResult as? Outcome.Success)?.value
                        ?.filter { it.productsCount > 0 }
                        .orEmpty(),
                    // Só é erro quando nada carregou: uma seção vazia é um
                    // detalhe, a tela inteira em branco é um problema.
                    error = (walletResult as? Outcome.Failure)?.error?.message
                        ?.takeIf { promotionsResult is Outcome.Failure && categoriesResult is Outcome.Failure },
                )

                val boughtBefore = (ordersResult as? Outcome.Success)?.value?.items.orEmpty()
                loadBuyAgain(boughtBefore)
            }
        }
    }

    /**
     * Os produtos das últimas compras, relidos da vitrine.
     *
     * O pedido guarda o preço da época; o que a tela precisa mostrar é o preço
     * e o estoque de hoje. Produto que saiu de linha some da lista sozinho,
     * porque a vitrine deixa de devolvê-lo.
     */
    private suspend fun loadBuyAgain(recentOrders: List<Order>) {
        val productIds = buyAgainIdsFrom(recentOrders)

        if (productIds.isEmpty()) {
            _uiState.value = _uiState.value.copy(buyAgain = emptyList())
            return
        }

        val products = coroutineScope {
            productIds.map { id -> async { shop.product(id) } }.awaitAll()
        }

        _uiState.value = _uiState.value.copy(
            buyAgain = products.filterIsInstance<Outcome.Success<Product>>().map { it.value },
        )
    }

    companion object {
        /** Poucos e recentes: a lista é um atalho, não um catálogo. */
        const val BUY_AGAIN_LIMIT = 6
        const val PROMO_LIMIT = 10
    }
}

/**
 * Quais produtos entram no "comprar de novo".
 *
 * Os pedidos chegam do mais recente para o mais antigo, e a ordem é preservada
 * — o que foi comprado ontem interessa mais do que o do mês passado. Repetido
 * conta uma vez só: a prateleira é um atalho, e o mesmo produto duas vezes
 * ocuparia o lugar de outro.
 */
fun buyAgainIdsFrom(
    orders: List<Order>,
    limit: Int = HomeFeedViewModel.BUY_AGAIN_LIMIT,
): List<Long> = orders
    .flatMap { it.items }
    .mapNotNull { it.productId }
    .distinct()
    .take(limit)
