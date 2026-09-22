package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.model.Pickup
import com.pixstop.mobile.domain.model.PickupReason
import com.pixstop.mobile.domain.model.PickupStatus
import com.pixstop.mobile.domain.model.UnlockTicket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * O que a tela do pedido diz sobre a retirada (Fase 9.8).
 *
 * A frase e os botões moram no estado, e não na tela, porque são eles que
 * mudam a cada situação — e é assim que se confere sem celular nenhum: a
 * geladeira ocupada, a janela correndo, o bilhete de Bluetooth na mão.
 */
class OrderPickupUiStateTest {

    private val ticket = UnlockTicket("corpo.assinatura", "NHMBE0CXJ5", 9, 3, null)

    private fun state(
        pickup: Pickup,
        now: Long = 0,
        ticket: UnlockTicket? = null,
        bluetooth: Boolean = false,
        unlocking: Boolean = false,
    ) = OrderUiState(pickup = pickup, now = now, ticket = ticket, bluetoothSupported = bluetooth, isUnlocking = unlocking)

    private fun pickup(
        status: PickupStatus,
        canUnlock: Boolean = false,
        reason: PickupReason? = null,
        windowUntil: Long? = null,
        attempts: Int = 0,
        hasTicket: Boolean = false,
    ) = Pickup(status, canUnlock, reason, attempts, 3, windowUntil, null, null, hasTicket)

    @Test
    fun `abrindo agora conta os segundos que a porta ainda responde`() {
        val state = state(pickup(PickupStatus.Unlocking, canUnlock = true, windowUntil = 40_000), now = 10_000)

        assertEquals(30L, state.unlockSecondsLeft)
        assertEquals("Abrindo! Puxe a porta da geladeira (30 s).", state.pickupMessage)
    }

    @Test
    fun `janela vencida sem abrir convida a tentar de novo`() {
        val state = state(pickup(PickupStatus.Unlocking, canUnlock = true, windowUntil = 10_000), now = 60_000)

        assertEquals(0L, state.unlockSecondsLeft)
        assertEquals("A porta não abriu. Toque para tentar de novo.", state.pickupMessage)
        assertTrue(state.canUnlock)
    }

    @Test
    fun `cada recusa tem a sua conversa`() {
        assertEquals(
            "Alguém está usando a geladeira agora. Aguarde a porta fechar.",
            state(pickup(PickupStatus.Awaiting, reason = PickupReason.Busy)).pickupMessage,
        )
        assertEquals(
            "A geladeira está sem internet. Procure o responsável pela empresa.",
            state(pickup(PickupStatus.Awaiting, reason = PickupReason.Offline)).pickupMessage,
        )
        assertEquals(
            "Você já tentou abrir o máximo de vezes. Procure o responsável.",
            state(pickup(PickupStatus.Awaiting, reason = PickupReason.Exhausted)).pickupMessage,
        )
    }

    @Test
    fun `sem internet mas com bilhete o texto muda para o Bluetooth`() {
        val state = state(pickup(PickupStatus.Awaiting, reason = PickupReason.Offline, hasTicket = true))

        assertEquals("A geladeira está sem internet. Abra por Bluetooth, aqui do lado dela.", state.pickupMessage)
    }

    @Test
    fun `a porta que abriu e o produto retirado encerram a conversa`() {
        assertEquals("A porta abriu. Pode retirar o produto.", state(pickup(PickupStatus.Opened)).pickupMessage)
        assertEquals("Produto retirado.", state(pickup(PickupStatus.PickedUp)).pickupMessage)
        assertEquals("Retire o produto com o responsável.", state(pickup(PickupStatus.Manual)).pickupMessage)
    }

    @Test
    fun `o botao de abrir some enquanto a tentativa esta em curso`() {
        assertTrue(state(pickup(PickupStatus.Awaiting, canUnlock = true)).canUnlock)
        assertFalse(state(pickup(PickupStatus.Awaiting, canUnlock = true), unlocking = true).canUnlock)
        assertFalse(state(pickup(PickupStatus.Awaiting, canUnlock = false)).canUnlock)
    }

    @Test
    fun `o Bluetooth so aparece quando resolve alguma coisa`() {
        val offline = pickup(PickupStatus.Awaiting, reason = PickupReason.Offline, hasTicket = true)

        // Aparelho com rádio, bilhete na mão e geladeira fora do ar.
        assertTrue(state(offline, ticket = ticket, bluetooth = true).canUnlockByBluetooth)

        // Sem rádio, sem bilhete, ou com a geladeira respondendo: o caminho
        // normal é melhor, e é o que a pessoa espera.
        assertFalse(state(offline, ticket = ticket, bluetooth = false).canUnlockByBluetooth)
        assertFalse(state(offline, ticket = null, bluetooth = true).canUnlockByBluetooth)
        assertFalse(state(pickup(PickupStatus.Awaiting, canUnlock = true), ticket = ticket, bluetooth = true).canUnlockByBluetooth)

        // Depois que a porta abriu não há o que abrir.
        assertFalse(state(pickup(PickupStatus.Opened, hasTicket = true), ticket = ticket, bluetooth = true).canUnlockByBluetooth)
        assertFalse(state(pickup(PickupStatus.PickedUp, hasTicket = true), ticket = ticket, bluetooth = true).canUnlockByBluetooth)
    }

    @Test
    fun `durante a janela o Bluetooth continua a mao porque a rede pode ter caido no meio`() {
        val unlocking = pickup(PickupStatus.Unlocking, canUnlock = true, windowUntil = 40_000, hasTicket = true)

        assertTrue(state(unlocking, now = 10_000, ticket = ticket, bluetooth = true).canUnlockByBluetooth)
    }
}
