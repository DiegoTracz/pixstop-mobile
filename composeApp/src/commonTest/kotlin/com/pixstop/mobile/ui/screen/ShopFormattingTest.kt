package com.pixstop.mobile.ui.screen

import com.pixstop.mobile.ui.components.formatMoney
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Preço e contador são lidos por quem está decidindo comprar. Um centavo
 * comido ou um "0:5" no lugar de "0:05" muda o que a pessoa entende.
 */
class ShopFormattingTest {

    @Test
    fun `reais sempre com duas casas`() {
        assertEquals("R$ 3,00", formatMoney(3.0))
        assertEquals("R$ 5,50", formatMoney(5.5))
        assertEquals("R$ 0,05", formatMoney(0.05))
        assertEquals("R$ 12,90", formatMoney(12.9))
    }

    @Test
    fun `zero nao vira vazio`() {
        assertEquals("R$ 0,00", formatMoney(0.0))
    }

    @Test
    fun `contador com zero a esquerda nos segundos`() {
        assertEquals("5:00", formatCountdown(300))
        assertEquals("0:05", formatCountdown(5))
        assertEquals("4:59", formatCountdown(299))
        assertEquals("0:00", formatCountdown(0))
    }
}
