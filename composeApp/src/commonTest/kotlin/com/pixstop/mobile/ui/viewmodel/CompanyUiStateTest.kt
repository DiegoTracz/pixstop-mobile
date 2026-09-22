package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.model.CompanyMember
import com.pixstop.mobile.domain.model.CompanyOrder
import com.pixstop.mobile.domain.model.CompanyRole
import com.pixstop.mobile.domain.model.Department
import com.pixstop.mobile.domain.model.OrderStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * As ações do painel mexem em dinheiro, pixels e no acesso de alguém, e várias
 * não têm desfazer. Liberar o botão de confirmar sem o que o servidor exige só
 * levaria a uma recusa depois do susto.
 */
class CompanyUiStateTest {

    private val ana = CompanyMember(
        id = 4,
        name = "Ana Costa",
        email = "ana.costa@alpha.com",
        role = CompanyRole.Member,
        isActive = true,
        balance = 150.0,
        pixelAvailable = 3_050,
    )

    private val pedido = CompanyOrder(
        id = 1,
        transactionId = "ORD-1",
        buyerName = "Ana Costa",
        status = OrderStatus.Pending,
        statusLabel = "Aguardando pagamento",
        paymentMethodLabel = "PIX",
        itemsCount = 1,
        totalMoney = 3.0,
        totalBalance = 0.0,
        totalPixels = 0,
        netRevenue = 2.85,
        createdAt = null,
    )

    private val vendas = Department(2, "Vendas", null, 9_900, 4, isActive = true)

    @Test
    fun `aprovar exige motivo com o tamanho que o servidor pede`() {
        val base = CompanyUiState(pending = CompanyAction.ApproveOrder(pedido), isLoading = false)

        assertTrue(base.needsReason)
        assertFalse(base.canConfirm)
        assertFalse(base.copy(reason = "ok").canConfirm)
        assertTrue(base.copy(reason = "pago na maquininha").canConfirm)
    }

    @Test
    fun `motivo so com espacos nao conta`() {
        val state = CompanyUiState(pending = CompanyAction.CancelOrder(pedido), reason = "     ", isLoading = false)

        assertFalse(state.canConfirm)
    }

    @Test
    fun `saldo aceita virgula como separador decimal`() {
        val state = CompanyUiState(
            pending = CompanyAction.AdjustBalance(ana, credit = true),
            amountText = "12,50",
            isLoading = false,
        )

        assertEquals(12.5, state.money)
        assertTrue(state.canConfirm)
    }

    @Test
    fun `saldo zerado nao libera o botao`() {
        val state = CompanyUiState(
            pending = CompanyAction.AdjustBalance(ana, credit = false),
            amountText = "0",
            isLoading = false,
        )

        assertFalse(state.canConfirm)
    }

    @Test
    fun `alocar exige quantidade nao motivo`() {
        val base = CompanyUiState(pending = CompanyAction.AllocatePixels(vendas), isLoading = false)

        assertFalse(base.needsReason)
        assertTrue(base.needsAmount)
        assertFalse(base.canConfirm)
        assertTrue(base.copy(amountText = "500").canConfirm)
    }

    @Test
    fun `desativar acesso nao pede nada alem da confirmacao`() {
        val state = CompanyUiState(pending = CompanyAction.ToggleUser(ana), isLoading = false)

        assertFalse(state.needsReason)
        assertFalse(state.needsAmount)
        assertTrue(state.canConfirm)
    }

    @Test
    fun `nada confirma enquanto a acao esta em curso`() {
        val state = CompanyUiState(
            pending = CompanyAction.ToggleUser(ana),
            isWorking = true,
            isLoading = false,
        )

        assertFalse(state.canConfirm)
    }

    @Test
    fun `pedido encerrado nao aceita acao`() {
        assertTrue(pedido.isOpen)
        assertFalse(pedido.copy(status = OrderStatus.Paid).isOpen)
        assertFalse(pedido.copy(status = OrderStatus.Canceled).isOpen)
    }

    @Test
    fun `escolhidos saem da lista de pessoas carregada`() {
        val outro = ana.copy(id = 5, name = "Pedro Souza")
        val state = CompanyUiState(members = listOf(ana, outro), chosen = setOf(5), isLoading = false)

        assertEquals(listOf("Pedro Souza"), state.chosenMembers.map { it.name })
    }
}
