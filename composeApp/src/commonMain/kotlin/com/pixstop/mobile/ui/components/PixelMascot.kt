package com.pixstop.mobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.FitaState
import com.pixstop.mobile.domain.model.MascotColors
import com.pixstop.mobile.domain.model.MascotEffect
import com.pixstop.mobile.domain.model.MascotFace
import com.pixstop.mobile.domain.model.MascotLook
import com.pixstop.mobile.domain.model.MascotState
import com.pixstop.mobile.ui.theme.LocalPixPalette
import kotlinx.coroutines.delay

/**
 * O mascote da Pixelstop: o LED RGB da fita, com cara.
 *
 * A cor do corpo é o estado, a mesma que a fita da geladeira mostra. O mapa de
 * estados está em `domain/model/Mascot.kt`; aqui só se desenha, da mesma grade
 * de 16 por 15 do `PixelMascot.vue` da web.
 *
 * @param state o vocabulário da interface (carregando, deu certo, deu errado).
 * @param fita um dos dez estados da geladeira, para a tela que a espelha; vence `state`.
 * @param face troca só o rosto, mantendo cor e efeito.
 * @param color a cor que a fita está mostrando de verdade, quando o app sabe: o
 *   LED na tela acende igual à porta.
 * @param burst força os pixels amarelos; por padrão, só a compra liberada solta.
 * @param decorative ao lado de um texto que já diz o estado: o leitor de tela pula.
 */
@Composable
fun PixelMascot(
    modifier: Modifier = Modifier,
    state: MascotState = MascotState.Idle,
    fita: FitaState? = null,
    size: Dp = 64.dp,
    face: MascotFace? = null,
    color: Color? = null,
    burst: Boolean? = null,
    animated: Boolean = true,
    decorative: Boolean = false,
) {
    val look = remember(state, fita) { fita?.let { MascotLook.of(it) } ?: MascotLook.of(state) }
    val moving = animated && !rememberReducedMotion()
    val shownFace = face ?: look.face
    val lightTheme = !LocalPixPalette.current.isDark

    // O ciclo RGB é só da espera: troca a cor a cada passo, pela ordem da fita.
    var cycleColor by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(moving, look.effect) {
        cycleColor = null
        if (moving && look.effect == MascotEffect.Cycle) {
            var step = 0
            while (true) {
                cycleColor = MascotColors.Cycle[step % MascotColors.Cycle.size]
                step++
                delay(CYCLE_STEP_MS)
            }
        }
    }
    val target = color ?: Color(cycleColor ?: look.color)

    // A cor desce pelo corpo, linha por linha, como a fita trocando.
    val rowColors = BODY_ROWS.indices.map { row -> animatedRowColor(target, row, moving) }

    // Três piscadas para o branco a cada troca de estado; depois a cor fica.
    var flashing by remember { mutableStateOf(false) }
    LaunchedEffect(look, moving) {
        flashing = false
        if (moving && look.effect == MascotEffect.Blink3) {
            repeat(3) {
                flashing = true
                delay(FLASH_MS)
                flashing = false
                delay(FLASH_MS)
            }
        }
    }

    // Um tique só move o balanço, as pernas e as faíscas: tudo no mesmo compasso de pixel.
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(moving) {
        tick = 0
        while (moving) {
            delay(TICK_MS)
            tick++
        }
    }

    var eyelid by remember { mutableStateOf(false) }
    LaunchedEffect(moving, shownFace) {
        eyelid = false
        while (moving && shownFace in BLINKING_FACES) {
            delay(BLINK_EVERY_MS)
            eyelid = true
            delay(BLINK_MS)
            eyelid = false
        }
    }

    val pulse = if (moving && look.effect == MascotEffect.Pulse) {
        val transition = rememberInfiniteTransition(label = "mascot-pulse")
        transition.animateFloat(0.35f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha").value
    } else {
        1f
    }

    // Só o branco some no fundo claro; quando a cor vem da fita de verdade, quem manda é ela.
    val pale = color == null && cycleColor == null && look.isPale
    val showBurst = burst ?: look.pixels
    val legsFrame = if (moving) (tick / 2) % 2 else 0
    val bob = if (moving && (tick / 2) % 2 == 1) 1f else 0f
    val sparkOn = !moving || tick % 2 == 0

    val semanticsModifier = if (decorative) {
        Modifier
    } else {
        Modifier.semantics {
            contentDescription = look.label
            role = Role.Image
        }
    }

    Canvas(modifier = modifier.then(semanticsModifier).size(size)) {
        val unit = this.size.minDimension / VIEW_SIZE

        fun cell(x: Float, y: Float, width: Float, height: Float, fill: Color, alpha: Float = 1f) {
            drawRect(
                color = fill,
                topLeft = Offset((x + VIEW_LEFT) * unit, (y + VIEW_TOP) * unit),
                size = Size(width * unit, height * unit),
                alpha = alpha,
            )
        }

        PINS[legsFrame].forEach { (x, last) -> cell(x.toFloat(), PINS_TOP.toFloat(), 1f, (last - PINS_TOP + 1).toFloat(), PIN_COLOR) }

        if (pale && lightTheme) {
            outline().forEach { (x, y, w, h) -> cell(x.toFloat(), y + bob, w.toFloat(), h.toFloat(), OUTLINE_COLOR) }
        }

        if (look.lit) {
            HALO.forEach { (x, y, w, h) -> cell(x.toFloat(), y + bob, w.toFloat(), h.toFloat(), rowColors[0].value, HALO_ALPHA) }
        }

        BODY_ROWS.forEachIndexed { row, (from, to) ->
            val base = rowColors[row].value
            val fill = if (flashing) lerp(base, FLASH_COLOR, 0.8f) else base
            cell(from.toFloat(), row + bob, (to - from + 1).toFloat(), 1f, fill, pulse)
        }

        FACES.getValue(shownFace).forEach { (x, y, w, h) -> cell(x.toFloat(), y + bob, w.toFloat(), h.toFloat(), FACE_COLOR) }

        if (eyelid) {
            val (x, y, w, h) = EYELID
            cell(x.toFloat(), y + bob, w.toFloat(), h.toFloat(), rowColors[y].value)
        }

        if (showBurst && sparkOn) {
            BURST_PIXELS.forEach { (x, y, w, h) -> cell(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), SPARK_COLOR) }
        }
    }
}

/** Troca de cor em três degraus, com cada linha esperando a de cima: a cascata da fita. */
@Composable
private fun animatedRowColor(target: Color, row: Int, moving: Boolean): State<Color> = animateColorAsState(
    targetValue = target,
    animationSpec = if (moving) tween(durationMillis = 250, delayMillis = row * 45, easing = ThreeSteps) else tween(0),
    label = "mascot-row-$row",
)

private val ThreeSteps = Easing { fraction -> (fraction * 3).toInt().coerceAtMost(3) / 3f }

private const val CYCLE_STEP_MS = 900L
private const val FLASH_MS = 300L
private const val TICK_MS = 300L
private const val BLINK_EVERY_MS = 4000L
private const val BLINK_MS = 160L
private const val HALO_ALPHA = 0.22f

/** A grade vai de -1 a 17 na horizontal e de -2 a 16 na vertical, como o viewBox da web. */
private const val VIEW_SIZE = 18f
private const val VIEW_LEFT = 1f
private const val VIEW_TOP = 2f

private val FACE_COLOR = Color(0xFF0B1020)
private val PIN_COLOR = Color(0xFF9AA7BC)
private val OUTLINE_COLOR = Color(0xFF9AA7BC)
private val SPARK_COLOR = Color(0xFFFFC93C)
private val FLASH_COLOR = Color(0xFFEEF2F7)

/** Cúpula, corpo e aba: uma corrida horizontal por linha, de `first` até `second` inclusive. */
private val BODY_ROWS = listOf(
    5 to 10, 4 to 11, 3 to 12, 3 to 12, 3 to 12, 3 to 12, 3 to 12, 3 to 12, 3 to 12, 2 to 13,
)

/** [x, y, largura, altura]. */
private data class Block(val x: Int, val y: Int, val w: Int, val h: Int)

private val HALO = listOf(
    Block(5, -1, 6, 1), Block(4, 0, 1, 1), Block(11, 0, 1, 1), Block(3, 1, 1, 1),
    Block(12, 1, 1, 1), Block(2, 2, 1, 7), Block(13, 2, 1, 7),
)

/** O contorno do fundo claro: cada linha do corpo alargada em um pixel, mais uma em cima e uma embaixo. */
private fun outline(): List<Block> = buildList {
    val (firstFrom, firstTo) = BODY_ROWS.first()
    add(Block(firstFrom, -1, firstTo - firstFrom + 1, 1))
    BODY_ROWS.forEachIndexed { row, (from, to) -> add(Block(from - 1, row, to - from + 3, 1)) }
    val (lastFrom, lastTo) = BODY_ROWS.last()
    add(Block(lastFrom, BODY_ROWS.size, lastTo - lastFrom + 1, 1))
}

private const val PINS_TOP = 10

/** As quatro pernas, [x, última linha], em dois quadros. A de x = 6 é o cátodo comum: a mais comprida, e não mexe. */
private val PINS = listOf(
    listOf(4 to 12, 6 to 14, 9 to 12, 11 to 11),
    listOf(4 to 11, 6 to 14, 9 to 11, 11 to 12),
)

private val BLINKING_FACES = setOf(MascotFace.Normal, MascotFace.Scared, MascotFace.Up, MascotFace.Smile, MascotFace.Worried)

private val EYELID = Block(5, 3, 6, 2)

private val FACES: Map<MascotFace, List<Block>> = mapOf(
    MascotFace.Normal to listOf(Block(5, 4, 2, 2), Block(9, 4, 2, 2)),
    MascotFace.Closed to listOf(Block(5, 5, 2, 1), Block(9, 5, 2, 1)),
    MascotFace.Scared to listOf(Block(5, 3, 2, 3), Block(9, 3, 2, 3), Block(7, 7, 2, 1)),
    MascotFace.Up to listOf(Block(5, 3, 2, 2), Block(9, 3, 2, 2)),
    MascotFace.Angry to listOf(Block(5, 4, 2, 2), Block(9, 4, 2, 2), Block(6, 3, 1, 1), Block(9, 3, 1, 1)),
    MascotFace.Happy to listOf(Block(5, 4, 2, 1), Block(9, 4, 2, 1), Block(5, 6, 1, 1), Block(10, 6, 1, 1), Block(6, 7, 4, 1)),
    MascotFace.Smile to listOf(Block(5, 4, 2, 2), Block(9, 4, 2, 2), Block(7, 7, 2, 1)),
    MascotFace.Worried to listOf(Block(5, 4, 2, 2), Block(9, 4, 2, 2), Block(4, 3, 2, 1), Block(10, 3, 2, 1)),
    MascotFace.Dizzy to listOf(Block(5, 4, 1, 1), Block(6, 5, 1, 1), Block(10, 4, 1, 1), Block(9, 5, 1, 1)),
)

private val BURST_PIXELS = listOf(
    Block(0, 1, 1, 1), Block(15, 0, 1, 1), Block(14, -2, 1, 1),
    Block(1, -2, 1, 1), Block(0, 6, 1, 1), Block(15, 5, 1, 1),
)
