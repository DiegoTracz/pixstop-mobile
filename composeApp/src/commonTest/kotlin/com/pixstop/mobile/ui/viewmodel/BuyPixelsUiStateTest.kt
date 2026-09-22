package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.model.TopupMethod
import com.pixstop.mobile.domain.model.TopupOffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * As contas que a tela mostra antes de cobrar: quanto se recebe e quanto se
 * paga. Errar aqui é prometer um preço e cobrar outro.
 */
class BuyPixelsUiStateTest {

    private val offer = TopupOffer(
        enabled = true,
        reason = null,
        min = 10,
        max = 500,
        presets = listOf(20, 50, 100),
        pixelsPerReal = 100,
        cardFeePercentage = 4.0,
        cardFeeFixed = 0.0,
    )

    @Test
    fun `reais 50 no pix compram 5000 pixels e custam 50`() {
        val state = BuyPixelsUiState(offer = offer, preset = 50)

        assertTrue(state.isValid)
        assertEquals(5000, state.pixels)
        assertEquals(50.0, state.charged)
    }

    @Test
    fun `no cartao a taxa entra no que se paga`() {
        val state = BuyPixelsUiState(offer = offer, preset = 50, method = TopupMethod.Card)

        assertEquals(52.0, state.charged)
        assertEquals(5000, state.pixels)
    }

    @Test
    fun `o campo livre vence o valor pronto`() {
        val state = BuyPixelsUiState(offer = offer, preset = 50, customText = "30")

        assertEquals(30, state.reais)
        assertEquals(3000, state.pixels)
    }

    @Test
    fun `fora dos limites nao deixa comprar`() {
        assertFalse(BuyPixelsUiState(offer = offer, customText = "9").isValid)
        assertFalse(BuyPixelsUiState(offer = offer, customText = "501").isValid)
        assertEquals(0, BuyPixelsUiState(offer = offer, customText = "9").pixels)
    }

    @Test
    fun `o contador do pix so aparece com relogio e prazo`() {
        assertNull(BuyPixelsUiState(offer = offer).pixSecondsLeft)
    }
}
