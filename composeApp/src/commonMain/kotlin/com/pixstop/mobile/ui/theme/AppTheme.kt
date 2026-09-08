package com.pixstop.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Como a pessoa quer ver o aplicativo.
 *
 * `System` é o padrão porque acompanhar o aparelho é o que quase todo mundo
 * espera — inclusive quem usa o modo escuro automático à noite.
 */
enum class AppThemeMode {
    System,
    Light,
    Dark,
    ;

    val label: String
        get() = when (this) {
            System -> "Do sistema"
            Light -> "Claro"
            Dark -> "Escuro"
        }

    companion object {
        fun fromStored(value: String?): AppThemeMode =
            entries.firstOrNull { it.name == value } ?: System
    }
}

/**
 * O esquema do Material a partir da paleta.
 *
 * O aplicativo desenha quase tudo na mão com o `PixColors`, mas o Material
 * ainda pinta o que é dele — diálogos, gaveta, indicadores — e sem isto essas
 * partes ficariam escuras no meio de uma tela clara.
 */
private fun schemeFor(palette: PixPalette) = if (palette.isDark) {
    darkColorScheme(
        primary = palette.Cyan,
        onPrimary = palette.Dark,
        primaryContainer = palette.Gray800,
        onPrimaryContainer = palette.Cyan,
        secondary = palette.Purple,
        onSecondary = palette.White,
        secondaryContainer = palette.Gray700,
        onSecondaryContainer = palette.Purple,
        tertiary = palette.Yellow,
        onTertiary = palette.Dark,
        tertiaryContainer = palette.Gray700,
        onTertiaryContainer = palette.Yellow,
        background = palette.Dark,
        onBackground = palette.Gray100,
        surface = palette.Gray800,
        onSurface = palette.Gray100,
        surfaceVariant = palette.Gray700,
        onSurfaceVariant = palette.Gray300,
        error = palette.Pink,
        onError = palette.White,
        errorContainer = palette.PinkAlpha20,
        onErrorContainer = palette.Pink,
        outline = palette.Gray600,
        outlineVariant = palette.Gray700,
        scrim = palette.Black,
    )
} else {
    lightColorScheme(
        primary = palette.Cyan,
        onPrimary = palette.White,
        primaryContainer = palette.CyanAlpha10,
        onPrimaryContainer = palette.Cyan,
        secondary = palette.Purple,
        onSecondary = palette.White,
        secondaryContainer = palette.Gray700,
        onSecondaryContainer = palette.Purple,
        tertiary = palette.Yellow,
        onTertiary = palette.White,
        tertiaryContainer = palette.Gray700,
        onTertiaryContainer = palette.Yellow,
        background = palette.Dark,
        onBackground = palette.Gray100,
        surface = palette.Gray800,
        onSurface = palette.Gray100,
        surfaceVariant = palette.Gray700,
        onSurfaceVariant = palette.Gray300,
        error = palette.Pink,
        onError = palette.White,
        errorContainer = palette.PinkAlpha20,
        onErrorContainer = palette.Pink,
        outline = palette.Gray600,
        outlineVariant = palette.Gray700,
        scrim = palette.Black,
    )
}

/**
 * O tema do Pixstop, retro 8-bit, agora nos dois modos.
 */
@Composable
fun AppTheme(
    mode: AppThemeMode = AppThemeMode.System,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        AppThemeMode.System -> isSystemInDarkTheme()
        AppThemeMode.Light -> false
        AppThemeMode.Dark -> true
    }

    val palette = if (dark) PixDarkPalette else PixLightPalette

    SystemBarsEffect(dark = dark)

    CompositionLocalProvider(LocalPixPalette provides palette) {
        MaterialTheme(
            colorScheme = schemeFor(palette),
            content = content,
        )
    }
}
