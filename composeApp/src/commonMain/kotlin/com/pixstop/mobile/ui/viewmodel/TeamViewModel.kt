package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.repository.TeamRepository
import com.pixstop.mobile.domain.model.Department
import com.pixstop.mobile.domain.model.Outcome
import com.pixstop.mobile.domain.model.TeamMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TeamUiState(
    val departments: List<Department> = emptyList(),
    val selected: Department? = null,
    val members: List<TeamMember> = emptyList(),
    val chosen: Set<Long> = emptySet(),
    val amountText: String = "",
    val reason: String = "",
    val isLoading: Boolean = true,
    val isDistributing: Boolean = false,
    val message: String? = null,
    val error: String? = null,
) {
    val amount: Int get() = amountText.toIntOrNull() ?: 0

    /** Só quem está ativo pode receber; inativo continua na lista, apagado. */
    val eligible: List<TeamMember> get() = members.filter { it.isActive }

    val allChosen: Boolean get() = eligible.isNotEmpty() && chosen.size == eligible.size

    /** O que sai da verba: a mesma quantia para cada pessoa escolhida. */
    val totalToDistribute: Int get() = amount * chosen.size

    val exceedsBudget: Boolean
        get() = selected != null && totalToDistribute > selected.pixelBalance

    val canDistribute: Boolean
        get() = !isDistributing && chosen.isNotEmpty() && amount > 0 && !exceedsBudget
}

/**
 * Área do gestor: a verba do departamento e a distribuição para o time.
 *
 * A distribuição é tudo ou nada no servidor, então a tela pode avisar o
 * resultado sem ressalva — ou todos receberam, ou ninguém recebeu.
 */
class TeamViewModel(private val team: TeamRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TeamUiState())
    val uiState: StateFlow<TeamUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = team.departments()) {
                is Outcome.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, departments = result.value)

                    // Com um departamento só, escolher não é decisão: abre nele.
                    result.value.firstOrNull()?.takeIf { result.value.size == 1 }?.let { select(it) }
                }

                is Outcome.Failure -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.error.message,
                )
            }
        }
    }

    fun select(department: Department) {
        _uiState.value = _uiState.value.copy(
            selected = department,
            members = emptyList(),
            chosen = emptySet(),
            message = null,
            error = null,
        )

        viewModelScope.launch {
            when (val result = team.members(department.id)) {
                is Outcome.Success -> _uiState.value = _uiState.value.copy(members = result.value)
                is Outcome.Failure -> _uiState.value = _uiState.value.copy(error = result.error.message)
            }
        }
    }

    fun back() {
        _uiState.value = _uiState.value.copy(selected = null, members = emptyList(), chosen = emptySet())
    }

    fun toggleMember(userId: Long) {
        val chosen = _uiState.value.chosen

        _uiState.value = _uiState.value.copy(
            chosen = if (userId in chosen) chosen - userId else chosen + userId,
            message = null,
            error = null,
        )
    }

    fun toggleAll() {
        val state = _uiState.value

        _uiState.value = state.copy(
            chosen = if (state.allChosen) emptySet() else state.eligible.map { it.userId }.toSet(),
            message = null,
            error = null,
        )
    }

    fun onAmountChange(value: String) {
        // Só dígitos: o servidor espera um inteiro, e deixar digitar letras só
        // adiaria a recusa.
        _uiState.value = _uiState.value.copy(
            amountText = value.filter { it.isDigit() }.take(7),
            message = null,
            error = null,
        )
    }

    fun onReasonChange(value: String) {
        _uiState.value = _uiState.value.copy(reason = value.take(255))
    }

    fun distribute() {
        val state = _uiState.value
        val department = state.selected

        if (!state.canDistribute || department == null) {
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isDistributing = true, error = null, message = null)

            val result = team.distribute(department.id, state.chosen.toList(), state.amount, state.reason)

            when (result) {
                is Outcome.Success -> {
                    val people = state.chosen.size

                    _uiState.value = _uiState.value.copy(
                        isDistributing = false,
                        chosen = emptySet(),
                        amountText = "",
                        reason = "",
                        message = "Distribuídos ${state.amount} pixels para $people " +
                            if (people == 1) "pessoa." else "pessoas.",
                    )

                    // A verba e os saldos mudaram: recarrega para a tela não
                    // seguir oferecendo pixels que já saíram.
                    reloadAfterDistribution(department.id)
                }

                is Outcome.Failure -> _uiState.value = _uiState.value.copy(
                    isDistributing = false,
                    error = result.error.message,
                )
            }
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    private suspend fun reloadAfterDistribution(departmentId: Long) {
        when (val departments = team.departments()) {
            is Outcome.Success -> _uiState.value = _uiState.value.copy(
                departments = departments.value,
                selected = departments.value.firstOrNull { it.id == departmentId } ?: _uiState.value.selected,
            )

            is Outcome.Failure -> Unit
        }

        when (val members = team.members(departmentId)) {
            is Outcome.Success -> _uiState.value = _uiState.value.copy(members = members.value)
            is Outcome.Failure -> Unit
        }
    }
}
