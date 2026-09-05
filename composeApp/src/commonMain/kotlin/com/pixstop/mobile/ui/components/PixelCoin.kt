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
 * A moeda de pixel do sistema, desenhada bloco a bloco.
 *
 * É o mesmo desenho do `PixelCoinIcon.vue` da web — duas moedas quadradas
 * sobrepostas com um "P" na da frente — em uma grade de 16×16 para os blocos
 * caírem sempre em pixels inteiros.
 */
@Composable
fun PixelCoin(modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Canvas(modifier = modifier.then(Modifier.size(size))) {
        val unit = this.size.width / GRID

        fun block(x: Int, y: Int, width: Int, height: Int, color: Color) {
            drawRect(
                color = color,
                topLeft = Offset(x * unit, y * unit),
                size = Size(width * unit, height * unit),
            )
        }

        // Moeda de trás, deslocada para dar profundidade.
        block(4, 0, 12, 12, EdgeDark)
        block(5, 1, 10, 10, BackFill)
        block(5, 1, 10, 2, BackShine)

        // Moeda da frente.
        block(0, 4, 12, 12, EdgeDark)
        block(1, 5, 10, 10, FrontFill)
        block(1, 5, 10, 3, FrontShine)

        // O "P" gravado na moeda da frente.
        block(4, 7, 1, 6, Engraving)
        block(5, 7, 3, 1, Engraving)
        block(8, 8, 1, 2, Engraving)
        block(5, 10, 3, 1, Engraving)
    }
}

private const val GRID = 16f

private val EdgeDark = Color(0xFFB45309)
private val BackFill = Color(0xFFD97706)
private val BackShine = Color(0x80EAB308)
private val FrontFill = Color(0xFFFACC15)
private val FrontShine = Color(0x99FDE047)
private val Engraving = Color(0xFF92400E)
