package com.pixstop.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixstop.mobile.domain.model.PrizeSpinKind
import com.pixstop.mobile.domain.model.PrizeWheel
import com.pixstop.mobile.domain.model.PrizeWheelSlice
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.floor
import kotlin.math.hypot

private const val GRID = 64

// Cores bem diferentes entre si, da marca: a legenda precisa dizer em qual
// fatia a roda parou só pela cor. As mesmas da web.
private val GoldInk = listOf(Color(0xFF015DFA), Color(0xFFFA00FB), Color(0xFF3DDC97), Color(0xFF9CC0FF))
private val PurchaseInk = listOf(Color(0xFF3DDC97), Color(0xFF015DFA), Color(0xFFFA00FB), Color(0xFF9CC0FF), Color(0xFF0E7C53))
private val Jackpot = Color(0xFFFFC93C)
private val Rim = Color(0xFF060A14)

/**
 * A roda em pixel art, numa grade de 64×64 como a da web: cada fatia do mesmo
 * tamanho (o peso de cada uma fica no servidor), o prêmio maior em dourado e
 * o ponteiro em cima. `angle` é o giro em radianos.
 */
@Composable
fun PrizeWheelArt(
    slices: List<PrizeWheelSlice>,
    kind: PrizeSpinKind,
    angle: Float,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    showLabels: Boolean = true,
) {
    val count = slices.size.coerceAtLeast(1)
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontFamily = PixTypography.pixelFontFamily, fontSize = (size.value / 22f).sp)

    Canvas(modifier = modifier.size(size)) {
        val cell = this.size.minDimension / GRID
        val center = GRID / 2f
        val radius = GRID / 2f - 3f
        val full = (2 * PI).toFloat()

        for (y in 0 until GRID) {
            for (x in 0 until GRID) {
                val dx = x + 0.5f - center
                val dy = y + 0.5f - center
                val distance = hypot(dx, dy)

                if (distance > radius + 2) continue

                val color = when {
                    distance > radius -> Rim
                    distance < 4f -> Color.White
                    else -> {
                        val turn = (((atan2(dy, dx) - angle) % full) + full) % full
                        val index = floor(turn / full * count).toInt().coerceIn(0, count - 1)
                        sliceColor(kind, index, slices.getOrNull(index))
                    }
                }

                drawRect(color = color, topLeft = Offset(x * cell, y * cell), size = Size(cell + 0.5f, cell + 0.5f))
            }
        }

        // O prêmio escrito em cada fatia, girando junto com a roda.
        val labelRadius = radius * cell * 0.6f
        if (showLabels) slices.forEachIndexed { index, slice ->
            // De pé, no meio da fatia: número de lado ou de cabeça para baixo
            // não se lê com a roda girando.
            val middle = angle + (index + 0.5f) * full / count
            val text = measurer.measure(shortLabel(slice), labelStyle.copy(color = inkOn(sliceColor(kind, index, slice))))
            val x = center * cell + labelRadius * cos(middle)
            val y = center * cell + labelRadius * sin(middle)

            drawText(textLayoutResult = text, topLeft = Offset(x - text.size.width / 2f, y - text.size.height / 2f))
        }

        // O ponteiro, em cima.
        fun pixel(x: Float, y: Float, width: Float, height: Float, color: Color) =
            drawRect(color = color, topLeft = Offset(x * cell, y * cell), size = Size(width * cell, height * cell))

        pixel(center - 3, 0f, 7f, 2f, Rim)
        pixel(center - 2, 2f, 5f, 2f, Rim)
        pixel(center - 1, 4f, 3f, 2f, Rim)
        pixel(center - 2, 0f, 5f, 1f, Jackpot)
        pixel(center - 1, 1f, 3f, 2f, Jackpot)
        pixel(center, 3f, 1f, 2f, Jackpot)
    }
}

/** O que vai escrito na fatia: o número de moedas, ou "MAIOR" no prêmio maior. */
fun shortLabel(slice: PrizeWheelSlice): String = if (slice.isJackpot) "MAIOR" else slice.pixels.toString()

/** Texto escuro em fatia clara, claro em fatia escura. */
private fun inkOn(color: Color): Color =
    if (0.299f * color.red + 0.587f * color.green + 0.114f * color.blue > 0.59f) Rim else Color.White

/** A cor da fatia na roda, a mesma que a legenda mostra. */
fun sliceColor(kind: PrizeSpinKind, index: Int, slice: PrizeWheelSlice?): Color {
    val ink = if (kind == PrizeSpinKind.Gold) GoldInk else PurchaseInk
    return if (slice?.isJackpot == true) Jackpot else ink[index % ink.size]
}

/**
 * A legenda da roda: cada prêmio com o quadrado da cor da fatia dele, para a
 * pessoa saber onde a roda parou.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PrizeWheelLegend(
    slices: List<PrizeWheelSlice>,
    kind: PrizeSpinKind,
    modifier: Modifier = Modifier,
    highlight: Int? = null,
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        slices.forEachIndexed { index, slice ->
            val color = sliceColor(kind, index, slice)
            val lit = highlight == index

            Row(
                modifier = Modifier
                    .border(if (lit) 2.dp else 1.dp, if (lit || slice.isJackpot) color else PixColors.Gray700)
                    .background(if (lit) color.copy(alpha = 0.18f) else Color.Transparent)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.foundation.layout.Box(modifier = Modifier.size(12.dp).background(color).border(1.dp, Rim))
                Text(
                    text = slice.label,
                    style = PixTypography.caption,
                    color = if (slice.isJackpot) Jackpot else PixColors.Gray100,
                )
            }
        }
    }
}

/** O ângulo em que a fatia `index` fica sob o ponteiro, que está em cima. */
fun restingAngle(index: Int, count: Int): Float {
    val width = (2 * PI / count.coerceAtLeast(1)).toFloat()
    return (-PI / 2).toFloat() - (index + 0.5f) * width
}

/**
 * O cartão da Home: quantos giros a pessoa tem. Abre a tela da roleta.
 */
@Composable
fun PrizeWheelTeaser(
    wheel: PrizeWheel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, PixColors.Yellow)
            .background(PixColors.Darker)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PrizeWheelArt(slices = wheel.slicesOf(wheel.nextKind), kind = wheel.nextKind, angle = restingAngle(0, wheel.slicesOf(wheel.nextKind).size), size = 72.dp, showLabels = false)

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "ROLETA", style = PixTypography.badgeText, color = PixColors.Gray400)
            Text(text = wheel.name, style = PixTypography.sectionTitle, color = PixColors.Yellow)
            Text(
                text = when {
                    wheel.goldSpins > 0 -> "Você tem ${wheel.goldSpins} ${if (wheel.goldSpins == 1) "giro de ouro" else "giros de ouro"}. Toque para girar."
                    wheel.purchaseSpins > 0 -> "Você tem ${wheel.purchaseSpins} ${if (wheel.purchaseSpins == 1) "giro" else "giros"} de compra. Toque para girar."
                    else -> "Pague uma compra com moedas e ganhe um giro por dia."
                },
                style = PixTypography.caption,
                color = PixColors.Gray100,
            )
        }
    }
}
