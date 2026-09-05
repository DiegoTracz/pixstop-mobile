package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.repository.ShopRepository
import com.pixstop.mobile.domain.model.Category
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.PixelWallet
import com.pixstop.mobile.domain.model.Product
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShopUiState(
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList(),
    val wallet: PixelWallet = PixelWallet.Empty,
    val query: String = "",
    val selectedCategory: Long? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val page: Int = 1,
    val hasNextPage: Boolean = false,
    val error: String? = null,
) {
    val isEmpty: Boolean get() = !isLoading && products.isEmpty() && error == null

    /** Filtro ativo muda o texto do vazio: "nada aqui" e "nada encontrado" não são a mesma coisa. */
    val isFiltered: Boolean get() = query.isNotBlank() || selectedCategory != null
}

/**
 * Vitrine.
 *
 * A busca e o filtro vão para o servidor porque a lista é paginada — filtrar
 * só o que já chegou esconderia o resto do catálogo.
 */
class ShopViewModel(private val shop: ShopRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ShopUiState())
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    /** Espera a pessoa parar de digitar antes de consultar o servidor. */
    private var searchJob: Job? = null

    init {
        loadCategories()
        loadWallet()
        loadProducts()
    }

    fun onQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(query = value)

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            loadProducts()
        }
    }

    fun onCategorySelected(id: Long?) {
        val current = _uiState.value.selectedCategory

        // Tocar na categoria já escolhida a desmarca.
        _uiState.value = _uiState.value.copy(selectedCategory = if (current == id) null else id)
        loadProducts()
    }

    fun refresh() {
        loadCategories()
        loadWallet()
        loadProducts()
    }

    fun loadMore() {
        val state = _uiState.value

        if (state.isLoading || state.isLoadingMore || !state.hasNextPage) {
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)

            when (val result = shop.products(state.page + 1, state.query, state.selectedCategory)) {
                is Outcome.Success -> _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    products = _uiState.value.products + result.value.items,
                    page = result.value.meta.currentPage,
                    hasNextPage = result.value.meta.hasNextPage,
                )

                is Outcome.Failure -> _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    error = result.error.message,
                )
            }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.value = state.copy(isLoading = true, error = null)

            when (val result = shop.products(page = 1, query = state.query, categoryId = state.selectedCategory)) {
                is Outcome.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    products = result.value.items,
                    page = result.value.meta.currentPage,
                    hasNextPage = result.value.meta.hasNextPage,
                )

                is Outcome.Failure -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    products = emptyList(),
                    error = result.error.message,
                )
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val result = shop.categories()

            if (result is Outcome.Success) {
                // Categoria sem produto só ocuparia espaço na régua de filtros.
                _uiState.value = _uiState.value.copy(categories = result.value.filter { it.productsCount > 0 })
            }
        }
    }

    private fun loadWallet() {
        viewModelScope.launch {
            val result = shop.pixelWallet()

            if (result is Outcome.Success) {
                _uiState.value = _uiState.value.copy(wallet = result.value)
            }
        }
    }

    companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 400L
    }
}
