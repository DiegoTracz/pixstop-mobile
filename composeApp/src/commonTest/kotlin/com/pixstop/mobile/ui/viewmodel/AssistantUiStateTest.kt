package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.model.MascotState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** As regras da tela do assistente, sem rede (ASSISTENTE_IA.md, A8). */
class AssistantUiStateTest {

    @Test
    fun `so envia com texto e sem nada em andamento`() {
        assertFalse(AssistantUiState().canSend)
        assertTrue(AssistantUiState(draft = "Oi").canSend)
        assertFalse(AssistantUiState(draft = "Oi", isSending = true).canSend)
        assertFalse(AssistantUiState(draft = "Oi", isUploading = true).canSend)
    }

    @Test
    fun `ate quatro fotos por mensagem`() {
        val three = List(3) { PendingPhoto(it.toLong(), byteArrayOf()) }
        assertTrue(AssistantUiState(photos = three).canAddPhoto)
        assertFalse(AssistantUiState(photos = three + PendingPhoto(9, byteArrayOf())).canAddPhoto)
    }

    @Test
    fun `o mascote pensa enquanto a IA responde`() {
        assertEquals(MascotState.Idle, AssistantUiState().mascot)
        assertEquals(MascotState.Loading, AssistantUiState(isSending = true).mascot)
        assertEquals(MascotState.Loading, AssistantUiState(isUploading = true).mascot)
    }
}
