package com.pixstop.mobile.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Mostra separadores sem colocá-los no valor.
 *
 * A tentação é formatar dentro do `onValueChange` — devolver "4235 6477" onde
 * a pessoa digitou "42356477". Não funciona: o campo guarda só a posição do
 * cursor, e cada separador que nasce antes dele desloca tudo um caractere. O
 * dígito seguinte cai no lugar errado e o número sai embaralhado.
 *
 * Aqui o valor continua sendo só dígitos — que é o que vai para o gateway — e
 * os separadores existem apenas na pintura, com o mapa que diz onde o cursor
 * de verdade está.
 */
class DigitMask(private val mask: (String) -> String) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val masked = mask(text.text)

        return TransformedText(
            AnnotatedString(masked),
            object : OffsetMapping {

                /** Onde pintar o cursor que está depois de `offset` dígitos. */
                override fun originalToTransformed(offset: Int): Int {
                    var digits = 0

                    masked.forEachIndexed { index, character ->
                        if (character.isDigit()) {
                            if (digits == offset) {
                                return index
                            }

                            digits++
                        }
                    }

                    return masked.length
                }

                /** Quantos dígitos existem antes do cursor pintado. */
                override fun transformedToOriginal(offset: Int): Int =
                    masked.take(offset.coerceIn(0, masked.length)).count { it.isDigit() }
            },
        )
    }
}
