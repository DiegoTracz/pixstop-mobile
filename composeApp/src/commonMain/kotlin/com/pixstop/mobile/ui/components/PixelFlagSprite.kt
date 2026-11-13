package com.pixstop.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.theme.PixColors
import kotlinx.coroutines.delay

/**
 * A bandeira da marca tremulando, como sprite de jogo.
 *
 * São 8 quadros desenhados à parte e trocados a 10 por segundo: o pano
 * inteiro ondula, as bordas curvam junto e as dobras ganham luz na subida e
 * sombra na descida; o mastro fica parado. São os mesmos quadros da logo do
 * site (`AppLogoIcon.vue`), saídos da mesma grade, na resolução dobrada da
 * bandeira parada para o pano andar de pixel em pixel.
 *
 * A margem segue o tema, como no resto da marca: branca no escuro,
 * azul-marinho no claro. Com `animated` falso fica o desenho parado — o mesmo
 * do ícone do app.
 */
@Composable
fun PixelFlagSprite(
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    animated: Boolean = true,
) {
    var frame by remember { mutableIntStateOf(0) }

    if (animated) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(FRAME_MILLIS)
                frame = (frame + 1) % FRAMES.size
            }
        }
    }

    val grid = if (animated) FRAMES[frame] else STILL
    val margin = if (PixColors.isDark) Color.White else NAVY
    // Uma casa de folga em cada lado para a margem.
    val columns = grid.first().length + 2
    val rows = grid.size + 2

    Canvas(modifier.height(height).width(height * columns / rows)) {
        val cell = size.height / rows

        grid.forEachIndexed { row, line ->
            line.forEachIndexed { column, char ->
                if (char != '.') {
                    drawRect(margin, Offset(column * cell, row * cell), Size(cell * 3, cell * 3))
                }
            }
        }
        grid.forEachIndexed { row, line ->
            line.forEachIndexed { column, char ->
                val color = COLORS[char]
                if (color != null) {
                    drawRect(color, Offset((column + 1) * cell, (row + 1) * cell), Size(cell, cell))
                }
            }
        }
    }
}

private const val FRAME_MILLIS = 100L

private val NAVY = Color(0xFF0B1020)

/** `b` azul da logo, `l` a luz da dobra, `d` a sombra; `w` o pano branco e `s` ele na sombra. */
private val COLORS = mapOf(
    'b' to Color(0xFF015DFA),
    'l' to Color(0xFF3378FF),
    'd' to Color(0xFF0149C8),
    'w' to Color(0xFFFFFFFF),
    's' to Color(0xFFD6E4FF),
)

/** A bandeira parada, na mesma caixa dos quadros. */
private val STILL = listOf(
        "..bbbb......................",
        "..bbbb......................",
        "bbbbbbbbbbbbbb..............",
        "bbbbbbbbbbbbbb..............",
        "bbbbbbwwwwbbbbbbbbbbbbbbbb..",
        "bbbbbbwwwwbbbbbbbbbbbbbbbb..",
        "bbbbbbwwwwbbbbwwwwbbbbwwwwbb",
        "bbbbbbwwwwbbbbwwwwbbbbwwwwbb",
        "bbbbbbbbbbwwwwwwwwbbbbwwwwbb",
        "bbbbbbbbbbwwwwwwwwbbbbwwwwbb",
        "bbbbbbbbbbwwwwbbbbwwwwbbbbbb",
        "bbbbbbbbbbwwwwbbbbwwwwbbbbbb",
        "bbbbbbwwwwbbbbbbbbwwwwbbbbbb",
        "bbbbbbwwwwbbbbbbbbwwwwbbbbbb",
        "bbbbbbwwwwbbbbwwwwbbbbwwwwbb",
        "bbbbbbwwwwbbbbwwwwbbbbwwwwbb",
        "bbbbbbbbbbwwwwwwwwbbbbwwwwbb",
        "bbbbbbbbbbwwwwwwwwbbbbwwwwbb",
        "bbbbbbbbbbwwwwbbbbwwwwbbbbbb",
        "bbbbbbbbbbwwwwbbbbwwwwbbbbbb",
        "bbbbbbbbbbbbbbbbbbwwwwbbbbbb",
        "bbbbbbbbbbbbbbbbbbwwwwbbbbbb",
        "bbbbbbbb......bbbbbbbbbbbb..",
        "bbbbbbbb......bbbbbbbbbbbb..",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "..bbbb......................",
        "..bbbb......................",
)

private val FRAMES = listOf(
    listOf(
        "..bbbb......................",
        "..bbbb.....bbb..............",
        "bbbbbbbbbbbbbb..............",
        "bbbbbbbbbbbbbb..............",
        "bbbbbbwwwwbbbbbdd........l..",
        "bbbbbbwwwwbbbbbdddbbbbbbllll",
        "bbbbbbwwwwbbbbwssdbbbbbblwll",
        "bbbbbbwwwwbwwwwsssbbbbwwwwll",
        "bbbbbbbbbbwwwwwsssbbbbwwwwll",
        "bbbbbbbbbbwwwwwsssbbbbwwwwll",
        "bbbbbbbbbbwwwwbddsbbbbwwwlll",
        "bbbbbbbbbbwbbbbdddwwwwbbllll",
        "bbbbbbwwwwbbbbbdddwwwwbbllll",
        "bbbbbbwwwwbbbbbdddwwwwbbllll",
        "bbbbbbwwwwbbbbwssdwwwwbblwll",
        "bbbbbbwwwwbwwwwsssbbbbwwwwll",
        "bbbbbbbbbbwwwwwsssbbbbwwwwll",
        "bbbbbbbbbbwwwwwsssbbbbwwwwll",
        "bbbbbbbbbbwwwwbddsbbbbwwwlll",
        "bbbbbbbbbbwbbbbdddwwwwbbllll",
        "bbbbbbbbbbbbbbbdddwwwwbbllll",
        "bbbbbbbbbbb...bdddwwwwbbll..",
        "bbbbbbbb......bdddwwwwbbll..",
        "bbbbbbbb......bdddbbbbbbll..",
        "bbbbbbbb.........dbbbbbbl...",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "..bbbb......................",
        "..bbbb......................",
    ),
    listOf(
        "..bbbb......................",
        "..bbbb......bb..............",
        "bbbbbbbbbbbbbb..............",
        "bbbbbbbbbbbbbbbbb...........",
        "bbbbbbwwwwbbbbbbbddd........",
        "bbbbbbwwwwbbbbwwwddddb...b..",
        "bbbbbbwwwwbbbbwwwsdddbbbbb.l",
        "bbbbbbwwwwbbwwwwwsdddbbbbwll",
        "bbbbbbbbbbwwwwwwwsdddbwwwwll",
        "bbbbbbbbbbwwwwbbbsdddbwwwwll",
        "bbbbbbbbbbwwwwbbbdssdbwwwwll",
        "bbbbbbbbbbwwbbbbbdssswwwwbll",
        "bbbbbbwwwwbbbbbbbdssswbbbbll",
        "bbbbbbwwwwbbbbwwwdssswbbbbll",
        "bbbbbbwwwwbbbbwwwsddswbbbbll",
        "bbbbbbwwwwbbwwwwwsdddbbbbwll",
        "bbbbbbbbbbwwwwwwwsdddbwwwwll",
        "bbbbbbbbbbwwwwbbbsdddbwwwwll",
        "bbbbbbbbbbwwwwbbbdssdbwwwwll",
        "bbbbbbbbbbwwbbbbbdssswwwwbll",
        "bbbbbbbbbbbbbbbbbdssswbbbbll",
        "bbbbbbbbbbbb..bbbdssswbbbbll",
        "bbbbbbbb......bbbdddswbbbbl.",
        "bbbbbbbb.........ddddbbbbb..",
        "bbbbbbbb............dbbbbb..",
        "bbbbbbbb..............bbb...",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "..bbbb......................",
        "..bbbb......................",
    ),
    listOf(
        "..bbbb......................",
        "..bbbb.......b..............",
        "bbbbbbbbbbbbbb..............",
        "bbbbbbbbbbbbbbbbbbbd........",
        "bbbbbbwwwwbbbbbbbbbddd......",
        "bbbbbbwwwwbbbbwwwwbddddd....",
        "bbbbbbwwwwbbbbwwwwbdddddbb..",
        "bbbbbbwwwwbbbwwwwwbdddssbb..",
        "bbbbbbbbbbwwwwwwwwbdddsswwbb",
        "bbbbbbbbbbwwwwbbbbwsddsswwbb",
        "bbbbbbbbbbwwwwbbbbwssssswwbb",
        "bbbbbbbbbbwwwbbbbbwsssddwwbb",
        "bbbbbbwwwwbbbbbbbbwsssddbbbb",
        "bbbbbbwwwwbbbbwwwwbdssddbbbb",
        "bbbbbbwwwwbbbbwwwwbdddddbbbb",
        "bbbbbbwwwwbbbwwwwwbdddssbbbb",
        "bbbbbbbbbbwwwwwwwwbdddsswwbb",
        "bbbbbbbbbbwwwwbbbbwsddsswwbb",
        "bbbbbbbbbbwwwwbbbbwssssswwbb",
        "bbbbbbbbbbwwwbbbbbwsssddwwbb",
        "bbbbbbbbbbbbbbbbbbwsssddbbbb",
        "bbbbbbbbbbbbb.bbbbbdssddbbbb",
        "bbbbbbbb......bbbbbdddddbbbb",
        "bbbbbbbb............ddddbbbb",
        "bbbbbbbb..............ddbb..",
        "bbbbbbbb................bb..",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "..bbbb......................",
        "..bbbb......................",
    ),
    listOf(
        "..bbbb......................",
        "..bbbb......................",
        "bbbbbbbbbbbbbb..............",
        "bbbbbbbbbbbbbb.bbbbbbd......",
        "bbbbbbwwwwbbbbbbbbbbbddd....",
        "bbbbbbwwwwbbbbbwwwbbbddddd..",
        "bbbbbbwwwwbbbbwwwwbbbdssdd..",
        "bbbbbbwwwwbbbbwwwwbbbdssss..",
        "bbbbbbbbbbwwwwwwwwbbbdssssbb",
        "bbbbbbbbbbwwwwwbbbwwwsssssbb",
        "bbbbbbbbbbwwwwbbbbwwwsddssbb",
        "bbbbbbbbbbwwwwbbbbwwwsddddbb",
        "bbbbbbwwwwbbbbbbbbwwwsddddbb",
        "bbbbbbwwwwbbbbbwwwbbbdddddbb",
        "bbbbbbwwwwbbbbwwwwbbbdssddbb",
        "bbbbbbwwwwbbbbwwwwbbbdssssbb",
        "bbbbbbbbbbwwwwwwwwbbbdssssbb",
        "bbbbbbbbbbwwwwwbbbwwwsssssbb",
        "bbbbbbbbbbwwwwbbbbwwwsddssbb",
        "bbbbbbbbbbwwwwbbbbwwwsddddbb",
        "bbbbbbbbbbbbbbbbbbwwwsddddbb",
        "bbbbbbbbbbbbbbbbbbbbbdddddbb",
        "bbbbbbbb......bbbbbbbdddddbb",
        "bbbbbbbb......b.......ddddbb",
        "bbbbbbbb................dd..",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "..bbbb......................",
        "..bbbb......................",
    ),
    listOf(
        "..bbbb......................",
        "..bbbb......................",
        "bbbbbbbbbbb.................",
        "bbbbbbbbbbbbbb...bbbbbbdd...",
        "bbbbbbwwwwbbbbbbbbbbbbbddd..",
        "bbbbbbwwwwbbbbbbbwbbbbwssd..",
        "bbbbbbwwwwbbbbwwwwbbbbwsss..",
        "bbbbbbwwwwbbbbwwwwbbbbwsssdd",
        "bbbbbbbbbbwbbbwwwwbbbbwsssdd",
        "bbbbbbbbbbwwwwwwwbwwwwbddsdd",
        "bbbbbbbbbbwwwwbbbbwwwwbddddd",
        "bbbbbbbbbbwwwwbbbbwwwwbddddd",
        "bbbbbbwwwwbwwwbbbbwwwwbddddd",
        "bbbbbbwwwwbbbbbbbwbbbbwssddd",
        "bbbbbbwwwwbbbbwwwwbbbbwsssdd",
        "bbbbbbwwwwbbbbwwwwbbbbwsssdd",
        "bbbbbbbbbbwbbbwwwwbbbbwsssdd",
        "bbbbbbbbbbwwwwwwwbwwwwbddsdd",
        "bbbbbbbbbbwwwwbbbbwwwwbddddd",
        "bbbbbbbbbbwwwwbbbbwwwwbddddd",
        "bbbbbbbbbbbwwwbbbbwwwwbddddd",
        "bbbbbbbbbbbbbbbbbbbbbbbddddd",
        "bbbbbbbb...bbbbbbbbbbbbddddd",
        "bbbbbbbb......bbb........d..",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "..bbbb......................",
        "..bbbb......................",
    ),
    listOf(
        "..bbbb......................",
        "..bbbb......................",
        "bbbbbbbbbbbb..........bbb...",
        "bbbbbbbbbbbbbb......bbbbbd..",
        "bbbbbbwwwwbbbb...bbbbbwwwd..",
        "bbbbbbwwwwbbbbbbbbbbbbwwwsd.",
        "bbbbbbwwwwbbbbbbbwbbbbwwwsdd",
        "bbbbbbwwwwbbbbwwwwbbbbwwwsdd",
        "bbbbbbbbbbwwbbwwwwbbbbbbbsdd",
        "bbbbbbbbbbwwwwwwwwbbwwbbbddd",
        "bbbbbbbbbbwwwwwwwbwwwwbbbddd",
        "bbbbbbbbbbwwwwbbbbwwwwbbbddd",
        "bbbbbbwwwwbbwwbbbbwwwwwwwddd",
        "bbbbbbwwwwbbbbbbbbwwbbwwwsdd",
        "bbbbbbwwwwbbbbbbbwbbbbwwwsdd",
        "bbbbbbwwwwbbbbwwwwbbbbwwwsdd",
        "bbbbbbbbbbwwbbwwwwbbbbbbbsdd",
        "bbbbbbbbbbwwwwwwwwbbwwbbbddd",
        "bbbbbbbbbbwwwwwwwbwwwwbbbddd",
        "bbbbbbbbbbwwwwbbbbwwwwbbbddd",
        "bbbbbbbbbbbbwwbbbbwwwwbbbddd",
        "bbbbbbbbbbbbbbbbbbwwbbbbbd.d",
        "bbbbbbbb....bbbbbbbbbb...d..",
        "bbbbbbbb......bbbbbb........",
        "bbbbbbbb......bbb...........",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "..bbbb......................",
        "..bbbb......................",
    ),
    listOf(
        "..bbbb......................",
        "..bbbb......................",
        "bbbbbbbbbbbbb...........bb..",
        "bbbbbbbbbbbbbb........lbbb..",
        "bbbbbbwwwwbbbb......lllbwwbd",
        "bbbbbbwwwwbbbbbbbbbbllwwwwbd",
        "bbbbbbwwwwbbbbbbbbbbllwwwwbd",
        "bbbbbbwwwwbbbbwwwwbbllwwwwbd",
        "bbbbbbbbbbwwwbwwwwbbllwwbbbd",
        "bbbbbbbbbbwwwwwwwwbblllbbbbd",
        "bbbbbbbbbbwwwwwwwwbbwwlbbbbd",
        "bbbbbbbbbbwwwwbbbbwwwwlbbbbd",
        "bbbbbbwwwwbbbwbbbbwwwwlbwwbd",
        "bbbbbbwwwwbbbbbbbbwwwwwwwwbd",
        "bbbbbbwwwwbbbbbbbbwwllwwwwbd",
        "bbbbbbwwwwbbbbwwwwbbllwwwwbd",
        "bbbbbbbbbbwwwbwwwwbbllwwbbbd",
        "bbbbbbbbbbwwwwwwwwbblllbbbbd",
        "bbbbbbbbbbwwwwwwwwbbwwlbbbbd",
        "bbbbbbbbbbwwwwbbbbwwwwlbbbbd",
        "bbbbbbbbbbbbbwbbbbwwwwlbbb..",
        "bbbbbbbbbbbbbbbbbbwwwwlbbb..",
        "bbbbbbbb.....bbbbbwwlllb....",
        "bbbbbbbb......bbbbbbll......",
        "bbbbbbbb......bbbbbb........",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "..bbbb......................",
        "..bbbb......................",
    ),
    listOf(
        "..bbbb......................",
        "..bbbb......................",
        "bbbbbbbbbbbbbb..............",
        "bbbbbbbbbbbbbb..........ll..",
        "bbbbbbwwwwbbbbb.......llllbb",
        "bbbbbbwwwwbbbbbbbbbbbbllwwbb",
        "bbbbbbwwwwbbbbwbbbbbbbwwwwbb",
        "bbbbbbwwwwbbbbwwwwbbbbwwwwbb",
        "bbbbbbbbbbwwwwwwwwbbbbwwwwbb",
        "bbbbbbbbbbwwwwwwwwbbbbwwllbb",
        "bbbbbbbbbbwwwwbwwwbbbbllllbb",
        "bbbbbbbbbbwwwwbbbbwwwwllllbb",
        "bbbbbbwwwwbbbbbbbbwwwwllllbb",
        "bbbbbbwwwwbbbbbbbbwwwwllwwbb",
        "bbbbbbwwwwbbbbwbbbwwwwwwwwbb",
        "bbbbbbwwwwbbbbwwwwbbbbwwwwbb",
        "bbbbbbbbbbwwwwwwwwbbbbwwwwbb",
        "bbbbbbbbbbwwwwwwwwbbbbwwllbb",
        "bbbbbbbbbbwwwwbwwwbbbbllllbb",
        "bbbbbbbbbbwwwwbbbbwwwwllllbb",
        "bbbbbbbbbbbbbbbbbbwwwwllll..",
        "bbbbbbbbbbbbbbbbbbwwwwllll..",
        "bbbbbbbb......bbbbwwwwllll..",
        "bbbbbbbb......bbbbbbbbll....",
        "bbbbbbbb.......bbbbbbb......",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "bbbbbbbb....................",
        "..bbbb......................",
        "..bbbb......................",
    ),
)
