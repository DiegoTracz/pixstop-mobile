package com.pixstop.mobile.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.pixstop.mobile.ui.components.gridPattern
import com.pixstop.mobile.ui.components.scanlines
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * Tela de Splash — estilo retro 8-bit.
 * Logo Pixel Art "P", nome PixStop em Press Start 2P, cursor piscando.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Fade in animation
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "splash_alpha"
    )

    // Pixel blink cursor (step animation — abrupto)
    val blinkAlpha by rememberInfiniteTransition(label = "blink").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                1f at 0 using LinearEasing
                1f at 499 using LinearEasing
                0f at 500 using LinearEasing
                0f at 999 using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "cursor_blink"
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(alphaAnim)
        ) {

            // Nome do app
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "PixStop",
                    style = PixTypography.pageTitle
                )
                // Cursor piscando (pixel blink)
                Text(
                    text = "_",
                    style = PixTypography.pageTitle,
                    modifier = Modifier.alpha(blinkAlpha)
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            // Loading bar pixel (3 blocos piscando)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val delayedAlpha by rememberInfiniteTransition(label = "dot_$index").animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = keyframes {
                                durationMillis = 900
                                0.3f at 0
                                1f at 300
                                0.3f at 600
                            },
                            repeatMode = RepeatMode.Restart,
                            initialStartOffset = StartOffset(index * 300)
                        ),
                        label = "dot_alpha_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .alpha(delayedAlpha)
                            .background(PixColors.Cyan)
                    )
                }
            }
        }
    }
}
