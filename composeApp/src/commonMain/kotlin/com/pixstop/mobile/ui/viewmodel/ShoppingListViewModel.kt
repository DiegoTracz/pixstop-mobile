package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.repository.ShoppingListRepository
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.ShoppingList
import com.pixstop.mobile.domain.model.ShoppingRow
import com.pixstop.mobile.ui.components.formatMoney
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Os dias do aviso, na ordem do `dayOfWeek` do servidor (0 = domingo). */
val WEEKDAYS = listOf("domingo", "segunda", "terça", "quarta", "quinta", "sexta", "sábado")

/**
 * A lista de compras (docs/plans/COMPRAS_MOBILE.md, passo 1).
 *
 * A quantidade editada vive só aqui e no texto compartilhado: o servidor não
 * guarda a lista, só a calcula.
 */
data class ShoppingListUiState(
    val list: ShoppingList? = null,
    val quantities: Map<Long, Int> = emptyMap(),
    val settingsOpen: Boolean = false,
    val coverDays: Int = 7,
    val safetyPercent: Int = 20,
    val weekday: Int? = 1,
    val isLoading: Boolean = false,
    val isWorking: Boolean = false,
    val error: String? = null,
    val message: String? = null,
) {
    val rows: List<ShoppingRow> get() = list?.rows.orEmpty()

    fun quantityOf(row: ShoppingRow): Int = quantities[row.productId] ?: row.suggested

    /** Por categoria, na ordem em que o servidor mandou. */
    val groups: List<Pair<String, List<ShoppingRow>>>
        get() = rows.groupBy { it.category ?: "Sem categoria" }.toList()

    val totalUnits: Int get() = rows.sumOf { quantityOf(it) }
    val totalCost: Double get() = rows.sumOf { quantityOf(it) * it.unitCost }
    val zeroed: Int get() = rows.count { it.zeroedSince != null }

    /**
     * O texto que vai para o grupo de quem compra, igual ao da web: negrito
     * com asterisco, um item por linha, total no fim e o que não precisa.
     */
    val messageText: String
        get() {
            val current = list ?: return ""
            val place = current.appliances.firstOrNull { it.id == current.applianceId }?.let { " (${it.name})" }.orEmpty()
            val lines = mutableListOf("🛒 *Compras da semana – ${current.company.ifBlank { "Geladeira" }}*$place")

            for ((category, items) in groups) {
                val chosen = items.filter { quantityOf(it) > 0 }
                if (chosen.isEmpty()) continue
                lines += ""
                lines += "*$category*"
                chosen.forEach { row ->
                    lines += "• ${row.name} – ${quantityOf(row)} un" + if (row.zeroedSince != null) " (zerado)" else ""
                }
            }

            lines += ""
            lines += "💰 Total estimado: ~${formatMoney(totalCost)} no custo"

            if (current.covered.isNotEmpty()) {
                lines += ""
                lines += "✅ Não precisa: " + current.covered.joinToString(", ") { "${it.name} (${it.stock})" }
            }

            return lines.joinToString("\n")
        }
}

class ShoppingListViewModel(private val repository: ShoppingListRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingListUiState())
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()

    fun load(applianceId: Long? = null) {
        _uiState.update { it.copy(isLoading = it.list == null, isWorking = true, error = null) }
        viewModelScope.launch {
            when (val outcome = repository.list(applianceId)) {
                is Outcome.Success -> _uiState.update {
                    it.copy(
                        list = outcome.value,
                        quantities = emptyMap(),
                        coverDays = outcome.value.coverDays,
                        safetyPercent = outcome.value.safetyPercent,
                        weekday = outcome.value.weekday,
                        isLoading = false,
                        isWorking = false,
                    )
                }

                is Outcome.Failure -> _uiState.update { it.copy(isLoading = false, isWorking = false, error = outcome.error.message) }
            }
        }
    }

    fun setQuantity(productId: Long, value: Int) =
        _uiState.update { it.copy(quantities = it.quantities + (productId to value.coerceAtLeast(0))) }

    fun openSettings() = _uiState.update { it.copy(settingsOpen = true) }
    fun closeSettings() = _uiState.update { it.copy(settingsOpen = false) }
    fun onCoverDays(value: Int) = _uiState.update { it.copy(coverDays = value.coerceIn(1, 30)) }
    fun onSafety(value: Int) = _uiState.update { it.copy(safetyPercent = value.coerceIn(0, 100)) }
    fun onWeekday(value: Int?) = _uiState.update { it.copy(weekday = value) }
    fun dismissMessage() = _uiState.update { it.copy(message = null, error = null) }
    fun notifyCopied() = _uiState.update { it.copy(message = "Lista copiada. Cole no WhatsApp.") }

    fun saveSettings() {
        val state = _uiState.value
        _uiState.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            when (val outcome = repository.saveSettings(state.coverDays, state.safetyPercent, state.weekday)) {
                is Outcome.Success -> {
                    _uiState.update { it.copy(settingsOpen = false, message = "Ritmo de compra atualizado.") }
                    load(state.list?.applianceId)
                }

                is Outcome.Failure -> _uiState.update { it.copy(isWorking = false, error = outcome.error.message) }
            }
        }
    }

    /** "Vou comprar": abre a compra com o que a lista pede e devolve o id para a tela navegar. */
    fun startPurchase(onOpened: (Long) -> Unit) {
        val state = _uiState.value
        if (state.totalUnits == 0) return
        _uiState.update { it.copy(isWorking = true, error = null) }
        viewModelScope.launch {
            when (val outcome = repository.startPurchase(state.rows.associateWith { state.quantityOf(it) })) {
                is Outcome.Success -> {
                    _uiState.update { it.copy(isWorking = false) }
                    onOpened(outcome.value)
                }

                is Outcome.Failure -> _uiState.update { it.copy(isWorking = false, error = outcome.error.message) }
            }
        }
    }
}
