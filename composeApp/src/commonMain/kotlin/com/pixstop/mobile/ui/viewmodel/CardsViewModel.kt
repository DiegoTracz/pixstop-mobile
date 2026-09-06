package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.repository.CardRepository
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.SavedCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CardsUiState(
    val cards: List<SavedCard> = emptyList(),
    val isLoading: Boolean = true,
    /** O cartão que está sendo alterado agora, para travar só a linha dele. */
    val busyCardId: Long? = null,
    /** O cartão que a pessoa pediu para remover e ainda não confirmou. */
    val removingCard: SavedCard? = null,
    val error: String? = null,
) {
    val isEmpty: Boolean get() = !isLoading && cards.isEmpty() && error == null

    /** Só um cartão é o padrão; escolher outro desmarca o anterior. */
    fun withDefault(id: Long): CardsUiState {
        if (cards.none { it.id == id } || cards.first { it.id == id }.isDefault) {
            return this
        }

        return copy(cards = cards.map { it.copy(isDefault = it.id == id) })
    }

    /**
     * Sem o cartão removido. O padrão que se foi não é herdado por ninguém:
     * o servidor é quem decide isso, e a próxima leitura traz.
     */
    fun without(id: Long): CardsUiState = copy(cards = cards.filterNot { it.id == id })
}

/**
 * "Meus cartões": o que o fechamento oferece, gerenciado fora dele.
 *
 * Remover pede confirmação; escolher o padrão não — é reversível com um
 * toque e não custa nada.
 */
class CardsViewModel(private val repository: CardRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CardsUiState())
    val uiState: StateFlow<CardsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            _uiState.value = when (val result = repository.cards()) {
                is Outcome.Success -> CardsUiState(cards = result.value, isLoading = false)
                is Outcome.Failure -> CardsUiState(isLoading = false, error = result.error.message)
            }
        }
    }

    fun setDefault(card: SavedCard) {
        if (card.isDefault || _uiState.value.busyCardId != null) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busyCardId = card.id, error = null)

            when (val result = repository.setDefault(card.id)) {
                is Outcome.Success -> _uiState.value = _uiState.value.withDefault(card.id).copy(busyCardId = null)

                is Outcome.Failure -> _uiState.value = _uiState.value.copy(busyCardId = null, error = result.error.message)
            }
        }
    }

    fun askRemove(card: SavedCard) {
        _uiState.value = _uiState.value.copy(removingCard = card)
    }

    fun dismissRemove() {
        _uiState.value = _uiState.value.copy(removingCard = null)
    }

    fun confirmRemove() {
        val card = _uiState.value.removingCard ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busyCardId = card.id, removingCard = null, error = null)

            when (val result = repository.remove(card.id)) {
                is Outcome.Success -> _uiState.value = _uiState.value.without(card.id).copy(busyCardId = null)

                is Outcome.Failure -> _uiState.value = _uiState.value.copy(busyCardId = null, error = result.error.message)
            }
        }
    }
}
