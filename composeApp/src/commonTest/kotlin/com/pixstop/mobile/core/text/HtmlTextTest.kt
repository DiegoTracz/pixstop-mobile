package com.pixstop.mobile.core.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Os documentos legais chegam em HTML porque o mesmo texto alimenta o site.
 * Mostrá-lo cru na tela de aceite fazia a pessoa ler `<p>` no meio da frase.
 */
class HtmlTextTest {

    @Test
    fun `remove as tags e mantem o texto`() {
        val html = "<h2>1. Objeto</h2><p>Os presentes Termos regulam o uso da <strong>Pixelstop</strong>.</p>"

        val text = HtmlText.toPlainText(html)

        assertFalse(text.contains("<"), text)
        assertTrue(text.contains("1. Objeto"), text)
        assertTrue(text.contains("uso da Pixelstop."), text)
    }

    @Test
    fun `itens de lista viram marcadores`() {
        val html = "<ul><li>Nome completo;</li><li>Endereço de e-mail;</li></ul>"

        val text = HtmlText.toPlainText(html)

        assertTrue(text.contains("• Nome completo;"), text)
        assertTrue(text.contains("• Endereço de e-mail;"), text)
    }

    @Test
    fun `traduz as entidades`() {
        assertEquals("Lei nº 13.709/2018 & LGPD", HtmlText.toPlainText("<p>Lei n&ordm; 13.709/2018 &amp; LGPD</p>"))
    }

    @Test
    fun `nao deixa uma pilha de linhas em branco`() {
        val text = HtmlText.toPlainText("<p>Um</p>\n\n<p></p>\n\n<p>Dois</p>")

        assertEquals("Um\n\nDois", text)
    }

    @Test
    fun `texto sem marcacao passa intacto`() {
        assertEquals("Texto simples.", HtmlText.toPlainText("Texto simples."))
    }
}
