package com.pixstop.mobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * A paleta do Pixstop, nos dois modos.
 *
 * Os nomes são **papéis**, não cores literais: `Dark` é o chão da tela e
 * `Gray100` é o texto mais forte, tanto no escuro quanto no claro. Foi a
 * escolha que permitiu acender a luz no aplicativo inteiro sem reescrever as
 * 650 chamadas espalhadas pelas telas — o preço é que, no modo claro,
 * `Dark` vale quase branco. Ler `Dark` como "o fundo" resolve o estranhamento.
 *
 * A escala de cinza inverte de um modo para o outro. Os acentos não: neon em
 * cima de branco não se lê, então cada cor tem uma versão fechada para o modo
 * claro, escolhida para passar em contraste sobre o fundo dele.
 */
data class PixPalette(
    // Chão e superfícies
    val Dark: Color,
    val Darker: Color,

    // Acentos
    val Cyan: Color,
    val Green: Color,
    val Yellow: Color,
    val Orange: Color,
    val Purple: Color,
    val Pink: Color,
    val Blue: Color,

    // Escala: Gray100 é o texto mais forte; Gray900 o fundo mais fundo.
    val Gray100: Color,
    val Gray200: Color,
    val Gray300: Color,
    val Gray400: Color,
    val Gray500: Color,
    val Gray600: Color,
    val Gray700: Color,
    val Gray800: Color,
    val Gray900: Color,

    // Transparências e apoios
    val CyanShadow: Color,
    val YellowDim: Color,
    val CyanAlpha10: Color,
    val CyanAlpha20: Color,
    val CyanAlpha40: Color,
    val GreenAlpha20: Color,
    val PinkAlpha20: Color,
    val PinkAlpha40: Color,

    /** Branco e preto de verdade: o QR do PIX precisa dos dois em qualquer modo. */
    val White: Color = Color(0xFFFFFFFF),
    val Black: Color = Color(0xFF000000),
    val Transparent: Color = Color(0x00000000),

    /** Qual modo está no ar, para quem precisa decidir por conta própria. */
    val isDark: Boolean,
)

/** O escuro de sempre, do MOBILE_STYLE_GUIDE. */
val PixDarkPalette = PixPalette(
    Dark = Color(0xFF050816),
    Darker = Color(0xFF030510),

    Cyan = Color(0xFF00F5D4),
    Green = Color(0xFF00E676),
    Yellow = Color(0xFFFFD700),
    Orange = Color(0xFFFF9100),
    Purple = Color(0xFF9D4EDD),
    Pink = Color(0xFFFF6B9D),
    Blue = Color(0xFF00B4D8),

    Gray100 = Color(0xFFE2E8F0),
    Gray200 = Color(0xFFCBD5E1),
    Gray300 = Color(0xFF94A3B8),
    Gray400 = Color(0xFF64748B),
    Gray500 = Color(0xFF475569),
    Gray600 = Color(0xFF334155),
    Gray700 = Color(0xFF1E293B),
    Gray800 = Color(0xFF0F172A),
    Gray900 = Color(0xFF0A0F1A),

    CyanShadow = Color(0xFF0A7D6E),
    YellowDim = Color(0x80FFD700),
    CyanAlpha10 = Color(0x1A00F5D4),
    CyanAlpha20 = Color(0x3300F5D4),
    CyanAlpha40 = Color(0x6600F5D4),
    GreenAlpha20 = Color(0x3300E676),
    PinkAlpha20 = Color(0x33FF6B9D),
    PinkAlpha40 = Color(0x66FF6B9D),

    isDark = true,
)

/**
 * O claro.
 *
 * A escala inverte e os acentos fecham: o ciano neon vira o teal do símbolo
 * da marca, que é o mesmo tom da sombra do logotipo — a identidade continua
 * reconhecível, e o texto se lê.
 */
val PixLightPalette = PixPalette(
    Dark = Color(0xFFF1F5F9),
    Darker = Color(0xFFFFFFFF),

    Cyan = Color(0xFF06776A),
    Green = Color(0xFF0F8F4E),
    Yellow = Color(0xFF9A6B00),
    Orange = Color(0xFFC25E00),
    Purple = Color(0xFF7B2CBF),
    Pink = Color(0xFFC2185B),
    Blue = Color(0xFF0077A3),

    Gray100 = Color(0xFF0F172A),
    Gray200 = Color(0xFF1E293B),
    Gray300 = Color(0xFF475569),
    Gray400 = Color(0xFF64748B),
    Gray500 = Color(0xFF94A3B8),
    Gray600 = Color(0xFFB6C2D2),
    Gray700 = Color(0xFFD9E1EA),
    Gray800 = Color(0xFFFFFFFF),
    Gray900 = Color(0xFFE7EDF3),

    CyanShadow = Color(0xFF9AD3C9),
    YellowDim = Color(0x809A6B00),
    CyanAlpha10 = Color(0x1A06776A),
    CyanAlpha20 = Color(0x3306776A),
    CyanAlpha40 = Color(0x6606776A),
    GreenAlpha20 = Color(0x330F8F4E),
    PinkAlpha20 = Color(0x33C2185B),
    PinkAlpha40 = Color(0x66C2185B),

    isDark = false,
)

val LocalPixPalette = staticCompositionLocalOf { PixDarkPalette }

/**
 * A paleta do momento.
 *
 * Continua se escrevendo `PixColors.Cyan` em toda a interface; o que mudou é
 * que o valor agora depende do tema escolhido.
 */
val PixColors: PixPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalPixPalette.current
