package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.core.image.downscaleJpeg
import com.pixstop.mobile.data.repository.ProductRepository
import com.pixstop.mobile.domain.model.CatalogSuggestion
import com.pixstop.mobile.domain.model.CreatedProduct
import com.pixstop.mobile.domain.model.NewProduct
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.PhotoBackground
import com.pixstop.mobile.domain.model.PhotoReading
import com.pixstop.mobile.domain.model.PhotoScene
import com.pixstop.mobile.domain.model.PhotoStyle
import com.pixstop.mobile.domain.model.ProductCategory
import com.pixstop.mobile.domain.model.ProductSuggestions
import com.pixstop.mobile.domain.model.SimilarProduct
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * O cadastro de produto no app (APP_OPERADOR_PRODUTOS.md, G4).
 *
 * A foto preenche o que dá; a pessoa diz o custo e confere o preço. Tudo o
 * que decide alguma coisa é puro, para ser testado sem rede.
 */
data class ProductFormUiState(
    val categories: List<ProductCategory> = emptyList(),
    val photo: ByteArray? = null,
    val name: String = "",
    val categoryId: Long? = null,
    val description: String = "",
    val barcode: String = "",
    val price: String = "",
    val cost: String = "",
    val initialStock: String = "",
    /** O que a foto sugeriu, para a tela dizer de onde veio cada campo. */
    val reading: PhotoReading? = null,
    /** A pessoa escolheu o item do catálogo da plataforma: a foto e a descrição vêm de lá. */
    val catalogChoice: CatalogSuggestion? = null,
    /** A pessoa viu os parecidos e disse que é outro produto. */
    val confirmedNew: Boolean = false,
    val isReading: Boolean = false,
    val isSaving: Boolean = false,
    val cameraOpen: Boolean = false,
    val scannerOpen: Boolean = false,
    val error: String? = null,
    val created: CreatedProduct? = null,
    /** Vindo de um item da nota: o que já existe para aquele nome, antes de qualquer foto. */
    val noteSuggestions: ProductSuggestions? = null,
    /** Como a IA deve deixar a foto: fundo limpo ou cenário (G6). */
    val style: PhotoStyle = PhotoStyle(),
    /** A prévia que a IA devolveu, à espera do "usar esta". */
    val enhanced: ByteArray? = null,
    /** A pessoa aprovou a prévia: é ela que vira a foto do produto. */
    val useEnhanced: Boolean = false,
    val isEnhancing: Boolean = false,
    /** Veio de um item da nota: nome e custo já estão lá, e falta o preço. */
    val fromNote: Boolean = false,
    /** A gaveta "Melhorar com IA" aberta: a IA só trabalha quando pedem. */
    val enhanceOpen: Boolean = false,
    /** Estoque inicial, código e descrição: opcionais, ficam recolhidos. */
    val moreOpen: Boolean = false,
) {
    /** Há de onde a IA partir: a foto do celular ou a do catálogo escolhido. */
    val canEnhance: Boolean
        get() = (photo != null || catalogChoice?.hasPhoto == true) && !isEnhancing && !isSaving && !isReading

    /**
     * A foto que vai no cadastro: a melhorada, se aprovada; senão a do
     * celular; com o catálogo escolhido e nada melhorado, nenhuma — o
     * servidor copia a do catálogo.
     */
    val photoToSend: ByteArray?
        get() = when {
            useEnhanced && enhanced != null -> enhanced
            catalogChoice != null -> null
            else -> photo
        }

    /** A foto do celular, guardada ao lado quando a vitrine usa a da IA. */
    val originalToSend: ByteArray?
        get() = if (useEnhanced && enhanced != null) photo else null

    val priceValue: Double? get() = parseMoney(price)
    val costValue: Double? get() = parseMoney(cost)
    val stockValue: Int get() = initialStock.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0

    /** Parecidos que ainda pedem uma resposta: é o mesmo, ou outro? */
    val similar: List<SimilarProduct>
        get() = if (confirmedNew) emptyList() else reading?.similar ?: noteSuggestions?.similar.orEmpty()

    /** O catálogo da plataforma, sem os de outro tamanho que o nome diz. */
    val catalog: List<CatalogSuggestion>
        get() = if (catalogChoice != null) {
            emptyList()
        } else {
            (reading?.catalog ?: noteSuggestions?.catalog.orEmpty()).filter { sameSize(name, it.name) != false }
        }

    /** A margem sobre o preço, que é o que quem compra no atacado olha. */
    val marginPercent: Int?
        get() {
            val sale = priceValue ?: return null
            val unit = costValue ?: return null
            if (sale <= 0.0) return null
            return (((sale - unit) / sale) * 100).let { kotlin.math.round(it).toInt() }
        }

    /** Custo acima do preço quase sempre é vírgula no lugar errado. */
    val sellsAtLoss: Boolean get() = (marginPercent ?: 0) < 0

    val nameError: String? get() = if (name.isNotBlank() && name.trim().length < 2) "Nome curto demais" else null

    val canSave: Boolean
        get() = name.trim().length >= 2 &&
            (priceValue ?: 0.0) > 0.0 &&
            costValue != null &&
            similar.isEmpty() &&
            !isSaving &&
            !isReading &&
            !isEnhancing

    /**
     * Aplica a leitura da foto sem apagar o que a pessoa já digitou: o que ela
     * escreveu vale mais que o que a foto achou.
     */
    fun withReading(result: PhotoReading): ProductFormUiState = copy(
        reading = result,
        name = name.ifBlank { result.name.orEmpty() },
        description = description.ifBlank { result.description.orEmpty() },
        categoryId = categoryId ?: result.category?.id,
        price = price.ifBlank { result.estimatedPrice?.let(::formatMoneyInput).orEmpty() },
        confirmedNew = false,
    )

    fun toNewProduct(): NewProduct = NewProduct(
        name = name.trim(),
        categoryId = categoryId,
        description = description.trim().ifBlank { null },
        barcode = barcode.trim().ifBlank { null },
        price = priceValue ?: 0.0,
        cost = costValue ?: 0.0,
        initialStock = stockValue,
        catalogProductId = catalogChoice?.id,
    )
}

class ProductFormViewModel(
    private val repository: ProductRepository,
    private val shrink: (ByteArray) -> ByteArray = { downscaleJpeg(it, maxSide = 1280) },
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductFormUiState())
    val uiState: StateFlow<ProductFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            (repository.categories() as? Outcome.Success)?.let { result ->
                _uiState.update { it.copy(categories = result.value) }
            }
        }
    }

    private var prefilled = false

    /**
     * Vindo de um item da nota: nome, custo e código já estão lá. Só uma vez,
     * para a recomposição da tela não apagar o que a pessoa corrigiu.
     */
    fun prefill(draft: ProductDraft) {
        if (prefilled) return
        prefilled = true
        _uiState.update {
            it.copy(
                name = draft.name,
                cost = draft.cost?.let(::formatMoneyInput).orEmpty(),
                barcode = draft.barcode.orEmpty(),
                catalogChoice = draft.catalog ?: it.catalogChoice,
                // Na nota a pessoa já viu os parecidos e disse "nenhum destes".
                confirmedNew = true,
                fromNote = true,
            )
        }

        // O nome da nota já basta para achar a foto no catálogo da plataforma.
        if (draft.name.length >= 3) {
            viewModelScope.launch {
                (repository.suggestions(draft.name, draft.barcode) as? Outcome.Success)?.let { result ->
                    _uiState.update { it.copy(noteSuggestions = result.value) }
                }
            }
        }
    }

    fun setBackground(value: PhotoBackground) = _uiState.update { it.copy(style = it.style.copy(background = value)) }

    fun setScene(value: PhotoScene) = _uiState.update { it.copy(style = it.style.copy(scene = value)) }

    fun setSceneHint(value: String) = _uiState.update { it.copy(style = it.style.copy(sceneHint = value.take(200))) }

    /** Manda a foto para a IA com o estilo escolhido; a prévia volta para aprovar. */
    fun enhance() {
        val current = _uiState.value
        if (!current.canEnhance) return

        // O catálogo escolhido vale mais que a foto do celular: é a foto boa.
        val fromCatalog = current.catalogChoice?.takeIf { it.hasPhoto }?.id

        viewModelScope.launch {
            _uiState.update { it.copy(isEnhancing = true, error = null, enhanced = null, useEnhanced = false) }

            when (val outcome = repository.enhance(if (fromCatalog != null) null else current.photo, fromCatalog, current.style)) {
                is Outcome.Success -> _uiState.update { it.copy(isEnhancing = false, enhanced = outcome.value) }
                is Outcome.Failure -> _uiState.update { it.copy(isEnhancing = false, error = outcome.error.message) }
            }
        }
    }

    fun useEnhanced() = _uiState.update { it.copy(useEnhanced = true, enhanceOpen = false) }

    fun openEnhance() = _uiState.update { it.copy(enhanceOpen = true, error = null) }

    fun closeEnhance() = _uiState.update { it.copy(enhanceOpen = false) }

    fun toggleMore() = _uiState.update { it.copy(moreOpen = !it.moreOpen) }

    /** "Ficar com a original": a prévia some, e a foto de antes volta a valer. */
    fun discardEnhanced() = _uiState.update { it.copy(enhanced = null, useEnhanced = false) }

    fun openCamera() = _uiState.update { it.copy(cameraOpen = true, error = null) }

    fun closeCamera() = _uiState.update { it.copy(cameraOpen = false) }

    fun openScanner() = _uiState.update { it.copy(scannerOpen = true) }

    fun closeScanner() = _uiState.update { it.copy(scannerOpen = false) }

    fun onBarcode(code: String) = _uiState.update { it.copy(barcode = code.trim(), scannerOpen = false) }

    /** A foto chegou: guarda e já manda ler. */
    fun onPhoto(raw: ByteArray) {
        val photo = shrink(raw)
        _uiState.update {
            it.copy(photo = photo, cameraOpen = false, isReading = true, error = null, catalogChoice = null, enhanced = null, useEnhanced = false)
        }

        viewModelScope.launch {
            when (val outcome = repository.readPhoto(photo)) {
                is Outcome.Success -> _uiState.update { it.withReading(outcome.value).copy(isReading = false) }
                // Sem leitura a foto continua valendo: ela vira a foto do produto.
                is Outcome.Failure -> _uiState.update { it.copy(isReading = false, error = outcome.error.message) }
            }
        }
    }

    fun updateName(value: String) = _uiState.update { it.copy(name = value) }

    fun updateDescription(value: String) = _uiState.update { it.copy(description = value) }

    fun updatePrice(value: String) = _uiState.update { it.copy(price = value) }

    fun updateCost(value: String) = _uiState.update { it.copy(cost = value) }

    fun updateStock(value: String) = _uiState.update { it.copy(initialStock = value.filter(Char::isDigit).take(5)) }

    fun updateBarcode(value: String) = _uiState.update { it.copy(barcode = value.filter(Char::isDigit).take(14)) }

    fun chooseCategory(id: Long?) = _uiState.update { it.copy(categoryId = id) }

    fun confirmIsNew() = _uiState.update { it.copy(confirmedNew = true) }

    fun chooseCatalog(item: CatalogSuggestion) = _uiState.update { it.copy(catalogChoice = item, enhanced = null, useEnhanced = false) }

    fun clearCatalog() = _uiState.update { it.copy(catalogChoice = null, enhanced = null, useEnhanced = false) }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    fun save() {
        val current = _uiState.value
        if (!current.canSave) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            when (val outcome = repository.create(current.toNewProduct(), current.photoToSend, current.originalToSend)) {
                is Outcome.Success -> _uiState.update { it.copy(isSaving = false, created = outcome.value) }
                is Outcome.Failure -> _uiState.update { it.copy(isSaving = false, error = outcome.error.message) }
            }
        }
    }

    /** "Cadastrar outro": a mesma tela, limpa, com as categorias já carregadas. */
    fun startAnother() = _uiState.update { ProductFormUiState(categories = it.categories) }
}
