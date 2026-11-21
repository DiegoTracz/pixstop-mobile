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
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.PrizeSpinKind
import com.pixstop.mobile.domain.model.PrizeWheel
import com.pixstop.mobile.domain.model.PrizeWheelSlice
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot

private const val GRID = 64

private val GoldInk = listOf(Color(0xFF015DFA), Color(0xFF4D8BFF), Color(0xFF0149C8), Color(0xFF9CC0FF))
private val PurchaseInk = listOf(Color(0xFF0E7C53), Color(0xFF3DDC97), Color(0xFF07402A), Color(0xFFA6F2D2))
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
) {
    val ink = if (kind == PrizeSpinKind.Gold) GoldInk else PurchaseInk
    val count = slices.size.coerceAtLeast(1)

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
                        if (slices.getOrNull(index)?.isJackpot == true) Jackpot else ink[index % ink.size]
                    }
                }

                drawRect(color = color, topLeft = Offset(x * cell, y * cell), size = Size(cell + 0.5f, cell + 0.5f))
            }
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
        PrizeWheelArt(slices = wheel.slicesOf(wheel.nextKind), kind = wheel.nextKind, angle = restingAngle(0, wheel.slicesOf(wheel.nextKind).size), size = 72.dp)

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
