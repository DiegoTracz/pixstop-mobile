package com.pixstop.mobile.domain.model

/**
 * Um convite com pixels: o presente que cadastra. Quem abre o link vê a
 * empresa e o valor; para receber, entra e — se o convite foi por telefone —
 * confirma o número que o recebeu.
 */
data class PixelInvite(
    val code: String,
    val pixels: Int,
    val message: String?,
    val recipientName: String?,
    val needsPhone: Boolean,
    val status: String,
    val statusLabel: String?,
    val companyId: String,
    val companyName: String,
) {
    val isOpen: Boolean get() = status == "sent"

    /** 1 pixel = R$ 0,01. */
    val reais: String get() {
        val cents = pixels % 100
        return "R$ ${pixels / 100},${cents.toString().padStart(2, '0')}"
    }
}
