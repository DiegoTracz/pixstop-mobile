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
import com.pixstop.mobile.ui.theme.PixColors

/**
 * Símbolo da marca: o P que solta pixels, e o ponto onde eles param.
 *
 * O P nasce de um quadrado e libera blocos pelos dois cantos da direita; os de
 * baixo caem e formam o ponto amarelo na linha de base. É Pixel e Stop no
 * mesmo gesto — a energia sai da letra e para no ponto.
 *
 * O desenho é o mesmo do favicon e do ícone do site, saído da mesma grade:
 * dois P diferentes na mesma marca fariam o app e o site parecerem produtos
 * distintos.
 *
 * **O ponto é sempre amarelo.** A letra aceita a cor que o chamador der, mas o
 * ponto não: ele é o pixel, a mesma coisa que a moeda do sistema, e um pixel
 * de outra cor não quer dizer nada.
 *
 * O logotipo do aplicativo é a palavra inteira; este símbolo existe para onde
 * ela não cabe.
 */
@Composable
fun PixelLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    color: Color = PixColors.Cyan,
) {
    Canvas(modifier = modifier.then(Modifier.size(size))) {
        // O lado maior manda: o desenho é mais alto que largo, e escalar pela
        // largura o deixaria transbordando embaixo.
        val cell = this.size.minDimension / maxOf(GLYPH_WIDTH, GLYPH.size).toFloat()
        val left = (this.size.width - GLYPH_WIDTH * cell) / 2f
        val top = (this.size.height - GLYPH.size * cell) / 2f

        GLYPH.forEachIndexed { row, line ->
            line.forEachIndexed { column, cellChar ->
                val blockColor = when (cellChar) {
                    'c' -> color
                    'y' -> PIXEL
                    'h' -> SHINE
                    else -> null
                }

                if (blockColor != null) {
                    drawRect(
                        color = blockColor,
                        topLeft = Offset(left + column * cell, top + row * cell),
                        size = Size(cell, cell),
                    )
                }
            }
        }
    }
}

/** O amarelo do pixel e o brilho do canto dele, fixos nos dois temas. */
private val PIXEL = Color(0xFFFFC93C)
private val SHINE = Color(0xFFFFF1A8)

/** Largura da grade; a altura é o tamanho da lista. */
private const val GLYPH_WIDTH = 20

/**
 * O símbolo numa grade de 20 por 21, recortada na caixa que ele ocupa.
 *
 * `c` é a letra e os blocos soltos, `y` o ponto e `h` o brilho dele.
 * Desenhado à mão em vez de tirado da fonte: a fonte pixelada tem espessura
 * fina demais para sobreviver aos tamanhos pequenos.
 */
private val GLYPH = listOf(
    "...........cc.......",
    "........c..cc.......",
    "..............c.....",
    "cccccccccc..........",
    "ccccccccccc.....cc..",
    "cccccccccccc....cc..",
    "ccccc....ccc.c......",
    "ccccc....ccc........",
    "ccccc....cc.........",
    "ccccc....ccc.cc.....",
    "cccccccccccc.cc.....",
    "ccccccccccc.....c...",
    "cccccccccc..........",
    "ccccc.............cc",
    "ccccc.............cc",
    "ccccc...............",
    "ccccc............c..",
    "ccccc...............",
    "ccccc.........hy....",
    "ccccc.........yy....",
    "ccccc...............",
)
