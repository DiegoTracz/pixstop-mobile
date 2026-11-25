package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.core.image.downscaleJpeg
import com.pixstop.mobile.data.repository.ProductRepository
import com.pixstop.mobile.domain.model.BatchItem
import com.pixstop.mobile.domain.model.NewProduct
import com.pixstop.mobile.domain.model.Outcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Em que pé está cada linha na hora de salvar. */
sealed interface BatchStatus {
    data object Waiting : BatchStatus
    data object Saving : BatchStatus
    data object Saved : BatchStatus
    data class Failed(val message: String) : BatchStatus
}

/**
 * Uma linha da revisão: o que a foto achou e o que a pessoa corrigiu. O
 * custo começa vazio de propósito: só quem comprou sabe quanto pagou.
 */
data class BatchRow(
    val item: BatchItem,
    val name: String = item.name,
    val price: String = item.estimatedPrice?.let(::formatMoneyInput).orEmpty(),
    val cost: String = "",
    /** Com parecido já cadastrado, começa de fora: o mais comum é ser o mesmo. */
    val included: Boolean = item.similar.isEmpty(),
    val status: BatchStatus = BatchStatus.Waiting,
) {
    val priceValue: Double? get() = parseMoney(price)
    val costValue: Double? get() = parseMoney(cost)

    /** Pronta para cadastrar: nome, preço e custo ditos. */
    val isReady: Boolean get() = name.trim().length >= 2 && (priceValue ?: 0.0) > 0.0 && costValue != null

    val pendingSave: Boolean get() = included && status != BatchStatus.Saved

    fun toNewProduct(): NewProduct = NewProduct(
        name = name.trim(),
        categoryId = item.category?.id,
        description = item.description?.takeIf { it.isNotBlank() },
        barcode = null,
        price = priceValue ?: 0.0,
        cost = costValue ?: 0.0,
        initialStock = 0,
        catalogProductId = null,
    )
}

/**
 * Vários produtos numa foto (APP_OPERADOR_PRODUTOS.md, G4.1).
 *
 * A foto vira uma lista; a pessoa confere cada linha, diz o custo e cadastra
 * tudo de uma vez. Cada linha é salva sozinha: a que falhar mostra o porquê,
 * e as outras não se perdem.
 */
data class BatchProductsUiState(
    val photo: ByteArray? = null,
    val rows: List<BatchRow> = emptyList(),
    val truncated: Boolean = false,
    val limit: Int = 12,
    val isReading: Boolean = false,
    val isSaving: Boolean = false,
    val cameraOpen: Boolean = false,
    val error: String? = null,
) {
    val toSave: List<BatchRow> get() = rows.filter { it.pendingSave }

    /** Só salva quando toda linha marcada está pronta: nada pela metade. */
    val canSave: Boolean get() = toSave.isNotEmpty() && toSave.all { it.isReady } && !isSaving && !isReading

    val missingCost: Int get() = toSave.count { it.costValue == null }

    val saved: Int get() = rows.count { it.status == BatchStatus.Saved }

    val failed: Int get() = rows.count { it.status is BatchStatus.Failed }

    /** Acabou: tudo o que estava marcado foi salvo. */
    val isDone: Boolean get() = rows.isNotEmpty() && saved > 0 && toSave.isEmpty()
}

class BatchProductsViewModel(
    private val repository: ProductRepository,
    private val shrink: (ByteArray) -> ByteArray = { downscaleJpeg(it, maxSide = 2048) },
) : ViewModel() {

    private val _uiState = MutableStateFlow(BatchProductsUiState())
    val uiState: StateFlow<BatchProductsUiState> = _uiState.asStateFlow()

    fun openCamera() = _uiState.update { it.copy(cameraOpen = true, error = null) }

    fun closeCamera() = _uiState.update { it.copy(cameraOpen = false) }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    /** A foto chegou: lê na hora. Rótulo pequeno pede a foto maior que a de um produto só. */
    fun onPhoto(raw: ByteArray) {
        val photo = shrink(raw)
        _uiState.update { BatchProductsUiState(photo = photo, isReading = true) }

        viewModelScope.launch {
            when (val outcome = repository.analyzeBatch(photo)) {
                is Outcome.Success -> _uiState.update {
                    it.copy(
                        isReading = false,
                        rows = outcome.value.items.map(::BatchRow),
                        truncated = outcome.value.truncated,
                        limit = outcome.value.limit,
                        error = if (outcome.value.items.isEmpty()) "Não achei nenhum produto na foto. Tente com mais luz e os rótulos de frente." else null,
                    )
                }

                is Outcome.Failure -> _uiState.update { it.copy(isReading = false, error = outcome.error.message) }
            }
        }
    }

    private fun updateRow(index: Int, change: (BatchRow) -> BatchRow) = _uiState.update { state ->
        state.copy(rows = state.rows.mapIndexed { i, row -> if (i == index) change(row) else row })
    }

    fun setName(index: Int, value: String) = updateRow(index) { it.copy(name = value) }

    fun setPrice(index: Int, value: String) = updateRow(index) { it.copy(price = value) }

    fun setCost(index: Int, value: String) = updateRow(index) { it.copy(cost = value) }

    fun setIncluded(index: Int, value: Boolean) = updateRow(index) { it.copy(included = value) }

    /** Salva uma linha por vez, na ordem da lista. */
    fun save() {
        if (!_uiState.value.canSave) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            _uiState.value.rows.forEachIndexed { index, row ->
                if (!row.pendingSave) return@forEachIndexed

                updateRow(index) { it.copy(status = BatchStatus.Saving) }

                when (val outcome = repository.create(row.toNewProduct(), row.item.photo)) {
                    is Outcome.Success -> updateRow(index) { it.copy(status = BatchStatus.Saved) }
                    is Outcome.Failure -> updateRow(index) { it.copy(status = BatchStatus.Failed(outcome.error.message)) }
                }
            }

            _uiState.update { it.copy(isSaving = false) }
        }
    }

    /** Outra foto: a lista recomeça. */
    fun restart() = _uiState.update { BatchProductsUiState() }
}
