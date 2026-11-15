package com.pixstop.mobile.data.mapper

import com.pixstop.mobile.core.network.apiJson
import com.pixstop.mobile.data.remote.dto.ChallengePageDto
import com.pixstop.mobile.data.remote.dto.OrderDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * O desafio coletivo como o servidor manda (`GET /api/challenges/current`).
 * O placar, a frase de baixo e o "+N no desafio" do pedido saem daqui.
 */
class ChallengeMapperTest {

    private val page = """
        {"challenge":{"id":3,"name":"Clube dos 200 Monsters","description":null,"unit_label":"latas",
          "units_total":147,"goal":200,"progress_percent":73,"starts_at":"2026-11-01T00:00:00+00:00",
          "ends_at":"2027-01-31T00:00:00+00:00","has_ended":false,"xp_enabled":true,
          "quotas":[
            {"units_required":25,"reward_xp":30,"title":null,"reached":true,"reached_at":"2026-11-03T10:00:00+00:00","earned":true,"percent":12},
            {"units_required":150,"reward_xp":80,"title":"Quase lá","reached":false,"reached_at":null,"earned":false,"percent":75},
            {"units_required":200,"reward_xp":120,"title":null,"reached":false,"reached_at":null,"earned":false,"percent":100}],
          "next_quota":{"units_required":150,"remaining":3,"reward_xp":80,"title":"Quase lá","in_stretch":true},
          "me":{"units":4,"joined":true,"hidden":false},
          "participants":{"count":14,"names":[{"name":"Ana","is_me":true},{"name":"Bruno","is_me":false}],"more":12},
          "achievements":[]},
         "achievements":[{"code":"challenge_joined","title":"Entrou na roda","description":"Primeira compra num desafio.",
          "icon":"handshake","xp":10,"earned_at":"2026-11-02T10:00:00+00:00","challenge":"Clube dos 200 Monsters"}]}
    """.trimIndent()

    @Test
    fun `o placar e a proxima cota chegam inteiros`() {
        val result = apiJson.decodeFromString<ChallengePageDto>(page).toDomain()
        val challenge = result.challenge!!

        assertEquals(147, challenge.unitsTotal)
        assertEquals(0.735f, challenge.fraction)
        assertEquals(4, challenge.myUnits)
        assertTrue(challenge.hasJoined)
        assertTrue(challenge.nextQuota!!.inStretch)
        assertEquals("Faltam 3 latas para a cota de 150, que rende 80 XP para quem ajudar.", challenge.headline)
        assertEquals("150 · Quase lá", challenge.quotas[1].label)
        assertEquals(12, challenge.moreParticipants)
        assertEquals("Entrou na roda", result.achievements.single().title)
    }

    @Test
    fun `uma lata so concorda no singular`() {
        val challenge = apiJson.decodeFromString<ChallengePageDto>(
            """{"challenge":{"id":1,"name":"X","unit_label":"latas","units_total":5,"goal":15,
               "next_quota":{"units_required":6,"remaining":1,"reward_xp":50},"me":{"units":1}}}""",
        ).toDomain().challenge!!

        assertEquals("Falta 1 lata para a cota de 6, que rende 50 XP para quem ajudar.", challenge.headline)
        assertEquals("lata", challenge.unitsFor(1))
        assertEquals("latas", challenge.unitsFor(2))
    }

    @Test
    fun `sem desafio as medalhas continuam`() {
        val result = apiJson.decodeFromString<ChallengePageDto>(
            """{"challenge":null,"achievements":[{"code":"challenge_closer","title":"Fechou a cota","xp":20}]}""",
        ).toDomain()

        assertNull(result.challenge)
        assertEquals(20, result.achievements.single().xp)
    }

    @Test
    fun `desafio encerrado diz onde parou e todas as cotas batidas comemora`() {
        val ended = apiJson.decodeFromString<ChallengePageDto>(
            """{"challenge":{"id":1,"name":"X","unit_label":"latas","units_total":80,"goal":200,"has_ended":true,"quotas":[]}}""",
        ).toDomain().challenge!!

        assertEquals("Encerrado com 80 latas.", ended.headline)

        val done = apiJson.decodeFromString<ChallengePageDto>(
            """{"challenge":{"id":2,"name":"Y","unit_label":"latas","units_total":210,"goal":200,
               "quotas":[{"units_required":200,"reward_xp":50,"reached":true,"earned":false}]}}""",
        ).toDomain().challenge!!

        assertTrue(done.allQuotasReached)
        assertEquals(1f, done.fraction)
        assertEquals("Todas as cotas batidas!", done.headline)
        assertFalse(done.hasJoined)
    }

    @Test
    fun `o pedido diz quanto somou no desafio`() {
        val json = """
            {"id":9,"status":"paid","totals":{"products":20},"items":[],
             "challenge":{"challenge":"Clube dos 200 Monsters","units":2,"units_total":149,"goal":200,"unit_label":"latas"}}
        """.trimIndent()

        val order = apiJson.decodeFromString<OrderDto>(json).toDomain()

        assertEquals("+2 no desafio · 149/200 latas", order.challenge!!.label)
        assertNull(apiJson.decodeFromString<OrderDto>("""{"id":1,"status":"paid"}""").toDomain().challenge)
    }
}
