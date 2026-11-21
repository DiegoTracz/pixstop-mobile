package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.model.ExpiryLot
import com.pixstop.mobile.domain.model.ExpiryOverview
import com.pixstop.mobile.domain.model.ExpiryPlace
import com.pixstop.mobile.domain.model.ProductExpiry
import com.pixstop.mobile.domain.model.UndatedLot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** As regras da tela "Validade" no app (VALIDADE.md, V5 c), sem rede. */
class ExpiryUiStateTest {

    private val vencido = ExpiryLot(1, "Pudim", "2026-11-18", 2, -2, expired = true, warning = true)
    private val vencendo = ExpiryLot(1, "Pudim", "2026-11-25", 3, 5, expired = false, warning = true)
    private val longe = ExpiryLot(2, "Brownie", "2027-01-19", 4, 60, expired = false, warning = false)
    private val copa = ExpiryPlace(1, "Copa", 2, listOf(vencido, vencendo, longe), listOf(UndatedLot(2, "Brownie", 1)))

    @Test
    fun `so o que pede atencao aparece, a menos que peca todos`() {
        val state = ExpiryUiState(overview = ExpiryOverview(listOf(copa), 2, 3, 1))

        assertEquals(listOf(vencido, vencendo), state.visibleLots(copa))
        assertEquals(3, state.copy(showAll = true).visibleLots(copa).size)
        assertFalse(state.isEmpty)
        assertTrue(ExpiryUiState(overview = ExpiryOverview(listOf(copa.copy(lots = emptyList(), undated = emptyList())), 0, 0, 0)).isEmpty)
    }

    @Test
    fun `o prazo vira frase e a data vira dia mes ano`() {
        assertEquals("venceu há 2 dias", vencido.whenLabel())
        assertEquals("venceu ontem", vencido.copy(daysLeft = -1).whenLabel())
        assertEquals("último dia hoje", vencido.copy(daysLeft = 0).whenLabel())
        assertEquals("vence amanhã", vencido.copy(daysLeft = 1).whenLabel())
        assertEquals("vence em 5 dias", vencendo.whenLabel())
        assertEquals("13/12/26", shortDate("2026-12-13"))
    }

    @Test
    fun `controla segue a categoria ate a pessoa escolher, e quem controla vem primeiro`() {
        val coca = ProductExpiry(1, "Coca", "Bebidas", categoryTracks = false, tracksExpiry = null, tracks = false, shelfLifeDays = null, warningDays = null)
        val pudim = ProductExpiry(2, "Pudim", "Lanches", categoryTracks = true, tracksExpiry = null, tracks = true, shelfLifeDays = 30, warningDays = null)

        assertEquals(TracksChoice.Category, coca.choice())
        assertTrue(coca.withChoice(TracksChoice.Yes).tracks)
        assertEquals(true, coca.withChoice(TracksChoice.Yes).tracksExpiry)
        assertFalse(pudim.withChoice(TracksChoice.No).tracks)
        // Voltar para "como a categoria" herda de novo.
        assertTrue(pudim.withChoice(TracksChoice.No).withChoice(TracksChoice.Category).tracks)

        assertEquals(listOf(2L, 1L), ExpirySettingsUiState(products = listOf(coca, pudim)).visible.map { it.id })
        assertEquals(listOf(2L), ExpirySettingsUiState(products = listOf(coca, pudim), query = "lanch").visible.map { it.id })
    }
}
