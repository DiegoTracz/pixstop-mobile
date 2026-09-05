package com.pixstop.mobile.domain.payment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Conferir o cartão no aparelho não é segurança — quem decide é o emissor. É
 * para não gastar uma ida à rede, e um "cartão recusado" no rosto da pessoa,
 * por um dígito trocado.
 */
class CardValidationTest {

    @Test
    fun `numero valido passa pelo digito verificador`() {
        // Números de teste públicos do MercadoPago.
        listOf("4235 6477 2802 5682", "5031 4332 1540 6351", "3711 803032 57522")
            .forEach { assertTrue(CardValidation.isValidCardNumber(it), it) }
    }

    @Test
    fun `um digito trocado nao passa`() {
        assertFalse(CardValidation.isValidCardNumber("4235 6477 2802 5683"))
    }

    @Test
    fun `dois digitos invertidos nao passam`() {
        // É o erro de digitação mais comum, e é o que o Luhn foi feito para pegar.
        assertFalse(CardValidation.isValidCardNumber("4235 6477 2802 5628"))
    }

    @Test
    fun `numero curto ou longo demais nao e cartao`() {
        assertFalse(CardValidation.isValidCardNumber("4111 1111"))
        assertFalse(CardValidation.isValidCardNumber("4".repeat(20)))
        assertFalse(CardValidation.isValidCardNumber(""))
    }

    @Test
    fun `cpf valido passa pelos dois digitos`() {
        listOf("529.982.247-25", "52998224725", "111.444.777-35")
            .forEach { assertTrue(CardValidation.isValidCpf(it), it) }
    }

    @Test
    fun `cpf de digitos repetidos nao vale`() {
        // Passa na conta dos verificadores e não é CPF de ninguém.
        (0..9).forEach { digit ->
            assertFalse(CardValidation.isValidCpf(digit.toString().repeat(11)), "$digit".repeat(11))
        }
    }

    @Test
    fun `cpf com digito verificador errado nao passa`() {
        assertFalse(CardValidation.isValidCpf("529.982.247-26"))
        assertFalse(CardValidation.isValidCpf("529982247"))
    }

    @Test
    fun `o cartao vale ate o ultimo dia do mes de vencimento`() {
        // Vencer "em setembro" quer dizer que setembro inteiro ainda paga.
        assertTrue(CardValidation.isValidExpiry(month = 9, year = 26, currentMonth = 9, currentYear = 2026))
        assertFalse(CardValidation.isValidExpiry(month = 8, year = 26, currentMonth = 9, currentYear = 2026))
        assertTrue(CardValidation.isValidExpiry(month = 1, year = 27, currentMonth = 12, currentYear = 2026))
    }

    @Test
    fun `mes fora de 1 a 12 nao e validade`() {
        assertFalse(CardValidation.isValidExpiry(0, 30, 9, 2026))
        assertFalse(CardValidation.isValidExpiry(13, 30, 9, 2026))
    }

    @Test
    fun `o codigo de seguranca da amex tem quatro digitos`() {
        val amex = "3711 803032 57522"
        val visa = "4235 6477 2802 5682"

        assertTrue(CardValidation.isValidSecurityCode("1234", amex))
        assertFalse(CardValidation.isValidSecurityCode("123", amex))
        assertTrue(CardValidation.isValidSecurityCode("123", visa))
        assertFalse(CardValidation.isValidSecurityCode("1234", visa))
    }

    @Test
    fun `o nome do titular tem sobrenome`() {
        assertTrue(CardValidation.isValidHolderName("ANA COSTA"))
        assertFalse(CardValidation.isValidHolderName("ANA"))
        assertFalse(CardValidation.isValidHolderName("   "))
    }

    @Test
    fun `a bandeira sai do inicio do numero`() {
        assertEquals(CardBrand.Visa, CardValidation.brandOf("4235647728025682"))
        assertEquals(CardBrand.Master, CardValidation.brandOf("5555555555554444"))
        assertEquals(CardBrand.Master, CardValidation.brandOf("2221000000000009"))
        assertEquals(CardBrand.Amex, CardValidation.brandOf("371180303257522"))
    }

    @Test
    fun `bandeira que nao reconhecemos nao trava nada`() {
        // A tabela de BIN é do gateway, não nossa: 5031 é Mastercard no Brasil
        // e não está nas faixas publicadas. Cair no desconhecido custa um
        // rótulo genérico na tela, e o cartão paga do mesmo jeito.
        assertEquals(CardBrand.Unknown, CardValidation.brandOf("5031433215406351"))
        assertEquals(CardBrand.Unknown, CardValidation.brandOf("9999"))
        assertTrue(CardValidation.isValidCardNumber("5031 4332 1540 6351"))
        assertTrue(CardValidation.isValidSecurityCode("123", "5031433215406351"))
    }

    @Test
    fun `as mascaras acompanham a digitacao`() {
        assertEquals("4235 6477 2802 5682", CardValidation.formatCardNumber("4235647728025682"))
        assertEquals("4235 64", CardValidation.formatCardNumber("423564"))
        assertEquals("529.982.247-25", CardValidation.formatCpf("52998224725"))
        assertEquals("529.98", CardValidation.formatCpf("52998"))
        assertEquals("12/30", CardValidation.formatExpiry("1230"))
        assertEquals("12", CardValidation.formatExpiry("12"))
    }

    @Test
    fun `a mascara ignora o que nao e digito`() {
        assertEquals("4235 6477", CardValidation.formatCardNumber("4235-6477abc"))
        assertEquals("529.982", CardValidation.formatCpf("529.982"))
    }
}
