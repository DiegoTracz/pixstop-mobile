package com.pixstop.mobile.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.MascotState
import com.pixstop.mobile.ui.theme.PixColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Onde o botão para depois de solto: encostado na borda mais perto, dentro da tela. */
internal fun snapBubble(x: Float, y: Float, maxX: Float, maxY: Float): Pair<Float, Float> =
    (if (x < maxX / 2f) 0f else maxX) to y.coerceIn(0f, maxY.coerceAtLeast(0f))

/**
 * O mascote flutuando sobre a Home: um toque abre o assistente, e a pessoa
 * arrasta para onde ele não atrapalha. Ao soltar ele encosta na borda mais
 * perto, como as bolhas de conversa, e lembra o lado e a altura.
 */
@Composable
fun AssistantBubble(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
) {
    // Lado e altura relativa: sobrevivem à rotação, que muda o tamanho da tela.
    var onRight by rememberSaveable { mutableStateOf(true) }
    var heightFraction by rememberSaveable { mutableStateOf(0.82f) }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val size = 56.dp
        val margin = 12.dp
        val sizePx = with(density) { size.toPx() }
        val marginPx = with(density) { margin.toPx() }
        val maxX = (with(density) { maxWidth.toPx() } - sizePx - marginPx * 2).coerceAtLeast(0f)
        val maxY = (with(density) { maxHeight.toPx() } - sizePx - marginPx * 2).coerceAtLeast(0f)

        // A posição muda a cada movimento do dedo, sem corrotina no meio:
        // só o encaixe na borda, ao soltar, é animado.
        var x by remember { mutableFloatStateOf(if (onRight) maxX else 0f) }
        var y by remember { mutableFloatStateOf(heightFraction * maxY) }
        val scope = rememberCoroutineScope()
        var dragging by remember { mutableStateOf(false) }
        var settle by remember { mutableStateOf<Job?>(null) }

        LaunchedEffect(maxX, maxY) {
            if (!dragging) {
                x = if (onRight) maxX else 0f
                y = heightFraction * maxY
            }
        }

        val shadowColor = if (dragging) PixColors.Cyan else PixColors.Purple

        Box(
            Modifier
                .offset { IntOffset((x + marginPx).roundToInt(), (y + marginPx).roundToInt()) }
                .size(size)
                // A sombra dura do PixelButton, à direita e embaixo; cresce
                // enquanto a pessoa arrasta, como se o botão subisse.
                .drawBehind {
                    val shadow = (if (dragging) 7.dp else 4.dp).toPx()
                    drawRect(shadowColor, topLeft = Offset(this.size.width, shadow), size = Size(shadow, this.size.height))
                    drawRect(shadowColor, topLeft = Offset(shadow, this.size.height), size = Size(this.size.width, shadow))
                }
                .background(PixColors.Darker)
                .border(2.dp, if (dragging) PixColors.Purple else PixColors.Blue)
                .pointerInput(maxX, maxY) {
                    detectDragGestures(
                        onDragStart = {
                            settle?.cancel()
                            dragging = true
                        },
                        onDragEnd = {
                            dragging = false
                            val (endX, endY) = snapBubble(x, y, maxX, maxY)
                            onRight = endX > 0f
                            heightFraction = if (maxY > 0f) endY / maxY else 0f
                            val startX = x
                            val startY = y
                            settle = scope.launch {
                                animate(0f, 1f, animationSpec = spring(dampingRatio = 0.7f)) { progress, _ ->
                                    x = startX + (endX - startX) * progress
                                    y = startY + (endY - startY) * progress
                                }
                            }
                        },
                        onDragCancel = { dragging = false },
                    ) { change, drag ->
                        change.consume()
                        x = (x + drag.x).coerceIn(0f, maxX)
                        y = (y + drag.y).coerceIn(0f, maxY)
                    }
                }
                .clickable(onClick = onClick)
                .semantics { contentDescription = "Abrir o assistente" },
            contentAlignment = Alignment.Center,
        ) {
            // Arrastando, o mascote e a borda mudam de cor: a pessoa vê que pegou o botão.
            PixelMascot(
                state = if (busy) MascotState.Loading else MascotState.Idle,
                size = 40.dp,
                color = if (dragging) PixColors.Purple else null,
                decorative = true,
            )
        }
    }
}
