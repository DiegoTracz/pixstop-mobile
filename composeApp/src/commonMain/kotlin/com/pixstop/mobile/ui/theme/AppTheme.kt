package com.pixstop.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Tema retro 8-bit do PixStop — Dark only.
 * Cores mapeadas de PixColors para o Material3 color scheme.
 */
private val PixStopColorScheme = darkColorScheme(
    primary = PixColors.Cyan,
    onPrimary = PixColors.Dark,
    primaryContainer = PixColors.Gray800,
    onPrimaryContainer = PixColors.Cyan,

    secondary = PixColors.Purple,
    onSecondary = PixColors.White,
    secondaryContainer = PixColors.Gray700,
    onSecondaryContainer = PixColors.Purple,

    tertiary = PixColors.Yellow,
    onTertiary = PixColors.Dark,
    tertiaryContainer = PixColors.Gray700,
    onTertiaryContainer = PixColors.Yellow,

    background = PixColors.Dark,
    onBackground = PixColors.Gray100,

    surface = PixColors.Gray800,
    onSurface = PixColors.Gray100,
    surfaceVariant = PixColors.Gray700,
    onSurfaceVariant = PixColors.Gray300,

    error = PixColors.Pink,
    onError = PixColors.White,
    errorContainer = PixColors.PinkAlpha20,
    onErrorContainer = PixColors.Pink,

    outline = PixColors.Gray600,
    outlineVariant = PixColors.Gray700,
    scrim = PixColors.Black
)

/**
 * Tema principal do PixStop — Dark only, retro 8-bit.
 */
@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PixStopColorScheme,
        content = content
    )
}
