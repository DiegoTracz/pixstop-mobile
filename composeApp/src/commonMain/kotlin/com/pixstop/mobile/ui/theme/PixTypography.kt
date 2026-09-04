package com.pixstop.mobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import pixstop_mobile.composeapp.generated.resources.Res
import pixstop_mobile.composeapp.generated.resources.inter_regular
import pixstop_mobile.composeapp.generated.resources.press_start_2p

/**
 * Tipografia retro 8-bit do PixStop.
 *
 * - Press Start 2P: títulos, labels, botões, badges (fonte 8-bit)
 * - Inter: texto corrido, descrições, valores de formulários
 */
object PixTypography {

    val pixelFontFamily: FontFamily
        @Composable get() = FontFamily(Font(Res.font.press_start_2p))

    val sansFontFamily: FontFamily
        @Composable get() = FontFamily(Font(Res.font.inter_regular))

    // ── Títulos (Press Start 2P) ──

    val pageTitle: TextStyle
        @Composable get() = TextStyle(
            fontFamily = pixelFontFamily,
            fontSize = 16.sp,
            color = PixColors.Cyan
        )

    val sectionTitle: TextStyle
        @Composable get() = TextStyle(
            fontFamily = pixelFontFamily,
            fontSize = 12.sp,
            color = PixColors.Cyan
        )

    val inputLabel: TextStyle
        @Composable get() = TextStyle(
            fontFamily = pixelFontFamily,
            fontSize = 8.sp,
            color = PixColors.Cyan
        )

    val buttonText: TextStyle
        @Composable get() = TextStyle(
            fontFamily = pixelFontFamily,
            fontSize = 10.sp,
            letterSpacing = 2.sp
        )

    val buttonTextSm: TextStyle
        @Composable get() = TextStyle(
            fontFamily = pixelFontFamily,
            fontSize = 8.sp,
            letterSpacing = 2.sp
        )

    val badgeText: TextStyle
        @Composable get() = TextStyle(
            fontFamily = pixelFontFamily,
            fontSize = 6.sp
        )

    val errorText: TextStyle
        @Composable get() = TextStyle(
            fontFamily = pixelFontFamily,
            fontSize = 6.sp,
            color = PixColors.Pink
        )

    val branding: TextStyle
        @Composable get() = TextStyle(
            fontFamily = pixelFontFamily,
            fontSize = 11.sp,
            color = PixColors.Cyan
        )

    val terminalHeader: TextStyle
        @Composable get() = TextStyle(
            fontFamily = pixelFontFamily,
            fontSize = 7.sp,
            color = PixColors.Cyan.copy(alpha = 0.6f)
        )

    /**
     * Texto de apoio em sans-serif.
     *
     * A fonte pixelada é linda em título e ilegível em texto miúdo; abaixo de
     * 10sp ela vira ruído. Aqui a legibilidade vence o estilo.
     */
    val caption: TextStyle
        @Composable get() = TextStyle(
            fontFamily = sansFontFamily,
            fontSize = 12.sp,
            color = PixColors.Gray300
        )

    val footerText: TextStyle
        @Composable get() = TextStyle(
            fontFamily = pixelFontFamily,
            fontSize = 6.sp,
            color = PixColors.Cyan.copy(alpha = 0.4f)
        )

    val authTitle: TextStyle
        @Composable get() = TextStyle(
            fontFamily = pixelFontFamily,
            fontSize = 14.sp,
            color = PixColors.Cyan
        )

    // ── Corpo (Inter) ──

    val bodyRegular: TextStyle
        @Composable get() = TextStyle(
            fontFamily = sansFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = PixColors.Gray100
        )

    val bodySecondary: TextStyle
        @Composable get() = TextStyle(
            fontFamily = sansFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = PixColors.Gray400
        )

    val bodyMuted: TextStyle
        @Composable get() = TextStyle(
            fontFamily = sansFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = PixColors.Gray300
        )

    val placeholder: TextStyle
        @Composable get() = TextStyle(
            fontFamily = sansFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = PixColors.Gray500
        )

    val link: TextStyle
        @Composable get() = TextStyle(
            fontFamily = sansFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = PixColors.Cyan
        )

    val inputValue: TextStyle
        @Composable get() = TextStyle(
            fontFamily = sansFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = PixColors.Gray100
        )
}

