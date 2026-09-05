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
 * Símbolo da marca: o "P" em pixel art com a sombra sólida deslocada.
 *
 * A sombra é a mesma assinatura dos botões do sistema, e é o que distingue
 * este "P" de um "P" qualquer. O desenho é o mesmo do favicon e do ícone do
 * site — dois "P" diferentes na mesma marca fariam o app e o site parecerem
 * produtos distintos.
 */
@Composable
fun PixelLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    color: Color = PixColors.Cyan,
    shadowColor: Color = PixColors.CyanShadow,
) {
    Canvas(modifier = modifier.size(size)) {
        val cell = this.size.minDimension / GRID

        fun block(x: Int, y: Int, blockColor: Color) {
            drawRect(
                color = blockColor,
                topLeft = Offset(x * cell, y * cell),
                size = Size(cell, cell),
            )
        }

        val left = (GRID - GLYPH[0].length - 1) / 2
        val top = (GRID - GLYPH.size - 1) / 2

        // A sombra vai primeiro: o símbolo passa por cima dela.
        GLYPH.forEachIndexed { row, line ->
            line.forEachIndexed { column, cellChar ->
                if (cellChar == 'X') {
                    block(left + column + 1, top + row + 1, shadowColor)
                }
            }
        }

        GLYPH.forEachIndexed { row, line ->
            line.forEachIndexed { column, cellChar ->
                if (cellChar == 'X') {
                    block(left + column, top + row, color)
                }
            }
        }
    }
}

/** Lado da grade. O mesmo do `brand:mark` no servidor. */
private const val GRID = 16

/**
 * O "P" numa grade de 12×10.
 *
 * Desenhado à mão em vez de tirado da fonte: a fonte pixelada tem espessura
 * fina demais para sobreviver aos tamanhos pequenos.
 */
private val GLYPH = listOf(
    "XXXXXXXXX.",
    "XXXXXXXXXX",
    "XXX.....XX",
    "XXX......X",
    "XXX.....XX",
    "XXXXXXXXXX",
    "XXXXXXXXX.",
    "XXX.......",
    "XXX.......",
    "XXX.......",
    "XXX.......",
    "XXX.......",
)
