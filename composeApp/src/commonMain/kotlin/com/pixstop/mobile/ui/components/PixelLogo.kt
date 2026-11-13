package com.pixstop.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.theme.AppBranding
import com.pixstop.mobile.ui.theme.PixColors

/**
 * A marca do Pixelstop: a bandeira quadriculada de pit stop e o nome escrito,
 * os dois no azul da logo com uma margem fina por fora.
 *
 * É o mesmo desenho do site (`public/images/brand/mark.svg` e
 * `wordmark.svg`), saído da mesma grade de pixels, e o mesmo do ícone do app.
 * Dois desenhos diferentes fariam o app e o site parecerem produtos distintos.
 *
 * **Só a margem muda com o tema**: branca no escuro, azul-marinho no claro. O
 * azul e o pano branco da bandeira são fixos — é a margem que faz o desenho
 * se destacar de qualquer fundo.
 */
@Composable
fun PixelstopLogo(
    modifier: Modifier = Modifier,
    height: Dp = 32.dp,
) {
    Row(
        modifier = modifier.semantics { contentDescription = AppBranding.APP_NAME },
        horizontalArrangement = Arrangement.spacedBy(height * 0.2f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelLogo(height = height)
        PixelWordmark(height = height * WORDMARK_TO_FLAG)
    }
}

/** O símbolo sozinho: a bandeira, para onde o nome não cabe. */
@Composable
fun PixelLogo(
    modifier: Modifier = Modifier,
    height: Dp = 96.dp,
) {
    PixelArt(FLAG, modifier, height)
}

/** O nome escrito, sem a bandeira. */
@Composable
fun PixelWordmark(
    modifier: Modifier = Modifier,
    height: Dp = 24.dp,
) {
    PixelArt(WORDMARK, modifier, height)
}

@Composable
private fun PixelArt(grid: List<String>, modifier: Modifier, height: Dp) {
    val margin = if (PixColors.isDark) Color.White else BRAND_NAVY
    // Meia casa de margem de cada lado: a caixa tem uma casa a mais.
    val columns = grid.first().length + 1
    val rows = grid.size + 1

    Canvas(modifier.height(height).width(height * columns / rows)) {
        val cell = size.height / rows
        val half = cell / 2f

        drawCells(grid, cell, 0f, 0f, cell * 2f) { margin }
        drawCells(grid, cell, half, half, cell) { char -> if (char == 'w') Color.White else BRAND_BLUE }
    }
}

/**
 * Cada casa pintada vira um quadrado. Na passada da margem o quadrado tem o
 * dobro do lado e começa meia casa antes, e a soma deles é o contorno.
 */
private inline fun DrawScope.drawCells(
    grid: List<String>,
    cell: Float,
    left: Float,
    top: Float,
    side: Float,
    color: (Char) -> Color,
) {
    grid.forEachIndexed { row, line ->
        line.forEachIndexed { column, char ->
            if (char != '.') {
                drawRect(color(char), Offset(left + column * cell, top + row * cell), Size(side, side))
            }
        }
    }
}

/** O azul da logo e o azul-marinho da margem no tema claro; fixos. */
private val BRAND_BLUE = Color(0xFF015DFA)
private val BRAND_NAVY = Color(0xFF0B1020)

/** Na assinatura o nome tem 0,77 da altura da bandeira, como no site. */
private const val WORDMARK_TO_FLAG = 0.77f

/** A bandeira: `b` é azul (mastro e casas escuras), `w` o pano branco. */
private val FLAG = listOf(
    ".bb...........",
    "bbbbbbb.......",
    "bbbwwbbbbbbbb.",
    "bbbwwbbwwbbwwb",
    "bbbbbwwwwbbwwb",
    "bbbbbwwbbwwbbb",
    "bbbwwbbbbwwbbb",
    "bbbwwbbwwbbwwb",
    "bbbbbwwwwbbwwb",
    "bbbbbwwbbwwbbb",
    "bbbbbbbbbwwbbb",
    "bbbb...bbbbbb.",
    "bbbb..........",
    "bbbb..........",
    "bbbb..........",
    ".bb...........",
)

/** PIXELSTOP, letra de pixel desenhada à mão na mesma grade da bandeira. */
private val WORDMARK = listOf(
    "bbbbbbb..bbbb.bbb..bbb.bbbbbbb.bbb......bbbbbbb.bbbbbbbb..bbbbbb..bbbbbbb.",
    "bbbbbbbb.bbbb.bbb..bbb.bbbbbbb.bbb.....bbbbbbbb.bbbbbbbb.bbbbbbbb.bbbbbbbb",
    "bbbbbbbb.bbbb.bbb..bbb.bbbbbbb.bbb.....bbbbbbbb.bbbbbbbb.bbbbbbbb.bbbbbbbb",
    "bbb..bbb.bbbb.bbbbbbbb.bbb.....bbb.....bbb........bbbb...bbb..bbb.bbb..bbb",
    "bbb..bbb.bbbb..bbbbbb..bbb.....bbb.....bbb........bbbb...bbb..bbb.bbb..bbb",
    "bbbbbbbb.bbbb...bbbb...bbbbbb..bbb.....bbbbbbb....bbbb...bbb..bbb.bbbbbbbb",
    "bbbbbbbb.bbbb...bbbb...bbbbbb..bbb......bbbbbbb...bbbb...bbb..bbb.bbbbbbbb",
    "bbbbbbb..bbbb..bbbbbb..bbb.....bbb..........bbb...bbbb...bbb..bbb.bbbbbbb.",
    "bbb......bbbb.bbbbbbbb.bbb.....bbb..........bbb...bbbb...bbb..bbb.bbb.....",
    "bbb......bbbb.bbb..bbb.bbbbbbb.bbbbbbb.bbbbbbbb...bbbb...bbbbbbbb.bbb.....",
    "bbb......bbbb.bbb..bbb.bbbbbbb.bbbbbbb.bbbbbbbb...bbbb...bbbbbbbb.bbb.....",
    "bbb......bbbb.bbb..bbb.bbbbbbb.bbbbbbb.bbbbbbb....bbbb....bbbbbb..bbb.....",
)
