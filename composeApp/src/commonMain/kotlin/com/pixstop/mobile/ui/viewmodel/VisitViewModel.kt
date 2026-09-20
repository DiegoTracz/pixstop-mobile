package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.core.storage.CountDraft
import com.pixstop.mobile.core.storage.CountDraftStore
import com.pixstop.mobile.data.repository.VisitRepository
import com.pixstop.mobile.domain.model.CountProduct
import com.pixstop.mobile.domain.model.CountResult
import com.pixstop.mobile.domain.model.CountResultItem
import com.pixstop.mobile.domain.model.CountSheet
import com.pixstop.mobile.domain.model.LossSuggestion
import com.pixstop.mobile.domain.model.Outcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Os passos da visita, na ordem em que a prateleira pede. */
enum class VisitStep { Counting, Differences, Done }

/**
 * O estado da visita (docs/plans/CONCILIACAO_MOBILE.md).
 *
 * Tudo o que decide alguma coisa mora aqui e é puro: é isso que permite
 * testar a regra sem rede, sem Compose e sem ViewModel.
 */
data class VisitUiState(
    val applianceId: Long = 0,
    val applianceName: String = "",
    val step: VisitStep = VisitStep.Counting,
    val products: List<CountProduct> = emptyList(),
    /** Produto → o que a pessoa contou. Ausente é **não contado**. */
    val counts: Map<Long, Int> = emptyMap(),
    val query: String = "",
    val result: CountResult? = null,
    val lastCountLabel: String? = null,
    val isLoading: Boolean = false,
    val isWorking: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val draftFound: Boolean = false,
    val scannerOpen: Boolean = false,
    /** O produto que o leitor achou: a lista rola até ele e ele fica em foco. */
    val focusedProductId: Long? = null,
) {
    val visibleProducts: List<CountProduct>
        get() = if (query.isBlank()) {
            products
        } else {
            products.filter { it.name.contains(query, ignoreCase = true) || it.category?.contains(query, ignoreCase = true) == true }
        }

    val countedCount: Int get() = counts.size

    val skipped: List<Long> get() = products.map { it.id }.filterNot { counts.containsKey(it) }

    val progress: Float get() = if (products.isEmpty()) 0f else countedCount.toFloat() / products.size

    /** Enviar exige ao menos uma linha: contagem vazia não concilia nada. */
    val canReview: Boolean get() = countedCount > 0 && !isWorking

    val hasSkipped: Boolean get() = skipped.isNotEmpty()

    fun countOf(productId: Long): Int? = counts[productId]
}

class VisitViewModel(
    private val repository: VisitRepository,
    private val drafts: CountDraftStore,
    private val appVersion: String? = null,
    private val now: () -> String = { "" },
) : ViewModel() {

    private val _uiState = MutableStateFlow(VisitUiState())
    val uiState: StateFlow<VisitUiState> = _uiState.asStateFlow()

    private var sheet: CountSheet? = null
    private var draft: CountDraft? = null

    fun load(applianceId: Long) {
        _uiState.update { it.copy(isLoading = true, error = null, applianceId = applianceId) }

        viewModelScope.launch {
            when (val outcome = repository.sheet(applianceId)) {
                is Outcome.Success -> applySheet(outcome.value)
                is Outcome.Failure -> _uiState.update { it.copy(isLoading = false, error = outcome.error.message) }
            }
        }
    }

    private fun applySheet(loaded: CountSheet) {
        sheet = loaded

        val saved = drafts.load(loaded.applianceId)
        // O rascunho manda no `clientId`: reenviar o que já foi enviado tem de
        // cair no mesmo documento, e não criar um segundo.
        draft = saved ?: CountDraft(clientId = loaded.clientId, startedAt = now())

        _uiState.update {
            it.copy(
                isLoading = false,
                applianceName = loaded.applianceName,
                products = loaded.products,
                counts = draft?.counts.orEmpty().mapKeys { entry -> entry.key.toLong() },
                draftFound = saved != null && saved.counts.isNotEmpty(),
                lastCountLabel = loaded.lastCount?.let { last ->
                    when {
                        last.daysAgo <= 0 -> "contada hoje" + (last.by?.let { by -> ", por $by" } ?: "")
                        last.daysAgo == 1 -> "contada ontem"
                        else -> "contada há ${last.daysAgo} dias"
                    }
                } ?: "nunca contada",
            )
        }
    }

    /** O número que a pessoa digitou ou o stepper mudou. Nunca vai abaixo de zero. */
    fun setCount(productId: Long, value: Int) {
        val safe = value.coerceAtLeast(0)

        _uiState.update { it.copy(counts = it.counts + (productId to safe), focusedProductId = null) }
        persist()
    }

    /** "Olhei e não tem": zero é um toque explícito, nunca o valor de partida. */
    fun markEmpty(productId: Long) = setCount(productId, 0)

    fun clearCount(productId: Long) {
        _uiState.update { it.copy(counts = it.counts - productId) }
        persist()
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }

    fun openScanner() = _uiState.update { it.copy(scannerOpen = true) }

    fun closeScanner() = _uiState.update { it.copy(scannerOpen = false) }

    /**
     * O código lido: acha o produto e o deixa em foco. Código desconhecido
     * vira aviso, não erro — pode ser produto de outra empresa na prateleira.
     */
    fun onBarcode(code: String) {
        val found = _uiState.value.products.firstOrNull { it.barcode == code }

        _uiState.update {
            it.copy(
                scannerOpen = false,
                focusedProductId = found?.id,
                query = "",
                message = if (found == null) "Este código não é de nenhum produto desta geladeira." else null,
            )
        }
    }

    fun dismissMessage() = _uiState.update { it.copy(message = null, error = null) }

    /** Envia a contagem e passa para as diferenças, que é onde o esperado aparece. */
    fun review() {
        val current = _uiState.value
        val loaded = sheet ?: return
        val saved = draft ?: return

        if (!current.canReview) return

        _uiState.update { it.copy(isWorking = true, error = null) }

        viewModelScope.launch {
            val outcome = repository.submit(
                applianceId = loaded.applianceId,
                clientId = saved.clientId,
                counts = current.counts,
                skipped = current.skipped,
                appVersion = appVersion,
                startedAt = saved.startedAt,
            )

            when (outcome) {
                is Outcome.Success -> {
                    // Só aqui o rascunho pode sumir: antes disso, ele é a
                    // única cópia do que a pessoa contou.
                    drafts.clear(loaded.applianceId)
                    _uiState.update { it.copy(isWorking = false, step = VisitStep.Differences, result = outcome.value) }
                }

                is Outcome.Failure -> _uiState.update {
                    it.copy(isWorking = false, error = outcome.error.message)
                }
            }
        }
    }

    /** O palpite sobre a falta. Sugestão: quem lança a perda é o dono do estoque. */
    fun suggest(item: CountResultItem, reason: LossSuggestion) {
        val result = _uiState.value.result ?: return
        val chosen = if (item.suggestedReason == reason) null else reason

        _uiState.update { it.copy(result = result.withSuggestion(item.productId, chosen)) }

        viewModelScope.launch { repository.suggest(result.id, item.productId, chosen) }
    }

    /** "Contei errado": o servidor estorna, e a linha volta para a contagem. */
    fun recount(item: CountResultItem) {
        val result = _uiState.value.result ?: return

        _uiState.update {
            it.copy(
                step = VisitStep.Counting,
                counts = it.counts - item.productId,
                focusedProductId = item.productId,
                message = "Conte de novo o ${item.product}. O acerto anterior foi estornado.",
                result = result.withRecount(item.productId),
            )
        }

        viewModelScope.launch { repository.recount(result.id, item.productId) }
    }

    fun finish() = _uiState.update { it.copy(step = VisitStep.Done) }

    fun backToCounting() = _uiState.update { it.copy(step = VisitStep.Counting) }

    private fun persist() {
        val loaded = sheet ?: return
        val saved = draft ?: return
        val updated = saved.copy(counts = _uiState.value.counts.mapKeys { it.key.toString() })

        draft = updated
        drafts.save(loaded.applianceId, updated)
    }
}

/** A sugestão aplicada na hora, sem esperar a rede: é um toque, não um formulário. */
fun CountResult.withSuggestion(productId: Long, reason: LossSuggestion?): CountResult =
    copy(items = items.map { if (it.productId == productId) it.copy(suggestedReason = reason) else it })

fun CountResult.withRecount(productId: Long): CountResult =
    copy(items = items.map { if (it.productId == productId) it.copy(recounted = true) else it })
