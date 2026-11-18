package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.model.DomainError
import com.pixstop.mobile.domain.model.Outcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ForgotPasswordUiStateTest {

    @Test
    fun soEnviaComUmEmailDeVerdade() {
        assertFalse(ForgotPasswordUiState(email = "").canSend)
        assertFalse(ForgotPasswordUiState(email = "diego@").canSend)
        assertTrue(ForgotPasswordUiState(email = " diego@pixelstop.com.br ").canSend)
        assertFalse(ForgotPasswordUiState(email = "diego@pixelstop.com.br", isSending = true).canSend)
    }

    @Test
    fun linkEnviadoMostraParaOndeFoi() {
        val state = ForgotPasswordUiState(email = " diego@pixelstop.com.br ", isSending = true).withResult(Outcome.Success(Unit))

        assertEquals("diego@pixelstop.com.br", state.sentTo)
        assertFalse(state.isSending)
        assertNull(state.error)
    }

    @Test
    fun falhaDoServidorMantemOFormularioComOAviso() {
        val state = ForgotPasswordUiState(email = "x@y.com", isSending = true)
            .withResult(Outcome.Failure(DomainError.Server(500, "O servidor não respondeu como esperado.")))

        assertNull(state.sentTo)
        assertEquals("O servidor não respondeu como esperado.", state.error)
    }

    @Test
    fun pedirDeNovoLogoEmSeguidaPedeParaEsperar() {
        val state = ForgotPasswordUiState(email = "x@y.com").withResult(Outcome.Failure(DomainError.RateLimited("Muitas tentativas.")))

        assertEquals("Você já pediu há pouco. Espere um minuto e tente de novo.", state.error)
    }
}
