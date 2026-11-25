package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.repository.ProductRepository
import com.pixstop.mobile.data.repository.VisitRepository
import com.pixstop.mobile.domain.model.FridgeProduct
import com.pixstop.mobile.domain.model.FridgeProducts
import com.pixstop.mobile.domain.model.Outcome
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * "Produtos desta geladeira" (APP_OPERADOR_PRODUTOS.md, G2).
 *
 * Ver o que mora nela, pôr o que o depósito tem e cadastrar o que falta, sem
 * passar pela contagem. É a tela da primeira carga e do abastecimento avulso;
 * a visita, que conta às cegas, continua sendo outra.
 */
data class FridgeProductsUiState(
    val applianceId: Long = 0,
    val products: FridgeProducts? = null,
    val query: String = "",
    /** Quanto pôr de cada produto agora, saindo do depósito, pelo id. */
    val adding: Map<Long, Int> = emptyMap(),
    /**
     * A primeira carga: o que está na mão de quem instala, sem nota e sem
     * depósito. Entra como acerto de estoque (decisão do Diego, 24/11).
     */
    val firstLoad: Map<Long, Int> = emptyMap(),
    /**
     * Gerado quando a pessoa começa a abastecer e mantido até dar certo: um
     * reenvio depois de uma queda de rede não abastece duas vezes.
     */
    val clientId: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val scannerOpen: Boolean = false,
    val message: String? = null,
    val error: String? = null,
) {
    private fun FridgeProduct.matches(): Boolean =
        query.isBlank() ||
            name.contains(query.trim(), ignoreCase = true) ||
            category?.contains(query.trim(), ignoreCase = true) == true ||
            barcode == query.trim()

    val here: List<FridgeProduct> get() = products?.inAppliance.orEmpty().filter { it.matches() }

    /** O resto do cadastro: o que tem no depósito dá para pôr agora. */
    val others: List<FridgeProduct> get() = products?.others.orEmpty().filter { it.matches() }

    val isEmptyFridge: Boolean get() = products != null && products.inAppliance.none { it.quantity > 0 }

    val hasNoProducts: Boolean get() = products != null && products.inAppliance.isEmpty() && products.others.isEmpty()

    /** Quanto a linha do produto mostra no contador, venha de onde vier. */
    fun addingOf(productId: Long): Int = adding[productId] ?: firstLoad[productId] ?: 0

    val units: Int get() = adding.values.sum() + firstLoad.values.sum()

    /** Quantas unidades entram como acerto, para o botão dizer. */
    val firstLoadUnits: Int get() = firstLoad.values.sum()

    val canRestock: Boolean get() = units > 0 && !isSaving

    /**
     * Com estoque no depósito, sai de lá, e não dá para pôr mais do que ele
     * tem: o servidor recusaria tudo. Sem nada no depósito, é a primeira
     * carga, e entra como acerto.
     */
    fun withAdding(product: FridgeProduct, value: Int): FridgeProductsUiState {
        if (product.inWarehouse <= 0) {
            val clamped = value.coerceIn(0, MAX_FIRST_LOAD)
            return copy(firstLoad = if (clamped == 0) firstLoad - product.id else firstLoad + (product.id to clamped))
        }

        val clamped = value.coerceIn(0, product.inWarehouse)
        return copy(adding = if (clamped == 0) adding - product.id else adding + (product.id to clamped))
    }

    companion object {
        /** O mesmo teto do servidor por item. */
        const val MAX_FIRST_LOAD = 10_000
    }
}

class FridgeProductsViewModel(
    private val products: ProductRepository,
    private val visits: VisitRepository,
    private val newClientId: () -> String = { "app-" + Random.nextLong().toULong().toString(36) + Random.nextInt().toUInt().toString(36) },
) : ViewModel() {

    private val _uiState = MutableStateFlow(FridgeProductsUiState())
    val uiState: StateFlow<FridgeProductsUiState> = _uiState.asStateFlow()

    fun load(applianceId: Long) {
        _uiState.update { it.copy(applianceId = applianceId, isLoading = it.products == null, error = null) }

        viewModelScope.launch {
            when (val outcome = products.applianceProducts(applianceId)) {
                is Outcome.Success -> _uiState.update { it.copy(products = outcome.value, isLoading = false) }
                is Outcome.Failure -> _uiState.update { it.copy(isLoading = false, error = outcome.error.message) }
            }
        }
    }

    fun onQuery(value: String) = _uiState.update { it.copy(query = value) }

    fun setAdding(product: FridgeProduct, value: Int) = _uiState.update { it.withAdding(product, value) }

    fun openScanner() = _uiState.update { it.copy(scannerOpen = true) }

    fun closeScanner() = _uiState.update { it.copy(scannerOpen = false) }

    /** O código lido vira a busca: o produto aparece sozinho na lista. */
    fun onBarcode(code: String) = _uiState.update { it.copy(query = code.trim(), scannerOpen = false) }

    fun dismissMessage() = _uiState.update { it.copy(message = null, error = null) }

    fun restock() {
        val current = _uiState.value
        if (!current.canRestock) return

        val clientId = current.clientId ?: newClientId()
        _uiState.update { it.copy(isSaving = true, clientId = clientId, error = null) }

        viewModelScope.launch {
            when (val outcome = visits.restock(current.applianceId, clientId, current.adding, current.firstLoad)) {
                is Outcome.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            adding = emptyMap(),
                            firstLoad = emptyMap(),
                            clientId = null,
                            message = "${outcome.value.units} unidade(s) na geladeira.",
                        )
                    }
                    load(current.applianceId)
                }

                is Outcome.Failure -> _uiState.update { it.copy(isSaving = false, error = outcome.error.message) }
            }
        }
    }
}
