package com.pixstop.mobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * A paleta do Pixelstop, nos dois modos.
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
 *
 * **A marca é o azul da logo, #015DFA**, a mesma escala do site. `Blue` é
 * esse azul exato; `Cyan`, a cor de destaque que mais aparece, é um tom
 * acima no escuro (#4D8BFF), porque o azul da logo como texto sobre o fundo
 * escuro dá só 3,5:1. `Purple` é o magenta, o destaque secundário.
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
    Dark = Color(0xFF0B1020),
    Darker = Color(0xFF060A14),

    Cyan = Color(0xFF4D8BFF),
    Green = Color(0xFF3DDC97),
    Yellow = Color(0xFFFFC93C),
    Orange = Color(0xFFFF8A3D),
    Purple = Color(0xFFFA00FB),
    Pink = Color(0xFFFF5C5C),
    Blue = Color(0xFF015DFA),

    Gray100 = Color(0xFFEEF2F7),
    Gray200 = Color(0xFFD3DBE6),
    Gray300 = Color(0xFF9AA7BC),
    Gray400 = Color(0xFF7C8AA0),
    Gray500 = Color(0xFF5B6B82),
    Gray600 = Color(0xFF2A3650),
    Gray700 = Color(0xFF1B2540),
    Gray800 = Color(0xFF131B2E),
    Gray900 = Color(0xFF0F1626),

    CyanShadow = Color(0xFF015DFA),
    YellowDim = Color(0x80FFC93C),
    CyanAlpha10 = Color(0x1A4D8BFF),
    CyanAlpha20 = Color(0x334D8BFF),
    CyanAlpha40 = Color(0x664D8BFF),
    GreenAlpha20 = Color(0x333DDC97),
    PinkAlpha20 = Color(0x33FF5C5C),
    PinkAlpha40 = Color(0x66FF5C5C),

    isDark = true,
)

/**
 * O claro.
 *
 * A escala inverte e os acentos fecham: o azul vira o 600 da escala da marca
 * (#0149C8), o mesmo que o site usa no tema claro — a identidade continua
 * reconhecível, e o texto se lê.
 */
val PixLightPalette = PixPalette(
    Dark = Color(0xFFF4F6FA),
    Darker = Color(0xFFFFFFFF),

    Cyan = Color(0xFF0149C8),
    Green = Color(0xFF0E7C53),
    Yellow = Color(0xFF7A5200),
    Orange = Color(0xFFB4530A),
    Purple = Color(0xFFC400C5),
    Pink = Color(0xFFC62828),
    Blue = Color(0xFF0149C8),

    Gray100 = Color(0xFF0F172A),
    Gray200 = Color(0xFF1E293B),
    Gray300 = Color(0xFF3D4B60),
    Gray400 = Color(0xFF5B6B82),
    Gray500 = Color(0xFFA8B4C4),
    Gray600 = Color(0xFFD3DBE6),
    Gray700 = Color(0xFFE9EEF5),
    Gray800 = Color(0xFFFFFFFF),
    Gray900 = Color(0xFFF4F6FA),

    CyanShadow = Color(0xFFADC8FF),
    YellowDim = Color(0x807A5200),
    CyanAlpha10 = Color(0x1A0149C8),
    CyanAlpha20 = Color(0x330149C8),
    CyanAlpha40 = Color(0x660149C8),
    GreenAlpha20 = Color(0x330E7C53),
    PinkAlpha20 = Color(0x33C62828),
    PinkAlpha40 = Color(0x66C62828),

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
