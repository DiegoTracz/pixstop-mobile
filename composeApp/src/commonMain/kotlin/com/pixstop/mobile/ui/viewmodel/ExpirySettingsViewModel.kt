package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.repository.ExpiryRepository
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.ProductExpiry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Os prazos que a Locarmais usa (decisão do Diego, 20/11). */
val SHELF_LIFE_OPTIONS = listOf(30, 60, 90)

/** A escolha de "controla validade": como a categoria, sim ou não. */
enum class TracksChoice { Category, Yes, No }

fun ProductExpiry.choice(): TracksChoice = when (tracksExpiry) {
    null -> TracksChoice.Category
    true -> TracksChoice.Yes
    false -> TracksChoice.No
}

/** Aplica a escolha na hora, sem esperar a rede: é um toque, não um formulário. */
fun ProductExpiry.withChoice(choice: TracksChoice): ProductExpiry {
    val explicit = when (choice) {
        TracksChoice.Category -> null
        TracksChoice.Yes -> true
        TracksChoice.No -> false
    }

    return copy(tracksExpiry = explicit, tracks = explicit ?: categoryTracks)
}

data class ExpirySettingsUiState(
    val products: List<ProductExpiry> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    /** Quem controla primeiro: é o que a pessoa veio acertar. */
    val visible: List<ProductExpiry>
        get() = products
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) || it.category?.contains(query, ignoreCase = true) == true }
            .sortedBy { if (it.tracks) 0 else 1 }
}

class ExpirySettingsViewModel(private val repository: ExpiryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpirySettingsUiState())
    val uiState: StateFlow<ExpirySettingsUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val outcome = repository.products()) {
                is Outcome.Success -> _uiState.update { it.copy(isLoading = false, products = outcome.value) }
                is Outcome.Failure -> _uiState.update { it.copy(isLoading = false, error = outcome.error.message) }
            }
        }
    }

    fun onQuery(value: String) = _uiState.update { it.copy(query = value) }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    fun setChoice(product: ProductExpiry, choice: TracksChoice) = save(product.withChoice(choice))

    /** Tocar no prazo marcado desmarca: fica sem prazo padrão. */
    fun setShelfLife(product: ProductExpiry, days: Int) =
        save(product.copy(shelfLifeDays = if (product.shelfLifeDays == days) null else days))

    private fun save(changed: ProductExpiry) {
        val before = _uiState.value.products.firstOrNull { it.id == changed.id } ?: return
        replace(changed)

        viewModelScope.launch {
            when (val outcome = repository.update(changed.id, changed.tracksExpiry, changed.shelfLifeDays, changed.warningDays)) {
                is Outcome.Success -> replace(outcome.value)
                is Outcome.Failure -> {
                    replace(before)
                    _uiState.update { it.copy(error = outcome.error.message) }
                }
            }
        }
    }

    private fun replace(product: ProductExpiry) = _uiState.update { state ->
        state.copy(products = state.products.map { if (it.id == product.id) product else it })
    }
}
