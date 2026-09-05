package com.pixstop.mobile.domain.payment

/**
 * O que dá para conferir no aparelho antes de mandar o cartão para o gateway.
 *
 * Não é segurança — quem decide se o cartão presta é o emissor. É para não
 * gastar uma ida à rede, e um "cartão recusado" no rosto da pessoa, por um
 * dígito digitado errado.
 */
object CardValidation {

    /**
     * Algoritmo de Luhn, o dígito verificador que todo cartão carrega.
     *
     * Pega quase todo erro de digitação — trocar dois números de lugar, errar
     * um dígito — sem consultar ninguém.
     */
    fun isValidCardNumber(value: String): Boolean {
        val digits = value.digitsOnly()
        if (digits.length < 13 || digits.length > 19) {
            return false
        }

        var sum = 0
        var double = false

        for (index in digits.lastIndex downTo 0) {
            var digit = digits[index] - '0'

            if (double) {
                digit *= 2
                if (digit > 9) {
                    digit -= 9
                }
            }

            sum += digit
            double = !double
        }

        return sum % 10 == 0
    }

    /**
     * CPF pelos dois dígitos verificadores.
     *
     * Os repetidos (111.111.111-11 e companhia) passam na conta mas não são
     * CPF de ninguém — daí a checagem à parte.
     */
    fun isValidCpf(value: String): Boolean {
        val digits = value.digitsOnly()
        if (digits.length != 11 || digits.all { it == digits[0] }) {
            return false
        }

        fun checkDigit(upTo: Int): Int {
            val weightStart = upTo + 1
            val sum = (0 until upTo).sumOf { (digits[it] - '0') * (weightStart - it) }
            val rest = sum * 10 % 11

            return if (rest == 10) 0 else rest
        }

        return checkDigit(9) == digits[9] - '0' && checkDigit(10) == digits[10] - '0'
    }

    /**
     * A validade ainda não passou.
     *
     * O mês de vencimento vale até o último dia dele, então um cartão que vence
     * neste mês ainda paga hoje.
     */
    fun isValidExpiry(month: Int, year: Int, currentMonth: Int, currentYear: Int): Boolean {
        if (month !in 1..12) {
            return false
        }

        val fullYear = if (year < 100) 2000 + year else year

        return fullYear > currentYear || (fullYear == currentYear && month >= currentMonth)
    }

    /** O código de segurança tem 3 dígitos, ou 4 na American Express. */
    fun isValidSecurityCode(value: String, cardNumber: String): Boolean {
        val digits = value.digitsOnly()
        val expected = if (brandOf(cardNumber) == CardBrand.Amex) 4 else 3

        return digits.length == expected
    }

    /** Nome impresso: pelo menos duas palavras, como vem no cartão. */
    fun isValidHolderName(value: String): Boolean =
        value.trim().split(" ").filter { it.isNotBlank() }.size >= 2

    /**
     * A bandeira, pelos prefixos que não mudam.
     *
     * Só duas coisas dependem disto, e nenhuma é a cobrança: o tamanho do
     * código de segurança — que só a Amex muda — e um rótulo na tela. Quem
     * identifica a bandeira de verdade é o gateway, que tem a tabela de BIN
     * atualizada; a nossa nunca estaria. Por isso o desconhecido é uma resposta
     * legítima aqui, e não um erro: mostra "cartão" e segue.
     */
    fun brandOf(cardNumber: String): CardBrand {
        val digits = cardNumber.digitsOnly()

        return when {
            digits.take(2) in listOf("34", "37") -> CardBrand.Amex
            digits.startsWith("4") -> CardBrand.Visa
            digits.take(2).toIntOrNull() in 51..55 -> CardBrand.Master
            digits.take(4).toIntOrNull() in 2221..2720 -> CardBrand.Master
            else -> CardBrand.Unknown
        }
    }

    /** `4111 1111 1111 1111`, em grupos de quatro. */
    fun formatCardNumber(value: String): String =
        value.digitsOnly().take(19).chunked(4).joinToString(" ")

    /** `123.456.789-09` */
    fun formatCpf(value: String): String {
        val digits = value.digitsOnly().take(11)

        return buildString {
            digits.forEachIndexed { index, digit ->
                when (index) {
                    3, 6 -> append('.')
                    9 -> append('-')
                }
                append(digit)
            }
        }
    }

    /** `12/30` */
    fun formatExpiry(value: String): String {
        val digits = value.digitsOnly().take(4)

        return if (digits.length <= 2) digits else "${digits.take(2)}/${digits.drop(2)}"
    }

    fun String.digitsOnly(): String = filter { it.isDigit() }
}

enum class CardBrand(val label: String) {
    Visa("visa"),
    Master("master"),
    Amex("amex"),
    Unknown("cartão"),
}
