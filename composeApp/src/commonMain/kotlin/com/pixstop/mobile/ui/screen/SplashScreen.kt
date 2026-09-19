package com.pixstop.mobile.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.pixstop.mobile.domain.model.MascotState
import com.pixstop.mobile.ui.components.PixelMascot
import com.pixstop.mobile.ui.components.gridPattern
import com.pixstop.mobile.ui.components.scanlines
import com.pixstop.mobile.ui.theme.PixColors

/**
 * Tela de Splash: o mascote, o LED da fita, acendendo em ciclo RGB.
 *
 * É a primeira espera do app, e a espera é o único lugar onde o mascote cicla
 * as cores (docs/plans/MASCOTE_LED.md). O ciclo já diz "carregando", então a
 * tela não tem mais a palavra com cursor nem a barra de três blocos: dois
 * indicadores de espera juntos diriam a mesma coisa duas vezes. O splash
 * nativo do Android (`splash_logo.png`) é o mesmo mascote parado, para a troca
 * de um para o outro não piscar.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "splash_alpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2000)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PixColors.Dark)
            .gridPattern()
            .scanlines(),
        contentAlignment = Alignment.Center
    ) {
        PixelMascot(
            modifier = Modifier.alpha(alphaAnim),
            state = MascotState.Loading,
            size = 160.dp,
        )
    }
}
