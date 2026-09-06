package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixstop.mobile.data.remote.dto.StaffMemberDto
import com.pixstop.mobile.data.remote.dto.TodayCheckinDto
import com.pixstop.mobile.data.repository.StaffRepository
import com.pixstop.mobile.domain.model.Outcome
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StaffCheckinUiState(
    val query: String = "",
    val members: List<StaffMemberDto> = emptyList(),
    val selected: StaffMemberDto? = null,
    val kind: String = "visit",
    val amount: String = "",
    val description: String = "",
    val today: List<TodayCheckinDto> = emptyList(),
    val isSearching: Boolean = false,
    val isRegistering: Boolean = false,
    val lastResult: String? = null,
    val error: String? = null,
    val voucherCode: String = "",
    val isValidating: Boolean = false,
) {
    val canRegister: Boolean get() = selected != null && !isRegistering && amountValue != null

    /** Vazio é "sem valor"; qualquer outra coisa precisa ser um número não negativo. */
    val amountValue: Double? get() = if (amount.isBlank()) 0.0 else amount.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 }
}

/**
 * O balcão: achar a pessoa (código, nome, e-mail ou telefone — ou o QR) e
 * registrar a visita. O que ela rendeu aparece na hora.
 */
class StaffCheckinViewModel(private val repository: StaffRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(StaffCheckinUiState())
    val uiState: StateFlow<StaffCheckinUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadToday()
    }

    fun onQueryChange(value: String) {
        val query = MemberCodeParser.parse(value)
        _uiState.value = _uiState.value.copy(query = query, error = null)

        searchJob?.cancel()
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(members = emptyList())
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.value = _uiState.value.copy(isSearching = true)

            when (val result = repository.findMembers(query)) {
                is Outcome.Success -> {
                    val members = result.value
                    // Um resultado só, e certeiro pelo código: já escolhe.
                    val exact = members.singleOrNull()?.takeIf { it.memberCode.equals(query, ignoreCase = true) }
                    _uiState.value = _uiState.value.copy(members = members, isSearching = false, selected = exact ?: _uiState.value.selected)
                }

                is Outcome.Failure -> _uiState.value = _uiState.value.copy(isSearching = false, error = result.error.message)
            }
        }
    }

    fun select(member: StaffMemberDto?) {
        _uiState.value = _uiState.value.copy(selected = member, error = null, lastResult = null)
    }

    fun onKindChange(kind: String) {
        _uiState.value = _uiState.value.copy(kind = kind)
    }

    fun onAmountChange(value: String) {
        _uiState.value = _uiState.value.copy(amount = value.filter { it.isDigit() || it == ',' || it == '.' }.take(10))
    }

    fun onDescriptionChange(value: String) {
        _uiState.value = _uiState.value.copy(description = value.take(120))
    }

    fun register() {
        val state = _uiState.value
        val member = state.selected ?: return
        if (!state.canRegister) return

        viewModelScope.launch {
            _uiState.value = state.copy(isRegistering = true, error = null)

            when (val result = repository.register(member.id, state.kind, state.amountValue?.takeIf { it > 0 }, state.description)) {
                is Outcome.Success -> {
                    val earned = listOfNotNull(
                        result.value.cashback.takeIf { it > 0 }?.let { "$it pixels de cashback" },
                        result.value.xp.takeIf { it > 0 }?.let { "$it XP" },
                    )
                    _uiState.value = StaffCheckinUiState(
                        today = _uiState.value.today,
                        lastResult = "Registrado para ${result.value.name}" + if (earned.isEmpty()) "." else ": ${earned.joinToString(" e ")}.",
                    )
                    loadToday()
                }

                is Outcome.Failure -> _uiState.value = _uiState.value.copy(isRegistering = false, error = result.error.message)
            }
        }
    }

    fun onVoucherCodeChange(value: String) {
        _uiState.value = _uiState.value.copy(voucherCode = VoucherCodeParser.parse(value).take(12), error = null)
    }

    /** O balcão valida um voucher: uma vez, dentro do prazo. */
    fun validateVoucher() {
        val code = _uiState.value.voucherCode.trim()
        if (code.length < 4 || _uiState.value.isValidating) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isValidating = true, error = null, lastResult = null)

            _uiState.value = when (val result = repository.validateVoucher(code)) {
                is Outcome.Success -> _uiState.value.copy(
                    isValidating = false,
                    voucherCode = "",
                    lastResult = "Voucher válido: ${result.value.rewardName}" + (result.value.name?.let { " para $it" } ?: "") + ". Pode entregar.",
                )

                is Outcome.Failure -> _uiState.value.copy(isValidating = false, error = result.error.message)
            }
        }
    }

    private fun loadToday() {
        viewModelScope.launch {
            when (val result = repository.today()) {
                is Outcome.Success -> _uiState.value = _uiState.value.copy(today = result.value)
                is Outcome.Failure -> Unit
            }
        }
    }
}

/** Um QR do balcão vem como `pixstop://u/ABC123XY`: só o código interessa. */
object MemberCodeParser {
    private val fromLink = Regex("/u/([A-Za-z0-9]+)")

    fun parse(raw: String): String =
        fromLink.find(raw)?.groupValues?.get(1)?.uppercase() ?: raw
}

/** Um QR de voucher vem como `pixstop://v/ABC123XY`: só o código interessa. */
object VoucherCodeParser {
    private val fromLink = Regex("/v/([A-Za-z0-9]+)")

    fun parse(raw: String): String =
        fromLink.find(raw)?.groupValues?.get(1)?.uppercase() ?: raw.uppercase()
}
