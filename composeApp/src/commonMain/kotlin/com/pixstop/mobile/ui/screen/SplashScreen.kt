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
import com.pixstop.mobile.ui.components.PixelFlagSprite
import com.pixstop.mobile.ui.components.PixelWordmark
import com.pixstop.mobile.ui.components.gridPattern
import com.pixstop.mobile.ui.components.scanlines
import com.pixstop.mobile.ui.theme.PixColors

/**
 * Tela de Splash: a bandeira da marca tremulando, com o nome embaixo.
 *
 * A bandeira em movimento já diz "carregando", então a tela não tem palavra
 * com cursor nem barra de progresso: dois indicadores de espera juntos diriam
 * a mesma coisa duas vezes. O splash nativo do Android (`splash_logo.png`) é
 * a mesma bandeira parada, no mesmo tamanho e no mesmo fundo, para a troca de
 * um para o outro não piscar.
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
        Column(
            modifier = Modifier.alpha(alphaAnim),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            PixelFlagSprite(height = 136.dp)
            PixelWordmark(height = 28.dp)
        }
    }
}
