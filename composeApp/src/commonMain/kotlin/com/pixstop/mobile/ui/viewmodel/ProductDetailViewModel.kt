package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.repository.ShopRepository
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val product: Product? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * Detalhe do produto.
 *
 * Recarrega do servidor em vez de reaproveitar o item da lista: entre abrir a
 * vitrine e tocar no produto, o estoque disponível pode ter mudado.
 */
class ProductDetailViewModel(private val shop: ShopRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = shop.product(id)) {
                is Outcome.Success -> _uiState.value = ProductDetailUiState(product = result.value, isLoading = false)
                is Outcome.Failure -> _uiState.value = ProductDetailUiState(isLoading = false, error = result.error.message)
            }
        }
    }
}
