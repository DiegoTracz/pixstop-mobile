package com.pixstop.mobile.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixstop.mobile.ui.theme.PixColors
import com.pixstop.mobile.ui.theme.PixTypography

/**
 * Variantes do botão pixel.
 */
enum class PixelButtonVariant {
    Primary,
    Secondary,
    Destructive
}

/**
 * Tamanhos do botão pixel.
 */
enum class PixelButtonSize {
    Small,
    Default,
    Large
}

/**
 * Botão retro 8-bit com sombra pixel sólida, cantos retos, estados pressed/disabled.
 */
@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PixelButtonVariant = PixelButtonVariant.Primary,
    buttonSize: PixelButtonSize = PixelButtonSize.Default,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingText: String? = null,
    icon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val btnColors = when (variant) {
        PixelButtonVariant.Primary -> ButtonColors(
            bg = PixColors.Cyan,
            text = PixColors.Dark,
            border = PixColors.Cyan,
            shadow = PixColors.Cyan
        )
        PixelButtonVariant.Secondary -> ButtonColors(
            bg = Color.Transparent,
            text = PixColors.Cyan,
            border = PixColors.Cyan,
            shadow = PixColors.Cyan
        )
        PixelButtonVariant.Destructive -> ButtonColors(
            bg = PixColors.Pink,
            text = PixColors.White,
            border = PixColors.Pink,
            shadow = PixColors.Pink
        )
    }

    val dimensions = when (buttonSize) {
        PixelButtonSize.Small -> ButtonDimensions(
            height = 32.dp,
            paddingH = 16.dp,
            paddingV = 8.dp
        )
        PixelButtonSize.Default -> ButtonDimensions(
            height = 36.dp,
            paddingH = 20.dp,
            paddingV = 10.dp
        )
        PixelButtonSize.Large -> ButtonDimensions(
            height = 40.dp,
            paddingH = 24.dp,
            paddingV = 12.dp
        )
    }

    val shadowOffset = when {
        !enabled || isLoading -> 0.dp
        isPressed -> 2.dp
        else -> 4.dp
    }

    val translateOffset = when {
        !enabled || isLoading -> 0.dp
        isPressed -> 2.dp
        else -> 0.dp
    }

    val animatedShadow by animateDpAsState(
        targetValue = shadowOffset,
        animationSpec = tween(50)
    )
    val animatedTranslate by animateDpAsState(
        targetValue = translateOffset,
        animationSpec = tween(50)
    )

    val textStyle = when (buttonSize) {
        PixelButtonSize.Small -> PixTypography.buttonTextSm
        else -> PixTypography.buttonText
    }

    Box(
        modifier = modifier
            .alpha(if (enabled && !isLoading) 1f else 0.5f)
    ) {
        Box(
            modifier = Modifier
                .offset(x = animatedTranslate, y = animatedTranslate)
                .drawBehind {
                    // Pixel shadow
                    if (animatedShadow > 0.dp) {
                        val shadowPx = animatedShadow.toPx()
                        drawRect(
                            color = btnColors.shadow,
                            topLeft = Offset(shadowPx, shadowPx),
                            size = Size(this.size.width, this.size.height)
                        )
                    }
                }
                .background(btnColors.bg)
                .pixelBorder(btnColors.border)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled && !isLoading,
                    onClick = onClick
                )
                .padding(horizontal = dimensions.paddingH, vertical = dimensions.paddingV),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = btnColors.text,
                        strokeWidth = 2.dp
                    )
                    if (loadingText != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = loadingText.uppercase(),
                            style = textStyle,
                            color = btnColors.text,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    if (icon != null) {
                        icon()
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = text.uppercase(),
                        style = textStyle,
                        color = btnColors.text,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private data class ButtonColors(
    val bg: Color,
    val text: Color,
    val border: Color,
    val shadow: Color
)

private data class ButtonDimensions(
    val height: Dp,
    val paddingH: Dp,
    val paddingV: Dp
)


