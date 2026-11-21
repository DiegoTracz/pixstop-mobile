package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.repository.ExpiryRepository
import com.pixstop.mobile.domain.model.ExpiryLot
import com.pixstop.mobile.domain.model.ExpiryOverview
import com.pixstop.mobile.domain.model.ExpiryPlace
import com.pixstop.mobile.domain.model.Outcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A tela "Validade" (VALIDADE.md, V5 c). As regras são puras e moram aqui. */
data class ExpiryUiState(
    val overview: ExpiryOverview? = null,
    /** Só o que pede atenção, a menos que a pessoa queira ver todos os lotes. */
    val showAll: Boolean = false,
    val isLoading: Boolean = false,
    /** O lugar cuja retirada está sendo enviada. */
    val removing: ExpiryPlace? = null,
    val message: String? = null,
    val error: String? = null,
) {
    fun visibleLots(place: ExpiryPlace): List<ExpiryLot> = if (showAll) place.lots else place.lots.filter { it.warning }

    val isEmpty: Boolean get() = overview?.places?.none { it.lots.isNotEmpty() || it.undated.isNotEmpty() } ?: false
}

/** "venceu há 2 dias", "último dia hoje", "vence em 5 dias". */
fun ExpiryLot.whenLabel(): String = when {
    daysLeft < -1 -> "venceu há ${-daysLeft} dias"
    daysLeft == -1 -> "venceu ontem"
    daysLeft == 0 -> "último dia hoje"
    daysLeft == 1 -> "vence amanhã"
    else -> "vence em $daysLeft dias"
}

/** "2026-12-13" vira "13/12/26". */
fun shortDate(iso: String): String {
    val parts = iso.split("-")
    return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0].takeLast(2)}" else iso
}

class ExpiryViewModel(private val repository: ExpiryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpiryUiState())
    val uiState: StateFlow<ExpiryUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val outcome = repository.overview()) {
                is Outcome.Success -> _uiState.update { it.copy(isLoading = false, overview = outcome.value) }
                is Outcome.Failure -> _uiState.update { it.copy(isLoading = false, error = outcome.error.message) }
            }
        }
    }

    fun toggleShowAll() = _uiState.update { it.copy(showAll = !it.showAll) }

    fun dismissMessage() = _uiState.update { it.copy(message = null, error = null) }

    /** Segurou para confirmar: o vencido daquele lugar sai, e a tela recarrega. */
    fun removeExpired(place: ExpiryPlace) {
        if (_uiState.value.removing != null || place.expiredUnits == 0) return

        _uiState.update { it.copy(removing = place, error = null) }
        viewModelScope.launch {
            when (val outcome = repository.removeExpired(place.applianceId)) {
                is Outcome.Success -> {
                    _uiState.update {
                        it.copy(
                            removing = null,
                            message = "${place.expiredUnits} ${if (place.expiredUnits == 1) "unidade vencida retirada" else "unidades vencidas retiradas"} de ${place.name}.",
                        )
                    }
                    load()
                }

                is Outcome.Failure -> _uiState.update { it.copy(removing = null, error = outcome.error.message) }
            }
        }
    }
}
