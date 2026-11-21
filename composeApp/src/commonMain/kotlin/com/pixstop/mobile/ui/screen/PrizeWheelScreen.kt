package com.pixstop.mobile.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.domain.model.PrizeSpinResult
import com.pixstop.mobile.domain.model.PrizeWheel
import com.pixstop.mobile.domain.model.RewardChest
import com.pixstop.mobile.domain.model.RewardCurrency
import com.pixstop.mobile.domain.model.RewardKind
import com.pixstop.mobile.domain.model.RewardScene
import com.pixstop.mobile.ui.components.PixelButton
import com.pixstop.mobile.ui.components.PixelEmptyState
import com.pixstop.mobile.ui.components.PixelLoader
import com.pixstop.mobile.ui.components.PixelScreenTopBar
import com.pixstop.mobile.ui.components.PrizeWheelArt
import com.pixstop.mobile.ui.components.PrizeWheelLegend
import com.pixstop.mobile.ui.components.reward.RewardCelebration
import com.pixstop.mobile.ui.components.restingAngle
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import com.pixstop.mobile.ui.viewmodel.PrizeWheelViewModel
import kotlin.math.PI
import org.koin.compose.viewmodel.koinViewModel

/**
 * A roleta de prêmios (docs/plans/LANCAMENTO_LOCARMAIS.md no servidor).
 *
 * O servidor sorteia, a roda dá cinco voltas e para na fatia dele. Prêmio
 * grande abre o baú das recompensas; o resto aparece numa linha.
 */
@Composable
fun PrizeWheelScreen(
    onBack: () -> Unit,
    viewModel: PrizeWheelViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var celebration by remember { mutableStateOf<List<RewardScene>>(emptyList()) }

    Box(modifier = Modifier.fillMaxSize().background(PixColors.Dark)) {
        Column(modifier = Modifier.fillMaxSize()) {
            PixelScreenTopBar(title = "Roleta de prêmios", onBack = onBack)

            val wheel = state.wheel

            when {
                state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { PixelLoader() }

                wheel == null -> Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                    PixelEmptyState(
                        message = state.error ?: "Não há roleta girando agora.",
                        isError = state.error != null,
                    )
                }

                else -> WheelContent(
                    wheel = wheel,
                    isSpinning = state.isSpinning,
                    pending = state.pending,
                    lastResult = state.lastResult,
                    error = state.error,
                    onSpin = viewModel::spin,
                    onStopped = { result ->
                        viewModel.onSpinAnimated()
                        celebration = listOf(sceneFor(result))
                    },
                )
            }
        }

        if (celebration.isNotEmpty()) {
            RewardCelebration(scenes = celebration, onFinished = { celebration = emptyList() })
        }
    }
}

@Composable
private fun WheelContent(
    wheel: PrizeWheel,
    isSpinning: Boolean,
    pending: PrizeSpinResult?,
    lastResult: PrizeSpinResult?,
    error: String?,
    onSpin: () -> Unit,
    onStopped: (PrizeSpinResult) -> Unit,
) {
    // A roda que aparece é a do giro em andamento; parada, a do próximo.
    val kind = pending?.kind ?: wheel.nextKind
    val slices = wheel.slicesOf(kind)
    val angle = remember { Animatable(restingAngle(0, wheel.slicesOf(wheel.nextKind).size)) }

    LaunchedEffect(pending) {
        val result = pending ?: return@LaunchedEffect
        val count = wheel.slicesOf(result.kind).size
        val full = (2 * PI).toFloat()
        val base = angle.value - (angle.value % full)
        angle.animateTo(
            targetValue = base - full * 5 + restingAngle(result.sliceIndex, count),
            animationSpec = tween(durationMillis = 3200, easing = CubicBezierEasing(0.15f, 0.85f, 0.35f, 1f)),
        )
        onStopped(result)
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = wheel.name, style = PixTypography.sectionTitle, color = PixColors.Yellow)
            wheel.message?.let { Text(text = it, style = PixTypography.caption, color = PixColors.Gray100) }
            if (wheel.wonPixels > 0) {
                Text(text = "Você já ganhou ${wheel.wonPixels} moedas.", style = PixTypography.caption, color = PixColors.Green)
            }
        }

        PrizeWheelArt(slices = slices, kind = kind, angle = angle.value, size = 260.dp)

        Text(
            text = when {
                wheel.goldSpins > 0 -> "${wheel.goldSpins} ${if (wheel.goldSpins == 1) "giro de ouro" else "giros de ouro"} esperando você" +
                    if (wheel.purchaseSpins > 0) " e ${wheel.purchaseSpins} de compra." else "."
                wheel.purchaseSpins > 0 -> "${wheel.purchaseSpins} ${if (wheel.purchaseSpins == 1) "giro de compra" else "giros de compra"}."
                else -> "Sem giros agora. Pague uma compra a partir de R$ ${formatMoney(wheel.purchaseMinTotal)} com moedas e ganhe um giro por dia."
            },
            style = PixTypography.bodyRegular,
            color = PixColors.Gray100,
            textAlign = TextAlign.Center,
        )

        if (wheel.guaranteedJackpot) {
            Text(
                text = "O seu último giro de ouro tem o prêmio maior garantido, se ele não sair antes.",
                style = PixTypography.caption,
                color = PixColors.Gray400,
                textAlign = TextAlign.Center,
            )
        }

        PixelButton(
            text = if (isSpinning) "GIRANDO..." else "GIRAR",
            onClick = onSpin,
            enabled = !isSpinning && wheel.spinsLeft > 0,
            modifier = Modifier.fillMaxWidth(),
        )

        lastResult?.takeIf { !isSpinning }?.let { result ->
            Text(
                text = if (result.isJackpot) "Prêmio maior! ${result.label}" else "Você ganhou ${result.label}",
                style = PixTypography.sectionTitle,
                color = if (result.isJackpot) PixColors.Yellow else PixColors.Green,
                textAlign = TextAlign.Center,
            )
        }

        error?.let { Text(text = it, style = PixTypography.errorText, color = PixColors.Pink, textAlign = TextAlign.Center) }

        PrizeWheelLegend(
            slices = slices,
            kind = kind,
            highlight = lastResult?.takeIf { !isSpinning && it.kind == kind }?.sliceIndex,
        )

        Text(
            text = "As moedas da roleta pagam a compra inteira e não vencem.",
            style = PixTypography.caption,
            color = PixColors.Gray400,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * A cena de recompensa de todo giro: sempre o baú, mudando só o valor. O
 * prêmio maior ganha o troféu. A fonte pixel não leva acento.
 */
private fun sceneFor(result: PrizeSpinResult) = RewardScene(
    kind = RewardKind.Level,
    tag = if (result.kind == com.pixstop.mobile.domain.model.PrizeSpinKind.Gold) "GIRO DE OURO" else "GIRO DE COMPRA",
    title = if (result.isJackpot) "PREMIO MAIOR!" else "+${result.pixels} MOEDAS",
    subtitle = if (result.isJackpot) result.label else "Você ganhou ${result.label} na roleta",
    prize = "+${result.pixels} MOEDAS",
    currency = RewardCurrency.Pixels,
    gain = result.pixels,
    coins = (result.pixels / 20).coerceIn(8, 30),
    chest = RewardChest.Blue,
    trophy = result.isJackpot,
)

private fun formatMoney(value: Double): String {
    val cents = kotlin.math.round(value * 100).toLong()
    return "${cents / 100},${(cents % 100).toString().padStart(2, '0')}"
}
