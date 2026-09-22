package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.data.remote.dto.StaffMemberDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * O balcão registra com a pessoa escolhida e um valor que faça sentido —
 * vazio é "sem valor", e um QR lido vira só o código.
 */
class StaffCheckinUiStateTest {

    private val ana = StaffMemberDto(id = 4, name = "Ana Costa", email = "ana@exemplo.com", memberCode = "ABC123XY")

    @Test
    fun `so registra com alguem escolhido e valor valido`() {
        assertFalse(StaffCheckinUiState().canRegister)
        assertTrue(StaffCheckinUiState(selected = ana).canRegister)
        assertTrue(StaffCheckinUiState(selected = ana, amount = "59,90").canRegister)
        assertFalse(StaffCheckinUiState(selected = ana, amount = "abc").canRegister)
        assertFalse(StaffCheckinUiState(selected = ana, isRegistering = true).canRegister)
    }

    @Test
    fun `o valor aceita virgula e vazio quer dizer sem valor`() {
        assertEquals(59.9, StaffCheckinUiState(amount = "59,90").amountValue)
        assertEquals(0.0, StaffCheckinUiState(amount = "").amountValue)
        assertNull(StaffCheckinUiState(amount = "x").amountValue)
    }

    @Test
    fun `o QR do balcao vira o codigo e o resto passa como esta`() {
        assertEquals("ABC123XY", MemberCodeParser.parse("pixstop://u/abc123xy"))
        assertEquals("Ana", MemberCodeParser.parse("Ana"))
    }
}
