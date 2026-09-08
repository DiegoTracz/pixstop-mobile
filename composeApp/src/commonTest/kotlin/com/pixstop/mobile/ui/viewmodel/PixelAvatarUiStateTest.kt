package com.pixstop.mobile.ui.viewmodel

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * O avatar em pixel art tem uma regra que importa mais que as outras: gerar
 * não é escolher. Enquanto ninguém aplicar, a foto do perfil não muda.
 */
class PixelAvatarUiStateTest {

    @Test
    fun `so ha o que aplicar depois de a previa chegar`() {
        val vazio = PixelAvatarUiState()

        assertFalse(vazio.hasPreview)
        assertTrue(vazio.copy(preview = "data:image/png;base64,AAAA").hasPreview)
    }

    @Test
    fun `abrir a camera limpa o recado anterior`() {
        val depoisDeUmErro = PixelAvatarUiState(error = "Não consegui gerar o avatar.", message = "Avatar atualizado.")

        val abrindo = depoisDeUmErro.copy(isCameraOpen = true, error = null, message = null)

        assertTrue(abrindo.isCameraOpen)
        assertNull(abrindo.error)
        assertNull(abrindo.message)
    }

    @Test
    fun `descartar a previa nao deixa nada meio aplicado`() {
        val comPrevia = PixelAvatarUiState(preview = "data:image/png;base64,AAAA", error = "algo")

        val descartada = comPrevia.copy(preview = null, error = null, message = null)

        assertFalse(descartada.hasPreview)
        assertNull(descartada.error)
    }
}
