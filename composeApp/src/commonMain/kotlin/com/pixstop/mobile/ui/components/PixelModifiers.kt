package com.pixstop.mobile.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.theme.PixColors

/**
 * Sombra pixel sólida sem blur — estilo retro 8-bit.
 */
fun Modifier.pixelShadow(
    color: Color = PixColors.Cyan,
    offsetX: Dp = 4.dp,
    offsetY: Dp = 4.dp
) = this.drawBehind {
    drawRect(
        color = color,
        topLeft = Offset(offsetX.toPx(), offsetY.toPx()),
        size = size
    )
}

/**
 * Glow neon — blur suave ao redor do componente.
 */
fun Modifier.neonGlow(
    color: Color = PixColors.Cyan,
    radius: Dp = 20.dp,
    alpha: Float = 0.4f
) = this.drawBehind {
    val r = radius.toPx()
    drawRect(
        color = color.copy(alpha = alpha),
        topLeft = Offset(-r, -r),
        size = Size(size.width + r * 2, size.height + r * 2),
        style = Fill
    )
}

/**
 * Overlay de scanlines CRT — linhas horizontais semitransparentes.
 */
fun Modifier.scanlines() = this.drawWithContent {
    drawContent()
    val lineHeight = 2.dp.toPx()
    var y = 0f
    while (y < size.height) {
        y += lineHeight // transparent gap
        drawRect(
            color = Color.Black.copy(alpha = 0.03f),
            topLeft = Offset(0f, y),
            size = Size(size.width, lineHeight)
        )
        y += lineHeight
    }
}

/**
 * Grid pattern decorativo — linhas ciano finas simulando tela digital.
 */
fun Modifier.gridPattern(
    color: Color = PixColors.Cyan.copy(alpha = 0.03f),
    spacing: Dp = 40.dp
) = this.drawBehind {
    val s = spacing.toPx()
    val lineWidth = 1.dp.toPx()
    // Vertical lines
    var x = 0f
    while (x < size.width) {
        drawRect(
            color = color,
            topLeft = Offset(x, 0f),
            size = Size(lineWidth, size.height)
        )
        x += s
    }
    // Horizontal lines
    var y = 0f
    while (y < size.height) {
        drawRect(
            color = color,
            topLeft = Offset(0f, y),
            size = Size(size.width, lineWidth)
        )
        y += s
    }
}

/**
 * Borda pixel sólida 2dp — cantos retos.
 */
fun Modifier.pixelBorder(
    color: Color = PixColors.Cyan,
    width: Dp = 2.dp
) = this.drawBehind {
    val bw = width.toPx()
    drawRect(color, Offset.Zero, Size(size.width, bw))
    drawRect(color, Offset(0f, size.height - bw), Size(size.width, bw))
    drawRect(color, Offset.Zero, Size(bw, size.height))
    drawRect(color, Offset(size.width - bw, 0f), Size(bw, size.height))
}


