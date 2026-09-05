package com.pixstop.mobile.core.text

/**
 * A saudação da tela de início.
 *
 * As faixas seguem o uso corrente em português: a tarde começa ao meio-dia e a
 * noite às dezoito horas. Antes das cinco ainda é madrugada, mas quem está de
 * pé a essa hora lê "boa noite" sem estranhar — dizer "bom dia" às três da
 * manhã é que soaria errado.
 */
fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "Bom dia"
    in 12..17 -> "Boa tarde"
    else -> "Boa noite"
}
