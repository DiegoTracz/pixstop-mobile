package com.pixstop.mobile.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * ╔════════════════════════════════════════════════════════════════════════════╗
 * ║                    CONFIGURAÇÃO DE CORES - ALTERE AQUI                     ║
 * ╠════════════════════════════════════════════════════════════════════════════╣
 * ║  Este arquivo centraliza todas as cores do aplicativo.                     ║
 * ║  Altere os valores abaixo para personalizar o visual do app.               ║
 * ╚════════════════════════════════════════════════════════════════════════════╝
 */
object AppColors {

    // ══════════════════════════════════════════════════════════════════════════
    // 🎨 CORES PRINCIPAIS - ALTERE AQUI
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Cor primária do aplicativo.
     * Usada em botões, links, elementos de destaque.
     *
     * 🔧 Para alterar: use um código hexadecimal (0xFF + 6 dígitos)
     * Exemplo: 0xFF6200EE (roxo), 0xFF2196F3 (azul), 0xFF4CAF50 (verde)
     *
     * 💡 Dica: Use https://m3.material.io/theme-builder para gerar paletas
     */
    val Primary = Color(0xFF6200EE)

    /**
     * Cor do texto/ícones sobre a cor primária.
     * Normalmente branco ou preto para bom contraste.
     */
    val OnPrimary = Color(0xFFFFFFFF)

    /**
     * Container da cor primária (versão mais clara).
     * Usada em cards, chips, backgrounds de destaque.
     */
    val PrimaryContainer = Color(0xFFE8DEF8)

    /**
     * Cor do texto sobre o container primário.
     */
    val OnPrimaryContainer = Color(0xFF21005D)

    // ══════════════════════════════════════════════════════════════════════════
    // 🎨 CORES SECUNDÁRIAS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Cor secundária do aplicativo.
     * Usada em elementos complementares, FABs secundários.
     */
    val Secondary = Color(0xFF03DAC6)

    val OnSecondary = Color(0xFF000000)

    val SecondaryContainer = Color(0xFFCCF3EF)

    val OnSecondaryContainer = Color(0xFF00201D)

    // ══════════════════════════════════════════════════════════════════════════
    // 🎨 CORES TERCIÁRIAS
    // ══════════════════════════════════════════════════════════════════════════

    val Tertiary = Color(0xFF7D5260)

    val OnTertiary = Color(0xFFFFFFFF)

    val TertiaryContainer = Color(0xFFFFD8E4)

    val OnTertiaryContainer = Color(0xFF31111D)

    // ══════════════════════════════════════════════════════════════════════════
    // 🎨 CORES DE FUNDO E SUPERFÍCIE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Cor de fundo geral do aplicativo.
     */
    val Background = Color(0xFFFFFBFE)

    val OnBackground = Color(0xFF1C1B1F)

    /**
     * Cor de superfícies (cards, dialogs, menus).
     */
    val Surface = Color(0xFFFFFBFE)

    val OnSurface = Color(0xFF1C1B1F)

    val SurfaceVariant = Color(0xFFE7E0EC)

    val OnSurfaceVariant = Color(0xFF49454F)

    // ══════════════════════════════════════════════════════════════════════════
    // 🎨 CORES DE ERRO
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Cor para mensagens e estados de erro.
     */
    val Error = Color(0xFFB3261E)

    val OnError = Color(0xFFFFFFFF)

    val ErrorContainer = Color(0xFFF9DEDC)

    val OnErrorContainer = Color(0xFF410E0B)

    // ══════════════════════════════════════════════════════════════════════════
    // 🎨 OUTRAS CORES
    // ══════════════════════════════════════════════════════════════════════════

    val Outline = Color(0xFF79747E)

    val OutlineVariant = Color(0xFFCAC4D0)

    val Scrim = Color(0xFF000000)

    // ══════════════════════════════════════════════════════════════════════════
    // 🌙 CORES DO TEMA ESCURO - ALTERE PARA DARK MODE
    // ══════════════════════════════════════════════════════════════════════════

    object Dark {
        val Primary = Color(0xFFD0BCFF)
        val OnPrimary = Color(0xFF381E72)
        val PrimaryContainer = Color(0xFF4F378B)
        val OnPrimaryContainer = Color(0xFFEADDFF)

        val Secondary = Color(0xFFCCC2DC)
        val OnSecondary = Color(0xFF332D41)
        val SecondaryContainer = Color(0xFF4A4458)
        val OnSecondaryContainer = Color(0xFFE8DEF8)

        val Tertiary = Color(0xFFEFB8C8)
        val OnTertiary = Color(0xFF492532)
        val TertiaryContainer = Color(0xFF633B48)
        val OnTertiaryContainer = Color(0xFFFFD8E4)

        val Background = Color(0xFF1C1B1F)
        val OnBackground = Color(0xFFE6E1E5)

        val Surface = Color(0xFF1C1B1F)
        val OnSurface = Color(0xFFE6E1E5)
        val SurfaceVariant = Color(0xFF49454F)
        val OnSurfaceVariant = Color(0xFFCAC4D0)

        val Error = Color(0xFFF2B8B5)
        val OnError = Color(0xFF601410)
        val ErrorContainer = Color(0xFF8C1D18)
        val OnErrorContainer = Color(0xFFF9DEDC)

        val Outline = Color(0xFF938F99)
        val OutlineVariant = Color(0xFF49454F)
        val Scrim = Color(0xFF000000)
    }
}
