package com.pixstop.mobile.domain.model

import com.pixstop.mobile.core.network.apiJson
import com.pixstop.mobile.data.mapper.toDomain
import com.pixstop.mobile.data.remote.dto.ActiveTenantDto
import com.pixstop.mobile.data.remote.dto.OrderDto
import com.pixstop.mobile.data.remote.dto.ProgressionDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A barra de XP só existe onde a empresa ligou a progressão, e o que ela
 * mostra vem pronto do servidor — o app não refaz conta de nível.
 */
class ProgressionTest {

    private val ligada = """
        {"enabled":true,"season":1,"season_ends_at":"2026-12-04","xp_total":130,"level":1,"title":"Explorador",
         "xp_into_level":30,"xp_to_next":120,"progress_percent":20,
         "next_level":{"level":2,"xp_required":250,"reward_pixels":400,"title":"Veterano"},
         "streak_days":3,"last_earned_on":"2026-09-06",
         "levels":[{"level":1,"xp_required":100,"reward_pixels":300,"title":"Explorador","reached":true},
                   {"level":2,"xp_required":250,"reward_pixels":400,"title":"Veterano","reached":false}]}
    """.trimIndent()

    @Test
    fun `progressao desligada vira nulo, e a tela nao desenha barra`() {
        assertNull(apiJson.decodeFromString<ProgressionDto>("""{"enabled":false}""").toDomain())
    }

    @Test
    fun `o me sem o campo continua valendo`() {
        // Servidor antigo, sem progressão: nada quebra, só não há barra.
        val dto = apiJson.decodeFromString<ActiveTenantDto>("""{"id":"1","name":"Alpha"}""")

        assertNull(dto.toDomain().progression)
        assertEquals(0, dto.toDomain().pixelsExpiringSoon)
    }

    @Test
    fun `a barra mede o trecho entre niveis, como o servidor mandou`() {
        val progression = apiJson.decodeFromString<ProgressionDto>(ligada).toDomain()

        assertNotNull(progression)
        assertEquals(0.2f, progression.progressFraction)
        assertEquals("Explorador", progression.displayTitle)
        assertEquals(120, progression.xpToNext)
        assertEquals(400, progression.nextLevel?.rewardPixels)
        assertTrue(progression.hasStreak)
        assertFalse(progression.isAtTop)
        assertTrue(progression.levels[0].reached)
        assertFalse(progression.levels[1].reached)
    }

    @Test
    fun `no topo nao ha proximo e a barra fica cheia`() {
        val json = ligada.replace(""""next_level":{"level":2,"xp_required":250,"reward_pixels":400,"title":"Veterano"},""", """"next_level":null,""")
            .replace(""""progress_percent":20""", """"progress_percent":100""")
        val progression = apiJson.decodeFromString<ProgressionDto>(json).toDomain()

        assertNotNull(progression)
        assertTrue(progression.isAtTop)
        assertEquals(1f, progression.progressFraction)
    }

    @Test
    fun `sem titulo o nivel zero e novato e os outros dizem o numero`() {
        val zero = apiJson.decodeFromString<ProgressionDto>("""{"enabled":true,"level":0}""").toDomain()
        val tres = apiJson.decodeFromString<ProgressionDto>("""{"enabled":true,"level":3}""").toDomain()

        assertEquals("Novato", zero?.displayTitle)
        assertEquals("Nível 3", tres?.displayTitle)
    }

    @Test
    fun `um dia so nao e sequencia`() {
        val umDia = apiJson.decodeFromString<ProgressionDto>("""{"enabled":true,"streak_days":1}""").toDomain()

        assertFalse(umDia!!.hasStreak)
    }

    @Test
    fun `o pedido sabe o que rendeu, e pedidos antigos nao rendem nada`() {
        val comXp = apiJson.decodeFromString<OrderDto>(
            """{"id":9,"status":"paid","xp":{"earned":10,"leveled_up_to":1}}""",
        ).toDomain()
        val antigo = apiJson.decodeFromString<OrderDto>("""{"id":8,"status":"paid"}""").toDomain()

        assertEquals(10, comXp.xp.earned)
        assertEquals(1, comXp.xp.leveledUpTo)
        assertFalse(comXp.xp.isEmpty)
        assertTrue(antigo.xp.isEmpty)
    }
}
