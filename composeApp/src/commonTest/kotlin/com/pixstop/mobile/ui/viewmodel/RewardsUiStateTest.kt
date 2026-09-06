package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.core.network.apiJson
import com.pixstop.mobile.data.remote.dto.RewardsPageDto
import com.pixstop.mobile.data.remote.dto.VoucherDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Os vouchers válidos vêm primeiro — é o que a pessoa vai mostrar — e o QR
 * de voucher vira só o código.
 */
class RewardsUiStateTest {

    @Test
    fun `os validos vem primeiro`() {
        val state = RewardsUiState(
            vouchers = listOf(
                VoucherDto(id = 1, code = "AAAA1111", rewardName = "Corte", status = "used"),
                VoucherDto(id = 2, code = "BBBB2222", rewardName = "Barba", status = "open"),
            ),
        )

        assertEquals(listOf(2L, 1L), state.sortedVouchers.map { it.id })
    }

    @Test
    fun `o servidor descreve o catalogo com o que da para pagar`() {
        val page = apiJson.decodeFromString<RewardsPageDto>(
            """{"available":300,"rewards":[{"id":1,"name":"Corte grátis","pixels":500,"affordable":false,"missing":200}],"vouchers":[]}""",
        )

        assertEquals(300, page.available)
        assertEquals(200, page.rewards.single().missing)
        assertTrue(page.vouchers.isEmpty())
    }

    @Test
    fun `o QR do voucher vira o codigo`() {
        assertEquals("ABC123XY", VoucherCodeParser.parse("pixstop://v/abc123xy"))
        assertEquals("ABC123XY", VoucherCodeParser.parse("abc123xy"))
    }
}
