package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.model.Named
import com.pixstop.mobile.domain.model.ShoppingList
import com.pixstop.mobile.domain.model.ShoppingRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** As regras da lista de compras (COMPRAS_MOBILE.md, A2). */
class ShoppingListUiStateTest {

    private val mango = ShoppingRow(7, "Monster Ultra Fiest Mango Loco", "Energéticos", null, 0, 9.8, "2026-10-16", 12, 8.99)
    private val trento = ShoppingRow(8, "Trento Chocolate", "Chocolates", null, 0, 9.8, null, 12, 1.79)
    private val kitkat = ShoppingRow(9, "Kit Kat", "Chocolates", null, 19, 5.0, null, 0, 4.49)

    private val list = ShoppingList(
        company = "Locarmais", appliances = listOf(Named(1, "Geladeira 1")), applianceId = null, weekday = 1,
        rows = listOf(mango, trento), covered = listOf(kitkat), coverDays = 7, safetyPercent = 20, sellingDaysPerWeek = 5.0,
    )

    @Test
    fun `a quantidade editada muda o total, e sem edicao vale a sugerida`() {
        val state = ShoppingListUiState(list = list)

        assertEquals(24, state.totalUnits)
        assertEquals(12 * 8.99 + 12 * 1.79, state.totalCost, 0.001)

        val edited = state.copy(quantities = mapOf(7L to 6))
        assertEquals(18, edited.totalUnits)
        assertEquals(6 * 8.99 + 12 * 1.79, edited.totalCost, 0.001)
    }

    @Test
    fun `os grupos seguem a categoria`() {
        assertEquals(listOf("Energéticos", "Chocolates"), ShoppingListUiState(list = list).groups.map { it.first })
    }

    @Test
    fun `o texto e o mesmo da web, e zero sai do texto`() {
        val text = ShoppingListUiState(list = list, quantities = mapOf(8L to 0)).messageText

        assertTrue(text.startsWith("🛒 *Compras da semana – Locarmais*"))
        assertTrue(text.contains("*Energéticos*\n• Monster Ultra Fiest Mango Loco – 12 un (zerado)"))
        assertFalse(text.contains("Trento"))
        assertFalse(text.contains("*Chocolates*"))
        assertTrue(text.contains("💰 Total estimado: ~R$ 107,88 no custo"))
        assertTrue(text.endsWith("✅ Não precisa: Kit Kat (19)"))
    }

    @Test
    fun `numa geladeira o texto diz qual`() {
        val text = ShoppingListUiState(list = list.copy(applianceId = 1)).messageText

        assertTrue(text.startsWith("🛒 *Compras da semana – Locarmais* (Geladeira 1)"))
    }
}
