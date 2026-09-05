package com.pixstop.mobile.domain.payment

import com.pixstop.mobile.domain.payment.CardValidation.digitsOnly

/**
 * O cartão como a pessoa digitou.
 *
 * Vive só o tempo de virar token: nada aqui é guardado no aparelho nem enviado
 * ao nosso servidor. O número vai do aparelho direto ao gateway, que devolve um
 * token de uso único — é ele que o nosso backend vê.
 */
data class CardInput(
    val number: String,
    val expiry: String,
    val securityCode: String,
    val holderName: String,
    val documentNumber: String,
    val documentType: String = "CPF",
) {
    val digits: String get() = number.digitsOnly()

    val expiryMonth: Int get() = expiry.digitsOnly().take(2).toIntOrNull() ?: 0

    /** O gateway quer o ano com quatro dígitos; a tela pede dois. */
    val expiryYear: Int
        get() = expiry.digitsOnly().drop(2).toIntOrNull()?.let { if (it < 100) 2000 + it else it } ?: 0

    val document: String get() = documentNumber.digitsOnly()

    val brand: CardBrand get() = CardValidation.brandOf(number)

    /**
     * O que está errado, campo a campo, para a tela marcar onde consertar.
     *
     * Vazio quer dizer que dá para tentar tokenizar — não que o cartão exista.
     */
    fun errors(currentMonth: Int, currentYear: Int): Map<String, String> = buildMap {
        if (!CardValidation.isValidCardNumber(number)) {
            put(FIELD_NUMBER, "Confira o número do cartão.")
        }

        if (!CardValidation.isValidExpiry(expiryMonth, expiryYear, currentMonth, currentYear)) {
            put(FIELD_EXPIRY, "Validade inválida ou vencida.")
        }

        if (!CardValidation.isValidSecurityCode(securityCode, number)) {
            val size = if (brand == CardBrand.Amex) 4 else 3
            put(FIELD_CVV, "O código de segurança tem $size dígitos.")
        }

        if (!CardValidation.isValidHolderName(holderName)) {
            put(FIELD_HOLDER, "Escreva o nome como está no cartão.")
        }

        if (!CardValidation.isValidCpf(documentNumber)) {
            put(FIELD_DOCUMENT, "CPF inválido.")
        }
    }

    /**
     * Quais campos mudaram em relação a [other].
     *
     * A tela usa isso para apagar o aviso do campo que está sendo corrigido, e
     * só dele: limpar todos a cada tecla faria a lista de erros piscar.
     */
    fun changedFieldsFrom(other: CardInput): Set<String> = buildSet {
        if (number != other.number) add(FIELD_NUMBER)
        if (expiry != other.expiry) add(FIELD_EXPIRY)
        if (securityCode != other.securityCode) add(FIELD_CVV)
        if (holderName != other.holderName) add(FIELD_HOLDER)
        if (documentNumber != other.documentNumber) add(FIELD_DOCUMENT)
    }

    companion object {
        const val FIELD_NUMBER = "number"
        const val FIELD_EXPIRY = "expiry"
        const val FIELD_CVV = "cvv"
        const val FIELD_HOLDER = "holder"
        const val FIELD_DOCUMENT = "document"
    }
}
