package com.pixstop.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A moeda do sistema é um pixel: o mesmo quadrado amarelo que fecha o símbolo
 * da marca.
 *
 * Eram duas moedas douradas sobrepostas com um "P" gravado, em seis tons fora
 * da paleta. Parecia dinheiro de jogo e não tinha relação nenhuma com a logo.
 * Agora é a mesma coisa em todo lugar — no ponto do símbolo, no saldo e no
 * extrato — e é isso que dá ao quadrado amarelo o valor de "isto é dinheiro".
 *
 * Sem borda, sem cifrão, sem gravação: um pixel é um pixel. O brilho no canto
 * é o que o faz parecer aceso em vez de pintado. É o mesmo desenho do
 * `PixelCoinIcon.vue` da web.
 *
 * @param stacked dois pixels empilhados, para quando o número é saldo e não
 *   unidade.
 */
@Composable
fun PixelCoin(modifier: Modifier = Modifier, size: Dp = 16.dp, stacked: Boolean = false) {
    Canvas(modifier = modifier.then(Modifier.size(size))) {
        val unit = this.size.width / GRID

        fun block(x: Int, y: Int, width: Int, height: Int, color: Color) {
            drawRect(
                color = color,
                topLeft = Offset(x * unit, y * unit),
                size = Size(width * unit, height * unit),
            )
        }

        if (stacked) {
            // O de trás, apagado: dois pixels dizem saldo, um diz unidade.
            block(5, 0, 11, 11, PixelDim)
            block(0, 5, 11, 11, Pixel)
            block(0, 5, 3, 3, Shine)
        } else {
            block(2, 2, 12, 12, Pixel)
            block(2, 2, 3, 3, Shine)
        }
    }
}

private const val GRID = 16f

/** O amarelo do pixel, o brilho do canto e a versão apagada do de trás. */
private val Pixel = Color(0xFFFFC93C)
private val Shine = Color(0xFFFFF1A8)
private val PixelDim = Color(0x73FFC93C)
