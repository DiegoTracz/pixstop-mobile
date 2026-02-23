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
 * Logo Pixel Art "P" do PixStop.
 * Grid 24x24 — blocos 4x4 desenhados com Canvas.
 */
@Composable
fun PixelLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    color: Color = PixColors.Cyan,
    accentAlpha: Float = 0.6f
) {
    Canvas(modifier = modifier.size(size)) {
        val canvasSize = this.size.minDimension
        val scale = canvasSize / 24f
        val blockSize = 4f * scale

        fun drawBlock(x: Float, y: Float, alpha: Float = 1f) {
            drawRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(x * scale, y * scale),
                size = Size(blockSize, blockSize)
            )
        }

        // Row y=2: blocos x=4, x=8, x=12
        drawBlock(4f, 2f)
        drawBlock(8f, 2f)
        drawBlock(12f, 2f)

        // Row y=6: blocos x=4, x=12
        drawBlock(4f, 6f)
        drawBlock(12f, 6f)

        // Row y=10: blocos x=4, x=8, x=12
        drawBlock(4f, 10f)
        drawBlock(8f, 10f)
        drawBlock(12f, 10f)

        // Row y=14: bloco x=4
        drawBlock(4f, 14f)

        // Row y=18: bloco x=4 + accent x=16
        drawBlock(4f, 18f)
        drawBlock(16f, 18f, alpha = accentAlpha)
    }
}

