package com.pixstop.mobile.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * O mascote diz o mesmo no app e na web.
 *
 * Estas regras são as do docs/plans/MASCOTE_LED.md e do `lib/mascot.ts` da
 * web. Quebrar aqui é sinal de que o app passou a dizer outra coisa que o site
 * para a mesma situação.
 */
class MascotTest {

    private fun pickup(status: PickupStatus, reason: PickupReason? = null) =
        Pickup(status, canUnlock = false, reason = reason, attempts = 0, maxAttempts = 3, windowUntil = null, doorOpenedAt = null, pickedUpAt = null, hasTicket = false)

    @Test
    fun `o pedido espelha a fita como na web`() {
        val cases = mapOf(
            pickup(PickupStatus.Awaiting) to FitaState.Conectado,
            pickup(PickupStatus.Unlocking) to FitaState.Liberada,
            pickup(PickupStatus.Opened) to FitaState.Aberta,
            pickup(PickupStatus.PickedUp) to FitaState.Parado,
            pickup(PickupStatus.Manual) to FitaState.Parado,
            pickup(PickupStatus.Awaiting, PickupReason.Busy) to FitaState.Demais,
            pickup(PickupStatus.Awaiting, PickupReason.Offline) to FitaState.SemRede,
            pickup(PickupStatus.Awaiting, PickupReason.Exhausted) to FitaState.Bilhete,
            Pickup.None to FitaState.Parado,
        )

        cases.forEach { (pickup, expected) ->
            assertEquals(expected, FitaState.of(pickup), "status=${pickup.status} reason=${pickup.reason}")
        }
    }

    @Test
    fun `a janela vencida sem abrir volta a esperar`() {
        assertEquals(FitaState.Conectado, FitaState.of(pickup(PickupStatus.Unlocking), windowExpired = true))
    }

    @Test
    fun `falando por Bluetooth a fita fica ciano esperando o bilhete`() {
        assertEquals(FitaState.Conectado, FitaState.of(pickup(PickupStatus.Unlocking), talkingByBluetooth = true))
    }

    @Test
    fun `o ciclo RGB e so da espera`() {
        val cycling = MascotState.entries.filter { MascotLook.of(it).effect == MascotEffect.Cycle } +
            FitaState.entries.filter { MascotLook.of(it).effect == MascotEffect.Cycle }

        assertEquals(listOf<Any>(MascotState.Loading), cycling)
    }

    @Test
    fun `o aviso da interface e laranja, e o amarelo fica para a fita`() {
        assertEquals(MascotColors.Orange, MascotLook.of(MascotState.Warning).color)
        assertEquals(MascotColors.Yellow, MascotLook.of(FitaState.Demais).color)
        assertTrue(MascotState.entries.none { MascotLook.of(it).color == MascotColors.Yellow })
    }

    @Test
    fun `so a compra liberada e o sucesso soltam pixels`() {
        val withPixels = MascotState.entries.filter { MascotLook.of(it).pixels } +
            FitaState.entries.filter { MascotLook.of(it).pixels }

        assertEquals(listOf<Any>(MascotState.Success, FitaState.Liberada), withPixels)
    }

    @Test
    fun `apagado nao tem halo e so o branco pede contorno`() {
        assertFalse(MascotLook.of(FitaState.Desligado).lit)
        assertFalse(MascotLook.of(MascotState.Offline).lit)
        assertTrue(MascotLook.of(FitaState.Aberta).isPale)
        assertFalse(MascotLook.of(FitaState.Parado).isPale)
    }
}
