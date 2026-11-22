package com.pixstop.mobile.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import kotlinx.coroutines.delay

private const val COLUMNS = 8
private const val ROWS = 7
private const val EMPTY = -1
private const val FALL_MS_PER_ROW = 60
private const val LAND_PAUSE_MS = 90L
private const val FLASH_MS = 260
private const val COLLAPSE_MS = 160

/** Uma peça da sequência: as casas dela, a coluna onde cai e a cor. */
private class Drop(val cells: List<Pair<Int, Int>>, val column: Int, val color: Int)

/**
 * Sequência fixa, a mesma da web: 14 peças que fecham 7 linhas (uma dupla) e
 * deixam o tabuleiro vazio, então o ciclo recomeça sem corte. Onde cada peça
 * para e quais linhas somem é calculado pela regra do jogo, não está escrito
 * aqui.
 */
private val SEQUENCE = listOf(
    Drop(listOf(0 to 0, 0 to 1, 1 to 1, 2 to 1), 1, 0),
    Drop(listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0), 4, 1),
    Drop(listOf(0 to 0, 0 to 1, 0 to 2, 1 to 2), 6, 5),
    Drop(listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1), 3, 2),
    Drop(listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0), 0, 0),
    Drop(listOf(0 to 0, 1 to 0, 1 to 1, 1 to 2), 4, 5),
    Drop(listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1), 0, 2),
    Drop(listOf(0 to 0, 1 to 0, 1 to 1, 1 to 2), 6, 5),
    Drop(listOf(0 to 1, 0 to 2, 1 to 0, 1 to 1), 2, 3),
    Drop(listOf(0 to 0, 0 to 1, 1 to 0, 2 to 0), 5, 5),
    Drop(listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3), 2, 1),
    Drop(listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2), 3, 4),
    Drop(listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1), 0, 2),
    Drop(listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0), 4, 1),
)

private val MESSAGES = listOf(
    "Gerando o mapa da sua empresa",
    "Instalando a geladeira",
    "Carregando os pixels",
    "Calibrando o placar",
    "Chamando os jogadores",
)

/** A linha mais baixa em que a peça cabe caindo reto na coluna dela. */
private fun landingRow(board: IntArray, drop: Drop): Int {
    var top = -4

    while (
        drop.cells.all { (x, y) ->
            val row = y + top + 1
            row < ROWS && (row < 0 || board[row * COLUMNS + x + drop.column] == EMPTY)
        }
    ) {
        top++
    }

    return top
}

private fun fullRows(board: IntArray): List<Int> =
    (0 until ROWS).filter { row -> (0 until COLUMNS).all { board[row * COLUMNS + it] != EMPTY } }

/**
 * Tela de espera enquanto o servidor cria o ambiente da empresa: um jogo de
 * blocos jogando sozinho. As peças caem e se encaixam; a linha que completa
 * pisca e some, e o que estava em cima desce. Fica por cima do formulário e
 * não deixa tocar nele.
 */
@Composable
fun PixelBuildLoader(title: String = "MONTANDO SEU SETUP", modifier: Modifier = Modifier) {
    val palette = listOf(PixColors.Blue, PixColors.Cyan, PixColors.Yellow, PixColors.Pink, PixColors.Green, PixColors.Orange)
    val emptyCell = PixColors.Gray800

    var board by remember { mutableStateOf(IntArray(COLUMNS * ROWS) { EMPTY }) }
    var falling by remember { mutableStateOf<Drop?>(null) }
    var clearing by remember { mutableStateOf(emptyList<Int>()) }
    var lines by remember { mutableIntStateOf(0) }
    var messageIndex by remember { mutableIntStateOf(0) }
    val fallRow = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    val collapse = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            SEQUENCE.forEach { drop ->
                val target = landingRow(board, drop)
                val start = -(drop.cells.maxOf { it.second } + 1)

                fallRow.snapTo(start.toFloat())
                falling = drop
                fallRow.animateTo(
                    targetValue = target.toFloat(),
                    animationSpec = tween((target - start) * FALL_MS_PER_ROW, easing = LinearEasing),
                )

                board = board.copyOf().also { next ->
                    drop.cells.forEach { (x, y) -> next[(y + target) * COLUMNS + x + drop.column] = drop.color }
                }
                falling = null
                delay(LAND_PAUSE_MS)

                val full = fullRows(board)

                if (full.isNotEmpty()) {
                    clearing = full
                    flash.snapTo(0f)
                    flash.animateTo(1f, tween(FLASH_MS, easing = LinearEasing))
                    collapse.snapTo(0f)
                    collapse.animateTo(1f, tween(COLLAPSE_MS, easing = FastOutLinearInEasing))

                    val next = IntArray(COLUMNS * ROWS) { EMPTY }
                    for (row in 0 until ROWS) {
                        if (row in full) continue
                        val shifted = row + full.count { it > row }
                        for (column in 0 until COLUMNS) {
                            next[shifted * COLUMNS + column] = board[row * COLUMNS + column]
                        }
                    }
                    board = next
                    clearing = emptyList()
                    collapse.snapTo(0f)
                    lines += full.size
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2_200)
            messageIndex = (messageIndex + 1) % MESSAGES.size
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PixColors.Darker)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text(text = "LINHAS  ${lines.toString().padStart(3, '0')}", style = PixTypography.footerText, color = PixColors.Gray400)

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .background(PixColors.Dark)
                    .pixelBorder(PixColors.Blue)
                    .padding(8.dp),
            ) {
                Canvas(modifier = Modifier.size(width = (COLUMNS * 25 - 3).dp, height = (ROWS * 25 - 3).dp)) {
                    val gap = 3.dp.toPx()
                    val cell = (size.width - gap * (COLUMNS - 1)) / COLUMNS

                    fun topLeft(column: Int, row: Float) = Offset(column * (cell + gap), row * (cell + gap))

                    for (row in 0 until ROWS) {
                        for (column in 0 until COLUMNS) {
                            drawRect(emptyCell, topLeft(column, row.toFloat()), Size(cell, cell))
                        }
                    }

                    clipRect {
                        for (row in 0 until ROWS) {
                            val isClearing = row in clearing
                            val shift = clearing.count { it > row } * collapse.value

                            for (column in 0 until COLUMNS) {
                                val color = board[row * COLUMNS + column]
                                if (color == EMPTY) continue

                                if (isClearing) {
                                    // Acende de branco e apaga; quando as de cima começam a descer já sumiu.
                                    if (collapse.value == 0f) {
                                        drawBlock(lerpToWhite(palette[color], minOf(1f, flash.value * 2.5f)), topLeft(column, row.toFloat()), cell, 1f - flash.value * 0.7f)
                                    }
                                } else {
                                    drawBlock(palette[color], topLeft(column, row + shift), cell, 1f)
                                }
                            }
                        }

                        falling?.let { drop ->
                            drop.cells.forEach { (x, y) ->
                                drawBlock(palette[drop.color], topLeft(x + drop.column, y + fallRow.value), cell, 1f)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(text = "$title...", style = PixTypography.sectionTitle, color = PixColors.Cyan)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "> ${MESSAGES[messageIndex]}",
                style = PixTypography.bodySecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Isso leva alguns segundos. Não feche o app.",
                style = PixTypography.bodyMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun lerpToWhite(color: Color, amount: Float): Color = Color(
    red = color.red + (1f - color.red) * amount,
    green = color.green + (1f - color.green) * amount,
    blue = color.blue + (1f - color.blue) * amount,
)

/** Bloco com relevo de pixel: luz em cima e à esquerda, sombra embaixo e à direita. */
private fun DrawScope.drawBlock(color: Color, topLeft: Offset, cell: Float, alpha: Float) {
    val bevel = cell * 0.14f

    drawRect(color, topLeft, Size(cell, cell), alpha = alpha)
    drawRect(Color.White, topLeft, Size(cell, bevel), alpha = 0.28f * alpha)
    drawRect(Color.White, topLeft, Size(bevel, cell), alpha = 0.28f * alpha)
    drawRect(Color.Black, Offset(topLeft.x, topLeft.y + cell - bevel), Size(cell, bevel), alpha = 0.28f * alpha)
    drawRect(Color.Black, Offset(topLeft.x + cell - bevel, topLeft.y), Size(bevel, cell), alpha = 0.28f * alpha)
}
