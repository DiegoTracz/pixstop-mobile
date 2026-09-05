package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.model.Department
import com.pixstop.mobile.domain.model.TeamMember
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A distribuição sai da verba do departamento e é tudo ou nada no servidor.
 * Deixar o botão liberado com um total acima da verba só gastaria uma ida e
 * volta para receber a mesma recusa.
 */
class TeamUiStateTest {

    private val vendas = Department(
        id = 2,
        name = "Vendas",
        description = null,
        pixelBalance = 1_000,
        membersCount = 3,
        isActive = true,
    )

    private fun membro(id: Long, ativo: Boolean = true) = TeamMember(
        userId = id,
        name = "Pessoa $id",
        email = "p$id@alpha.com",
        isManager = false,
        isActive = ativo,
        pixelAvailable = 0,
    )

    private val base = TeamUiState(
        departments = listOf(vendas),
        selected = vendas,
        members = listOf(membro(1), membro(2), membro(3, ativo = false)),
        isLoading = false,
    )

    @Test
    fun `o total e a quantia vezes o numero de escolhidos`() {
        val state = base.copy(chosen = setOf(1, 2), amountText = "100")

        assertEquals(200, state.totalToDistribute)
        assertTrue(state.canDistribute)
    }

    @Test
    fun `acima da verba o botao trava`() {
        val state = base.copy(chosen = setOf(1, 2), amountText = "600")

        assertEquals(1_200, state.totalToDistribute)
        assertTrue(state.exceedsBudget)
        assertFalse(state.canDistribute)
    }

    @Test
    fun `sem ninguem escolhido ou sem quantia o botao trava`() {
        assertFalse(base.copy(amountText = "100").canDistribute)
        assertFalse(base.copy(chosen = setOf(1)).canDistribute)
        assertFalse(base.copy(chosen = setOf(1), amountText = "0").canDistribute)
    }

    @Test
    fun `so quem esta ativo conta como elegivel`() {
        assertEquals(listOf(1L, 2L), base.eligible.map { it.userId })
    }

    @Test
    fun `marcar todos ignora quem esta sem acesso`() {
        val todos = base.copy(chosen = setOf(1, 2))

        assertTrue(todos.allChosen, "os dois ativos já são todos")
    }

    @Test
    fun `quantia sem numero conta como zero`() {
        assertEquals(0, base.copy(amountText = "").amount)
    }
}
