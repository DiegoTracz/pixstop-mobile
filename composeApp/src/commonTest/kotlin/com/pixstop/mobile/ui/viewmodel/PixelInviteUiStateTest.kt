package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.core.network.apiJson
import com.pixstop.mobile.data.remote.dto.PixelInviteDto
import com.pixstop.mobile.data.repository.toDomain
import com.pixstop.mobile.domain.model.PixelInvite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * O botão "Receber" só acende com o convite aberto e, quando ele foi por
 * telefone, com um número que pareça um número.
 */
class PixelInviteUiStateTest {

    private fun convite(needsPhone: Boolean = true, status: String = "sent") = PixelInvite(
        code = "ABC123XY",
        pixels = 500,
        message = null,
        recipientName = "Ana",
        needsPhone = needsPhone,
        status = status,
        statusLabel = "Enviado",
        companyId = "1",
        companyName = "Revenda Alfa",
    )

    @Test
    fun `por telefone so aceita com o numero preenchido`() {
        val semNumero = PixelInviteUiState(invite = convite(), isLoading = false)

        assertFalse(semNumero.canAccept)
        assertTrue(semNumero.copy(phone = "(11) 99999-0000").canAccept)
        assertFalse(semNumero.copy(phone = "1199").canAccept)
    }

    @Test
    fun `por e-mail aceita sem telefone e fechado nao aceita nunca`() {
        assertTrue(PixelInviteUiState(invite = convite(needsPhone = false), isLoading = false).canAccept)
        assertFalse(PixelInviteUiState(invite = convite(needsPhone = false, status = "accepted"), isLoading = false).canAccept)
        assertFalse(PixelInviteUiState(invite = convite(needsPhone = false), isAccepting = true).canAccept)
    }

    @Test
    fun `o servidor descreve o convite e o valor vira reais`() {
        val dto = apiJson.decodeFromString<PixelInviteDto>(
            """{"code":"ABC123XY","pixels":1250,"needs_phone":true,"status":"sent","company":{"id":"7","name":"Revenda Alfa"}}""",
        )
        val invite = dto.toDomain()

        assertEquals("R$ 12,50", invite.reais)
        assertEquals("Revenda Alfa", invite.companyName)
        assertTrue(invite.isOpen)
    }
}
