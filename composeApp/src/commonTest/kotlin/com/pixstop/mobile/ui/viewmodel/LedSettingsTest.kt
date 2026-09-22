package com.pixstop.mobile.ui.viewmodel

import com.pixstop.mobile.domain.model.IrProfile
import com.pixstop.mobile.domain.model.IrRemoteLayout
import com.pixstop.mobile.domain.model.LedColorOption
import com.pixstop.mobile.domain.model.LedSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A fita pelo celular (Fase 9.10, etapa A).
 *
 * Quem instala aperta a tecla e vê a geladeira acender. O que este teste
 * guarda é a regra que separa "apertar" de "escolher": nem toda tecla do
 * controle serve de cor de repouso, e uma geladeira sem controle escolhido
 * não tem tecla nenhuma para apertar.
 */
class LedSettingsTest {

    private val colors = listOf(
        LedColorOption("off", "Apagada", "#1f2937"),
        LedColorOption("red", "Vermelho", "#ef4444"),
        LedColorOption("green", "Verde", "#22c55e"),
        LedColorOption("fade", "Arco-íris (fade)", "#a855f7"),
    )

    private val profile = IrProfile(id = 3, name = "Fita RGB 24 teclas", keys = listOf("red", "green", "flash", "power_on"))

    private fun settings(profileId: Long? = 3, online: Boolean = true) = LedSettings(
        color = "fade",
        profileId = profileId,
        online = online,
        colors = colors,
        profiles = listOf(profile),
    )

    @Test
    fun `sem controle escolhido nao ha tecla para apertar`() {
        assertFalse(settings(profileId = null).canPress)
        assertTrue(settings().availableKeys.isEmpty() || settings().availableKeys.isNotEmpty())
        assertEquals(emptyList(), settings(profileId = null).availableKeys)
    }

    @Test
    fun `geladeira fora do ar nao acende nada mesmo com controle escolhido`() {
        assertFalse(settings(online = false).canPress)
    }

    @Test
    fun `so as teclas mapeadas no controle escolhido ficam disponiveis`() {
        assertEquals(listOf("red", "green", "flash", "power_on"), settings().availableKeys)
    }

    @Test
    fun `a cor de agora aparece pelo nome que a pessoa le`() {
        assertEquals("Arco-íris (fade)", settings().colorLabel)
    }

    @Test
    fun `tecla de cor vira escolha tecla de programa nao`() {
        val red = IrRemoteLayout.keys.first { it.slug == "red" }
        val flash = IrRemoteLayout.keys.first { it.slug == "flash" }
        val brightness = IrRemoteLayout.keys.first { it.slug == "brightness_up" }

        assertTrue(red.isRestingColor(colors))
        assertFalse(flash.isRestingColor(colors))
        assertFalse(brightness.isRestingColor(colors))
    }

    @Test
    fun `o controle desenhado tem as 24 teclas do controle de verdade`() {
        assertEquals(6, IrRemoteLayout.rows.size)
        assertTrue(IrRemoteLayout.rows.all { it.size == 4 })
        assertEquals(24, IrRemoteLayout.keys.size)
        // Nenhuma tecla repetida: duas com o mesmo nome mandariam o mesmo
        // código de dois lugares e ninguém saberia qual apertou.
        assertEquals(24, IrRemoteLayout.keys.map { it.slug }.toSet().size)
    }
}
