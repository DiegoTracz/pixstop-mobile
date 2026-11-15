package com.pixstop.mobile.ui.components.reward

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixstop.mobile.domain.model.RewardChest
import com.pixstop.mobile.domain.model.RewardCurrency
import com.pixstop.mobile.domain.model.RewardKind
import com.pixstop.mobile.domain.model.RewardScene
import com.pixstop.mobile.domain.reward.RewardCue
import com.pixstop.mobile.domain.reward.RewardEngine
import com.pixstop.mobile.domain.reward.RewardPhase
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.rememberReducedMotion
import com.pixstop.mobile.ui.theme.PixTypography
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * As recompensas de um pedido, uma cena por vez, em tela cheia.
 *
 * Cada cena tem os quatro tempos: a espera (o baú respira até a pessoa
 * tocar), a carga, o estouro e a contagem, com as moedas voando para o
 * contador. A cena sem baú — o cashback — é curta e começa sozinha, porque
 * toca em toda compra.
 */
@Composable
fun RewardCelebration(
    scenes: List<RewardScene>,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var index by remember(scenes) { mutableIntStateOf(0) }
    val scene = scenes.getOrNull(index)

    if (scene == null) {
        LaunchedEffect(scenes) { onFinished() }
        return
    }

    key(index) {
        RewardStage(
            scene = scene,
            onContinue = { if (index < scenes.lastIndex) index++ else onFinished() },
            modifier = modifier,
        )
    }
}

@Composable
private fun RewardStage(
    scene: RewardScene,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val engine = remember { RewardEngine(scene) }
    val stage = remember { stageOf(scene) }
    val jitter = remember { Random(7) }
    val sound = rememberRewardSound()
    val haptics = LocalHapticFeedback.current
    val reduced = rememberReducedMotion()

    var frame by remember { mutableLongStateOf(0L) }
    var phase by remember { mutableStateOf(engine.phase) }
    var shown by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var landed by remember { mutableIntStateOf(0) }
    val lastCoinSound = remember { floatArrayOf(-1f) }

    val handle: (List<RewardCue>) -> Unit = { cues ->
        for (cue in cues) {
            when (cue) {
                is RewardCue.ChargeTick -> sound.play(RewardTunes.chargeTick(cue.step))
                RewardCue.Burst -> {
                    sound.play(RewardTunes.burst)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }

                is RewardCue.CoinLanded -> {
                    landed = cue.index

                    // Trinta moedas em um segundo virariam ruído: uma nota a cada 45 ms basta.
                    if (engine.clock - lastCoinSound[0] > 0.045f) {
                        lastCoinSound[0] = engine.clock
                        sound.play(RewardTunes.coin(cue.index))
                    }
                }

                RewardCue.Reveal -> sound.play(RewardTunes.reveal)
            }
        }
    }

    val open: () -> Unit = {
        if (engine.phase == RewardPhase.Idle) {
            if (reduced) engine.skip() else handle(engine.open())
            phase = engine.phase
            shown = engine.shown
            revealed = engine.revealed
        }
    }

    LaunchedEffect(engine) {
        var last = 0L
        var waited = 0f

        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                last = now

                if (scene.opensByItself && engine.phase == RewardPhase.Idle) {
                    waited += dt
                    if (waited > 0.35f) open()
                }

                handle(engine.step(dt))
                phase = engine.phase
                shown = engine.shown
                revealed = engine.revealed
                frame = now
            }
        }
    }

    val accent = if (scene.currency == RewardCurrency.Xp) Ink.Soft else Ink.Gold
    val pixel = PixTypography.pixelFontFamily
    val sans = PixTypography.sansFontFamily

    val revealScale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow),
    )
    val hudScale by animateFloatAsState(
        targetValue = if (landed % 2 == 1) 1.16f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
    )
    val blink by rememberInfiniteTransition().animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                1f at 0
                1f at 499
                0.25f at 500
                0.25f at 999
            },
            repeatMode = RepeatMode.Restart,
        ),
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(stage.background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = open),
    ) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val unit = min(widthPx / RewardEngine.WIDTH, heightPx / RewardEngine.HEIGHT)
        val originX = (widthPx - RewardEngine.WIDTH * unit) / 2f
        val originY = (heightPx - RewardEngine.HEIGHT * unit) / 2f
        val top: (Float) -> androidx.compose.ui.unit.Dp = { logicalY -> with(density) { (originY + logicalY * unit).toDp() } }
        val side = with(density) { (originX + 6f * unit).toDp() }

        Canvas(modifier = Modifier.fillMaxSize()) {
            // A leitura do quadro é o que faz o palco redesenhar a cada instante.
            if (frame >= 0L) drawReward(engine, scene, stage, jitter)
        }

        if (scene.gain > 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = top(6f), end = side)
                    .scale(hudScale)
                    .background(Ink.Night)
                    .border(2.dp, accent)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(10.dp).background(accent))
                Text(text = "+$shown", style = TextStyle(fontFamily = pixel, fontSize = 12.sp, color = accent))
            }
        }

        if (phase == RewardPhase.Idle || phase == RewardPhase.Charge) {
            Text(
                text = scene.tag,
                style = TextStyle(fontFamily = pixel, fontSize = 14.sp, color = Ink.White, textAlign = TextAlign.Center),
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(top = top(54f)),
            )
        }

        if (phase == RewardPhase.Idle && !scene.opensByItself) {
            Text(
                text = "TOQUE PARA ABRIR",
                style = TextStyle(fontFamily = pixel, fontSize = 12.sp, color = Ink.White, textAlign = TextAlign.Center),
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(top = top(246f)).alpha(blink),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = top(36f), start = 20.dp, end = 20.dp)
                .scale(revealScale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = scene.title,
                style = TextStyle(fontFamily = pixel, fontSize = 24.sp, lineHeight = 32.sp, color = Ink.Gold, textAlign = TextAlign.Center),
            )
            Text(
                text = scene.subtitle,
                style = TextStyle(fontFamily = sans, fontSize = 16.sp, color = Ink.White, textAlign = TextAlign.Center),
            )
            Text(
                text = scene.prize,
                style = TextStyle(fontFamily = pixel, fontSize = 16.sp, color = Ink.Night),
                modifier = Modifier.background(accent).padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        if (phase == RewardPhase.Done) {
            PixelButton(
                text = "CONTINUAR",
                onClick = onContinue,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .fillMaxWidth(),
            )
        }
    }
}

/* ── O palco, desenhado em blocos num espaço lógico de 180 × 320 ─────────── */

private object Ink {
    val Brand = Color(0xFF015DFA)
    val Soft = Color(0xFF4D8BFF)
    val Pale = Color(0xFF9CC0FF)
    val Night = Color(0xFF060A14)
    val Gold = Color(0xFFFFC93C)
    val GoldDark = Color(0xFFB37A00)
    val GoldLight = Color(0xFFFFE89A)
    val Green = Color(0xFF3DDC97)
    val GreenDark = Color(0xFF0E7C53)
    val GreenLight = Color(0xFFA6F2D2)
    val White = Color(0xFFFFFFFF)
    val Gray = Color(0xFF9AA7C4)
    val Shadow = Color(0x59000000)
}

private class StageLook(val background: Color, val floor: Color, val rays: Color?)

private fun stageOf(scene: RewardScene) = when (scene.kind) {
    RewardKind.Level -> StageLook(Color(0xFF0149C8), Color(0xFF013BA3), Ink.GoldLight)
    RewardKind.Cashback -> StageLook(Color(0xFF0B1020), Color(0xFF111A33), null)
    RewardKind.Quota -> StageLook(Color(0xFF06301F), Color(0xFF07402A), Ink.GreenLight)
    RewardKind.Medal -> StageLook(Color(0xFF1A1240), Color(0xFF241A57), Ink.GoldLight)
}

private const val CX = RewardEngine.CX
private const val FLOOR = RewardEngine.FLOOR

private fun DrawScope.px(x: Float, y: Float, w: Float, h: Float, color: Color, alpha: Float = 1f) {
    drawRect(
        color = color,
        topLeft = Offset(x.roundToInt().toFloat(), y.roundToInt().toFloat()),
        size = Size(w.roundToInt().toFloat(), h.roundToInt().toFloat()),
        alpha = alpha,
    )
}

private fun DrawScope.drawReward(engine: RewardEngine, scene: RewardScene, stage: StageLook, jitter: Random) {
    val unit = min(size.width / RewardEngine.WIDTH, size.height / RewardEngine.HEIGHT)
    val originX = (size.width - RewardEngine.WIDTH * unit) / 2f
    val originY = (size.height - RewardEngine.HEIGHT * unit) / 2f

    val t = engine.time
    val clock = engine.clock
    val phase = engine.phase

    var squash = 0f
    var shakeX = 0f
    var open = 0f
    var shake = 0f

    when (phase) {
        RewardPhase.Idle -> squash = sin(clock * 3f) * 0.12f
        RewardPhase.Charge -> {
            val k = (t / RewardEngine.CHARGE_SECONDS).coerceAtMost(1f)
            squash = 0.2f + k * 0.6f
            shakeX = sin(clock * 70f) * (1f + k * 4f)
            shake = k * 1.5f
        }

        RewardPhase.Burst, RewardPhase.Done -> {
            open = (t / 0.14f).coerceAtMost(1f)
            squash = if (phase == RewardPhase.Burst) -max(0f, 0.5f - t * 2.4f) else 0f
            shake = max(0f, 3f - t * 9f)
        }
    }

    val shiftX = if (shake > 0f) ((jitter.nextFloat() - 0.5f) * shake * 2f).roundToInt() else 0
    val shiftY = if (shake > 0f) ((jitter.nextFloat() - 0.5f) * shake * 2f).roundToInt() else 0
    val burning = phase == RewardPhase.Burst || phase == RewardPhase.Done
    val life = if (burning) max(0.35f, 1f - t * 0.18f) else 0f
    val mouth = engine.mouthY

    withTransform({
        translate(originX + shiftX * unit, originY + shiftY * unit)
        scale(unit, unit, pivot = Offset.Zero)
    }) {
        checkerFloor(stage, originX / unit, originY / unit)

        if (life > 0f && stage.rays != null) rays(mouth, life, clock, stage.rays)

        if (engine.hasChest) {
            // O baú é desenhado em dobro: é o protagonista da tela.
            withTransform({ scale(2f, 2f, pivot = Offset(CX, FLOOR)) }) {
                chest(scene.chest, open, squash, shakeX / 2f)
            }
        } else {
            receipt(if (phase == RewardPhase.Idle) (sin(clock * 3f) * 2f).roundToInt().toFloat() else 0f)
        }

        if (life > 0f && stage.rays != null) ribbons(mouth, (t / 0.7f).coerceAtMost(1f), max(0f, 1f - t * 0.55f), clock)

        if (scene.trophy && burning) {
            val rise = (t / 0.5f).coerceAtMost(1f)
            trophy(CX, mouth - 6f - rise * 44f + sin(clock * 3f) * 2f)
        }

        if (phase == RewardPhase.Idle || phase == RewardPhase.Charge) sparkles(clock)

        coins(engine, scene.currency)
    }

    if (phase == RewardPhase.Burst) {
        val flash = max(0f, 1f - t / 0.22f)
        if (flash > 0f) drawRect(color = Ink.White, alpha = flash)
    }
}

/** O chão quadriculado é a bandeira da marca, deitada; vai até as bordas da tela. */
private fun DrawScope.checkerFloor(stage: StageLook, marginX: Float, marginY: Float) {
    val firstCol = floor(-marginX / 18f).toInt()
    val lastCol = ceil((RewardEngine.WIDTH + marginX) / 18f).toInt()
    var row = 0

    while (FLOOR + 6f + row * 18f < RewardEngine.HEIGHT + marginY) {
        for (col in firstCol..lastCol) {
            if ((row + col).mod(2) == 0) px(col * 18f, FLOOR + 6f + row * 18f, 18f, 18f, stage.floor)
        }
        row++
    }
}

private fun DrawScope.chest(kind: RewardChest, open: Float, squash: Float, shakeX: Float) {
    val fill = if (kind == RewardChest.Green) Ink.GreenDark else Ink.Brand
    val light = if (kind == RewardChest.Green) Ink.Green else Ink.Soft
    val pale = if (kind == RewardChest.Green) Ink.GreenLight else Ink.Pale

    val w = (48f * (1f + squash * 0.14f)).roundToInt().toFloat()
    val bodyH = (20f * (1f - squash * 0.2f)).roundToInt().toFloat()
    val lidFull = 15f
    val x = (CX - w / 2f + shakeX).roundToInt().toFloat()
    val y = FLOOR - bodyH
    val lockX = CX + shakeX

    px(x + 3f, FLOOR, w - 6f, 3f, Ink.Shadow)

    if (open > 0f) {
        val inner = (13f * open).roundToInt().toFloat()
        px(x + 2f, y - inner, w - 4f, inner + 2f, Ink.Night)
        px(x + 2f, y - inner, w - 4f, 2f, Ink.GoldDark)
        px(x + 4f, y - inner + 2f, w - 8f, inner, Ink.GoldLight, alpha = 0.55f * open)
    }

    px(x, y, w, bodyH, Ink.GoldDark)
    px(x + 2f, y + 2f, w - 4f, bodyH - 4f, fill)
    px(x + 2f, y + 2f, w - 4f, 2f, light)
    px(x, y, 5f, bodyH, Ink.Gold)
    px(x + w - 5f, y, 5f, bodyH, Ink.Gold)
    px(x, FLOOR - 3f, w, 3f, Ink.Gold)
    px(x, y, w, 2f, Ink.Gold)
    px(x, y, 2f, bodyH, Ink.GoldLight)
    px(lockX - 5f, y + 3f, 10f, 10f, Ink.Gold)
    px(lockX - 5f, y + 3f, 10f, 2f, Ink.GoldLight)
    px(lockX - 1f, y + 7f, 2f, 4f, Ink.Night)

    val lift = (open * 22f).roundToInt().toFloat()
    val lidH = (lidFull - open * 6f).roundToInt().toFloat()
    val lidY = y - lidH - lift + (if (open > 0f) 0f else 1f)

    px(x + 3f, lidY, w - 6f, lidH, Ink.GoldDark)
    px(x, lidY + 3f, w, lidH - 3f, Ink.GoldDark)
    px(x + 2f, lidY + 3f, w - 4f, lidH - 5f, light)
    px(x + 5f, lidY + 2f, w - 10f, 2f, pale)
    px(x + 8f, lidY, 5f, lidH, Ink.Gold)
    px(x + w - 13f, lidY, 5f, lidH, Ink.Gold)
    px(x, lidY + lidH - 2f, w, 2f, Ink.Gold)
}

/** O recibo da compra: de onde sai o cashback. */
private fun DrawScope.receipt(bob: Float) {
    val x = CX - 13f
    val y = FLOOR - 44f + bob

    px(x + 3f, FLOOR, 20f, 3f, Ink.Shadow)
    px(x, y, 26f, 40f, Ink.White)
    for (i in 0 until 6) px(x + i * 5f, y + 40f, 3f, 3f, Ink.White)
    px(x + 4f, y + 6f, 18f, 2f, Ink.Gray)
    px(x + 4f, y + 12f, 12f, 2f, Ink.Gray)
    px(x + 4f, y + 18f, 16f, 2f, Ink.Gray)
    px(x + 7f, y + 26f, 12f, 9f, Ink.Green)
    px(x + 10f, y + 31f, 2f, 2f, Ink.Night)
    px(x + 12f, y + 29f, 2f, 4f, Ink.Night)
    px(x + 14f, y + 27f, 2f, 4f, Ink.Night)
}

private fun DrawScope.trophy(cx: Float, cy: Float) {
    px(cx - 8f, cy - 9f, 16f, 3f, Ink.GoldLight)
    px(cx - 7f, cy - 6f, 14f, 8f, Ink.Gold)
    px(cx - 11f, cy - 6f, 3f, 5f, Ink.Gold)
    px(cx + 8f, cy - 6f, 3f, 5f, Ink.Gold)
    px(cx - 5f, cy + 2f, 10f, 2f, Ink.GoldDark)
    px(cx - 2f, cy + 4f, 4f, 4f, Ink.Gold)
    px(cx - 6f, cy + 8f, 12f, 3f, Ink.GoldDark)
    px(cx - 5f, cy - 5f, 2f, 5f, Ink.GoldLight)
}

private fun DrawScope.sparkles(clock: Float) {
    for (i in 0 until 6) {
        val phase = clock * 2f + i * 1.7f
        val glow = sin(phase)

        if (glow > 0.2f) {
            val x = (CX + cos(i * 2.1f) * 68f).roundToInt().toFloat()
            val y = (FLOOR - 40f + sin(i * 1.3f) * 46f).roundToInt().toFloat()
            val arm = if (glow > 0.75f) 3f else 2f

            px(x - arm, y, arm * 2f + 1f, 1f, Ink.White)
            px(x, y - arm, 1f, arm * 2f + 1f, Ink.White)
        }
    }
}

private fun DrawScope.rays(mouthY: Float, life: Float, clock: Float, color: Color) {
    for (i in 0 until 9) {
        val angle = -PI.toFloat() / 2f + (i - 4) * 0.36f + sin(clock * 1.6f + i) * 0.05f
        val spread = 0.085f
        val length = 420f
        val path = Path().apply {
            moveTo(CX, mouthY)
            lineTo(CX + cos(angle - spread) * length, mouthY + sin(angle - spread) * length)
            lineTo(CX + cos(angle + spread) * length, mouthY + sin(angle + spread) * length)
            close()
        }

        drawPath(path = path, color = color, alpha = 0.2f * life)
    }

    px(CX - 12f, mouthY - 420f, 24f, 420f, color, alpha = 0.28f * life)
}

/** As duas fitas brancas em espiral que sobem do baú. */
private fun DrawScope.ribbons(mouthY: Float, progress: Float, life: Float, clock: Float) {
    if (life <= 0f) return

    for (ribbon in 0 until 2) {
        var s = 0f

        while (s <= progress) {
            val y = mouthY - s * 210f
            val x = CX + sin(s * 10f + clock * 5f + ribbon * PI.toFloat()) * (6f + s * 38f)

            px(x - 1f, y - 1f, 3f, 3f, Ink.White, alpha = life * (1f - s * 0.35f))
            s += 0.012f
        }
    }
}

private fun DrawScope.coins(engine: RewardEngine, currency: RewardCurrency) {
    val xp = currency == RewardCurrency.Xp
    val face = if (xp) Ink.Soft else Ink.Gold
    val shine = if (xp) Ink.Pale else Ink.GoldLight
    val edge = if (xp) Ink.Brand else Ink.GoldDark

    for (coin in engine.coins) {
        if (!coin.visible) continue

        // A moeda gira: de frente é um quadrado, de lado é um risco.
        val wide = if (abs(sin(coin.age * 9f + coin.spin)) > 0.35f) coin.size else 2f
        val x = coin.x - wide / 2f
        val y = coin.y - coin.size / 2f

        px(x, y, wide, coin.size, face)

        if (wide > 2f) {
            px(x, y, wide, 1f, shine)
            px(x + wide - 1f, y + 1f, 1f, coin.size - 1f, edge)
        }
    }
}
