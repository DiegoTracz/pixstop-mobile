package com.pixstop.mobile.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * O modo claro não é o escuro com outra cor de fundo.
 *
 * O aplicativo escreve `PixColors.Gray100` em cima de `PixColors.Dark` em toda
 * parte, e essa dupla precisa se ler nos dois modos. Estas contas são a rede
 * de segurança: mexer numa cor sem mexer no par que ela acompanha quebra aqui,
 * e não numa tela que ninguém abriu.
 */
class PixPaletteTest {

    /** Contraste WCAG entre duas cores opacas. */
    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05

        return if (la > lb) la / lb else lb / la
    }

    private val paletas = listOf(PixDarkPalette, PixLightPalette)

    @Test
    fun `o texto forte se le sobre o fundo nos dois modos`() {
        paletas.forEach { palette ->
            val razao = contrast(palette.Gray100, palette.Dark)

            assertTrue(razao >= 4.5, "Gray100 sobre Dark ficou em $razao (isDark=${palette.isDark})")
        }
    }

    @Test
    fun `o texto secundario ainda se le nos dois modos`() {
        paletas.forEach { palette ->
            val razao = contrast(palette.Gray300, palette.Dark)

            assertTrue(razao >= 3.0, "Gray300 sobre Dark ficou em $razao (isDark=${palette.isDark})")
        }
    }

    @Test
    fun `o botao principal se le - a cor do chao sobre o acento`() {
        paletas.forEach { palette ->
            val razao = contrast(palette.Dark, palette.Cyan)

            assertTrue(razao >= 4.5, "Dark sobre Cyan ficou em $razao (isDark=${palette.isDark})")
        }
    }

    @Test
    fun `o titulo em ciano se le sobre o fundo`() {
        paletas.forEach { palette ->
            val razao = contrast(palette.Cyan, palette.Dark)

            assertTrue(razao >= 4.5, "Cyan sobre Dark ficou em $razao (isDark=${palette.isDark})")
        }
    }

    @Test
    fun `o rosa tambem escreve com a cor do chao`() {
        // O contador de avisos, o selo de desconto e o botão destrutivo
        // pintam o fundo de rosa. Escreviam em branco, que sobre o rosa claro
        // do modo escuro dava 2,7 de contraste — abaixo de qualquer mínimo.
        paletas.forEach { palette ->
            val razao = contrast(palette.Dark, palette.Pink)

            assertTrue(razao >= 4.5, "Dark sobre Pink ficou em $razao (isDark=${palette.isDark})")
        }
    }

    @Test
    fun `a escala inverte de um modo para o outro`() {
        // No escuro Gray100 é claro; no claro, escuro. Trocar um sem o outro
        // é o erro que faz uma tela inteira sumir.
        assertTrue(PixDarkPalette.Gray100.luminance() > PixDarkPalette.Gray900.luminance())
        assertTrue(PixLightPalette.Gray100.luminance() < PixLightPalette.Gray900.luminance())
    }

    @Test
    fun `o modo guardado volta como foi escolhido e o desconhecido cai no sistema`() {
        assertEquals(AppThemeMode.Light, AppThemeMode.fromStored("Light"))
        assertEquals(AppThemeMode.Dark, AppThemeMode.fromStored("Dark"))
        assertEquals(AppThemeMode.System, AppThemeMode.fromStored(null))
        assertEquals(AppThemeMode.System, AppThemeMode.fromStored("Sepia"))
    }
}
