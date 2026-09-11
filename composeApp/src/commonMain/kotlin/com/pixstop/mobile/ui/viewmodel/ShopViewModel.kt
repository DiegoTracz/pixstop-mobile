package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.repository.ShopRepository
import com.pixstop.mobile.domain.model.Appliance
import com.pixstop.mobile.domain.model.ApplianceChoice
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
    /** De qual geladeira é esta compra (Fase 9.5). */
    val appliances: ApplianceChoice = ApplianceChoice.Empty,
    /** A folha de escolha está aberta. */
    val choosingAppliance: Boolean = false,
) {
    /** A porta em frente à qual a pessoa está, quando já se sabe qual é. */
    val appliance: Appliance? get() = appliances.current
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
        loadAppliances()
        loadCategories()
        loadWallet()
        loadProducts()
    }

    /**
     * Quais geladeiras existem e em qual a pessoa está.
     *
     * Com uma porta só o servidor responde `mustChoose: false` e nada muda na
     * tela: ela é a resposta, e não há pergunta a fazer.
     */
    private fun loadAppliances() {
        viewModelScope.launch {
            val result = shop.appliances()

            if (result is Outcome.Success) {
                _uiState.value = _uiState.value.copy(
                    appliances = result.value,
                    choosingAppliance = result.value.mustChoose,
                )
            }
        }
    }

    fun openApplianceChoice() {
        if (_uiState.value.appliances.hasChoice) {
            _uiState.value = _uiState.value.copy(choosingAppliance = true)
        }
    }

    fun dismissApplianceChoice() {
        // Fechar sem escolher só vale depois de já haver uma porta: sem
        // nenhuma, a vitrine não sabe o que mostrar.
        if (_uiState.value.appliance != null) {
            _uiState.value = _uiState.value.copy(choosingAppliance = false)
        }
    }

    /**
     * A pessoa disse em frente a qual porta está — pela lista ou pelo QR
     * colado na geladeira.
     *
     * A vitrine recarrega porque o disponível é o daquela porta: o que sobra
     * no terceiro andar não diz nada sobre o primeiro.
     */
    fun onApplianceSelected(id: Long) {
        val state = _uiState.value

        if (state.appliances.appliances.none { it.id == id }) return

        _uiState.value = state.copy(
            appliances = state.appliances.copy(currentId = id, mustChoose = false),
            choosingAppliance = false,
        )

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
        loadAppliances()
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
