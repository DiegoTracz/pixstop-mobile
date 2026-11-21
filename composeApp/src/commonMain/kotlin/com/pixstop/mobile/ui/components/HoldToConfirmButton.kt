package com.pixstop.mobile.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography
import kotlinx.coroutines.launch

/**
 * Segurar para confirmar: a barra enche enquanto o dedo está em cima, e
 * soltar antes cancela. Meio segundo basta para não ser sem querer.
 */
@Composable
fun HoldToConfirmButton(
    text: String,
    enabled: Boolean,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = PixColors.Green,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val accent = if (enabled) color else PixColors.Gray600

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(PixColors.Darker)
            .border(2.dp, accent)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        val fillUp = scope.launch {
                            progress.animateTo(1f, tween(durationMillis = 600))
                            onConfirm()
                        }
                        tryAwaitRelease()
                        if (progress.value < 1f) {
                            fillUp.cancel()
                            scope.launch { progress.animateTo(0f, tween(durationMillis = 150)) }
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(progress.value)
                .background(accent.copy(alpha = 0.35f)),
        )
        Text(text = text, style = PixTypography.bodySecondary, color = if (enabled) PixColors.Gray100 else PixColors.Gray500)
    }
}

