package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.repository.CompanyRepository
import com.pixstop.mobile.domain.model.CompanyDashboard
import com.pixstop.mobile.domain.model.CompanyMember
import com.pixstop.mobile.domain.model.CompanyOrder
import com.pixstop.mobile.domain.model.Department
import com.pixstop.mobile.domain.model.Outcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Seções do painel da empresa.
 */
enum class CompanySection(val label: String) {
    Summary("Resumo"),
    Orders("Pedidos"),
    People("Pessoas"),
    Teams("Times"),
}

/**
 * O que a pessoa está prestes a fazer e ainda pode desistir.
 *
 * Toda ação daqui mexe em dinheiro, pixels ou no acesso de alguém — nenhuma
 * acontece com um toque só.
 */
sealed interface CompanyAction {
    data class ApproveOrder(val order: CompanyOrder) : CompanyAction
    data class CancelOrder(val order: CompanyOrder) : CompanyAction
    data class ToggleUser(val member: CompanyMember) : CompanyAction
    data class AllocatePixels(val department: Department) : CompanyAction
    data class DistributePixels(val members: List<CompanyMember>) : CompanyAction
}

data class CompanyUiState(
    val section: CompanySection = CompanySection.Summary,
    val dashboard: CompanyDashboard? = null,
    val orders: List<CompanyOrder> = emptyList(),
    val members: List<CompanyMember> = emptyList(),
    val departments: List<Department> = emptyList(),
    val chosen: Set<Long> = emptySet(),
    val pending: CompanyAction? = null,
    val amountText: String = "",
    val reason: String = "",
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val message: String? = null,
    val error: String? = null,
) {
    val amount: Int get() = amountText.toIntOrNull() ?: 0


    val chosenMembers: List<CompanyMember> get() = members.filter { it.id in chosen }

    /**
     * Cancelar e aprovar exigem motivo no servidor — quem comprou vai ler.
     */
    val needsReason: Boolean
        get() = pending is CompanyAction.ApproveOrder || pending is CompanyAction.CancelOrder

    val needsAmount: Boolean
        get() = pending is CompanyAction.AllocatePixels ||
            pending is CompanyAction.DistributePixels

    val canConfirm: Boolean
        get() = when {
            isWorking -> false
            needsReason -> reason.trim().length >= MIN_REASON
            needsAmount -> amount > 0
            else -> pending != null
        }

    companion object {
        /** O servidor exige ao menos três caracteres no motivo. */
        const val MIN_REASON = 3
    }
}

/**
 * Painel do administrador da empresa.
 *
 * Cada seção carrega o que precisa, quando é aberta: o resumo é caro e as
 * outras listas não interessam a quem só quer ver as vendas.
 */
class CompanyViewModel(private val company: CompanyRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CompanyUiState())
    val uiState: StateFlow<CompanyUiState> = _uiState.asStateFlow()

    init {
        openSection(CompanySection.Summary)
    }

    fun openSection(section: CompanySection) {
        _uiState.value = _uiState.value.copy(section = section, error = null, message = null)
        load(section)
    }

    fun refresh() = load(_uiState.value.section)

    private fun load(section: CompanySection) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (section) {
                CompanySection.Summary -> when (val result = company.dashboard()) {
                    is Outcome.Success -> _uiState.value = _uiState.value.copy(isLoading = false, dashboard = result.value)
                    is Outcome.Failure -> fail(result.error.message)
                }

                CompanySection.Orders -> when (val result = company.orders()) {
                    is Outcome.Success -> _uiState.value = _uiState.value.copy(isLoading = false, orders = result.value.items)
                    is Outcome.Failure -> fail(result.error.message)
                }

                CompanySection.People -> when (val result = company.users()) {
                    is Outcome.Success -> _uiState.value = _uiState.value.copy(isLoading = false, members = result.value)
                    is Outcome.Failure -> fail(result.error.message)
                }

                CompanySection.Teams -> when (val result = company.departments()) {
                    is Outcome.Success -> _uiState.value = _uiState.value.copy(isLoading = false, departments = result.value)
                    is Outcome.Failure -> fail(result.error.message)
                }
            }
        }
    }

    fun toggleChosen(id: Long) {
        val chosen = _uiState.value.chosen

        _uiState.value = _uiState.value.copy(chosen = if (id in chosen) chosen - id else chosen + id)
    }

    fun ask(action: CompanyAction) {
        _uiState.value = _uiState.value.copy(pending = action, amountText = "", reason = "", error = null, message = null)
    }

    fun dismiss() {
        _uiState.value = _uiState.value.copy(pending = null, amountText = "", reason = "", error = null)
    }

    fun onAmountChange(value: String) {
        val filtered = value.filter { it.isDigit() }.take(7)

        _uiState.value = _uiState.value.copy(amountText = filtered, error = null)
    }

    fun onReasonChange(value: String) {
        _uiState.value = _uiState.value.copy(reason = value.take(500), error = null)
    }

    fun confirm() {
        val state = _uiState.value
        val action = state.pending

        if (!state.canConfirm || action == null) {
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isWorking = true, error = null)

            val result = when (action) {
                is CompanyAction.ApproveOrder ->
                    company.orderAction(action.order.id, "approve", state.reason.trim())

                is CompanyAction.CancelOrder ->
                    company.orderAction(action.order.id, "cancel", state.reason.trim())

                // O resultado diz o novo estado, mas quem age já sabe qual
                // é: o que importa aqui é ter dado certo.
                is CompanyAction.ToggleUser ->
                    when (val toggled = company.toggleUser(action.member.id)) {
                        is Outcome.Success -> Outcome.Success(Unit)
                        is Outcome.Failure -> Outcome.Failure(toggled.error)
                    }

                is CompanyAction.AllocatePixels ->
                    company.allocateToDepartment(action.department.id, state.amount, state.reason)

                is CompanyAction.DistributePixels ->
                    company.distributePixels(action.members.map { it.id }, state.amount, state.reason)
            }

            when (result) {
                is Outcome.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isWorking = false,
                        pending = null,
                        amountText = "",
                        reason = "",
                        chosen = emptySet(),
                        message = successMessage(action, state),
                    )

                    // Os números mudaram; seguir mostrando os antigos levaria a
                    // uma segunda ação em cima de um estado que já não existe.
                    refresh()
                }

                is Outcome.Failure -> _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    error = result.error.message,
                )
            }
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    private fun successMessage(action: CompanyAction, state: CompanyUiState): String = when (action) {
        is CompanyAction.ApproveOrder -> "Pedido aprovado."
        is CompanyAction.CancelOrder -> "Pedido cancelado."
        is CompanyAction.ToggleUser ->
            if (action.member.isActive) "Acesso removido." else "Acesso liberado."

        is CompanyAction.AllocatePixels -> "${state.amount} pixels na verba de ${action.department.name}."
        is CompanyAction.DistributePixels ->
            "${state.amount} pixels para ${action.members.size} " +
                if (action.members.size == 1) "pessoa." else "pessoas."
    }

    private fun fail(message: String) {
        _uiState.value = _uiState.value.copy(isLoading = false, error = message)
    }
}
