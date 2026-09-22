package com.pixstop.mobile.ui.components

import androidx.compose.ui.text.AnnotatedString
import com.pixstop.mobile.domain.payment.CardValidation
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * O mapa de posições é a razão de a máscara existir aqui e não no
 * `onValueChange`. Errado, ele embaralha o número: cada separador nascido
 * antes do cursor empurra o dígito seguinte para o lugar errado — foi o que
 * aconteceu no emulador antes desta classe existir.
 */
class DigitMaskTest {

    private val cartao = DigitMask(CardValidation::formatCardNumber)
    private val cpf = DigitMask(CardValidation::formatCpf)

    private fun mapping(mask: DigitMask, digits: String) =
        mask.filter(AnnotatedString(digits)).offsetMapping

    @Test
    fun `o texto pintado leva os separadores e o valor nao`() {
        val transformed = cartao.filter(AnnotatedString("4235647728025682"))

        assertEquals("4235 6477 2802 5682", transformed.text.text)
    }

    @Test
    fun `o cursor pula o separador em vez de parar nele`() {
        // Depois do quarto dígito o cursor tem de estar depois do espaço; parar
        // antes faria o quinto dígito entrar do lado errado.
        val map = mapping(cartao, "42356477")

        assertEquals(0, map.originalToTransformed(0))
        assertEquals(4, map.originalToTransformed(3) + 1)
        assertEquals(5, map.originalToTransformed(4))
        assertEquals(8, map.originalToTransformed(7))
    }

    @Test
    fun `o fim do texto continua sendo o fim`() {
        val map = mapping(cartao, "4235")

        assertEquals(4, map.originalToTransformed(4))
    }

    @Test
    fun `a volta conta digitos nao caracteres`() {
        val map = mapping(cartao, "42356477")

        assertEquals(4, map.transformedToOriginal(5))
        assertEquals(8, map.transformedToOriginal(9))
        assertEquals(0, map.transformedToOriginal(0))
    }

    @Test
    fun `ida e volta se anulam em toda posicao`() {
        // É a propriedade que o campo exige: sem ela o cursor anda sozinho.
        val digits = "52998224725"
        val map = mapping(cpf, digits)

        (0..digits.length).forEach { offset ->
            assertEquals(offset, map.transformedToOriginal(map.originalToTransformed(offset)), "posição $offset")
        }
    }

    @Test
    fun `campo vazio nao quebra o mapa`() {
        val map = mapping(cartao, "")

        assertEquals(0, map.originalToTransformed(0))
        assertEquals(0, map.transformedToOriginal(0))
    }
}
