package com.pixstop.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Tema claro do aplicativo.
 * As cores são definidas em AppColors.kt
 */
private val LightColorScheme = lightColorScheme(
    primary = AppColors.Primary,
    onPrimary = AppColors.OnPrimary,
    primaryContainer = AppColors.PrimaryContainer,
    onPrimaryContainer = AppColors.OnPrimaryContainer,

    secondary = AppColors.Secondary,
    onSecondary = AppColors.OnSecondary,
    secondaryContainer = AppColors.SecondaryContainer,
    onSecondaryContainer = AppColors.OnSecondaryContainer,

    tertiary = AppColors.Tertiary,
    onTertiary = AppColors.OnTertiary,
    tertiaryContainer = AppColors.TertiaryContainer,
    onTertiaryContainer = AppColors.OnTertiaryContainer,

    background = AppColors.Background,
    onBackground = AppColors.OnBackground,

    surface = AppColors.Surface,
    onSurface = AppColors.OnSurface,
    surfaceVariant = AppColors.SurfaceVariant,
    onSurfaceVariant = AppColors.OnSurfaceVariant,

    error = AppColors.Error,
    onError = AppColors.OnError,
    errorContainer = AppColors.ErrorContainer,
    onErrorContainer = AppColors.OnErrorContainer,

    outline = AppColors.Outline,
    outlineVariant = AppColors.OutlineVariant,
    scrim = AppColors.Scrim
)

/**
 * Tema escuro do aplicativo.
 * As cores são definidas em AppColors.Dark
 */
private val DarkColorScheme = darkColorScheme(
    primary = AppColors.Dark.Primary,
    onPrimary = AppColors.Dark.OnPrimary,
    primaryContainer = AppColors.Dark.PrimaryContainer,
    onPrimaryContainer = AppColors.Dark.OnPrimaryContainer,

    secondary = AppColors.Dark.Secondary,
    onSecondary = AppColors.Dark.OnSecondary,
    secondaryContainer = AppColors.Dark.SecondaryContainer,
    onSecondaryContainer = AppColors.Dark.OnSecondaryContainer,

    tertiary = AppColors.Dark.Tertiary,
    onTertiary = AppColors.Dark.OnTertiary,
    tertiaryContainer = AppColors.Dark.TertiaryContainer,
    onTertiaryContainer = AppColors.Dark.OnTertiaryContainer,

    background = AppColors.Dark.Background,
    onBackground = AppColors.Dark.OnBackground,

    surface = AppColors.Dark.Surface,
    onSurface = AppColors.Dark.OnSurface,
    surfaceVariant = AppColors.Dark.SurfaceVariant,
    onSurfaceVariant = AppColors.Dark.OnSurfaceVariant,

    error = AppColors.Dark.Error,
    onError = AppColors.Dark.OnError,
    errorContainer = AppColors.Dark.ErrorContainer,
    onErrorContainer = AppColors.Dark.OnErrorContainer,

    outline = AppColors.Dark.Outline,
    outlineVariant = AppColors.Dark.OutlineVariant,
    scrim = AppColors.Dark.Scrim
)

/**
 * Tema principal do aplicativo.
 *
 * @param darkTheme Se true, usa o tema escuro. Por padrão, segue o sistema.
 * @param content Conteúdo do aplicativo.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
