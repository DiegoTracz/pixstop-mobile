package com.pixstop.mobile.domain.model

import com.pixstop.mobile.core.network.apiJson
import com.pixstop.mobile.data.mapper.toDomain
import com.pixstop.mobile.data.remote.dto.OrderDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * O que um pedido rendeu vira cenas, do menor prêmio para o maior. O
 * cashback toca em toda compra; o resto só quando aconteceu.
 */
class OrderRewardsTest {

    @Test
    fun `uma compra comum tem so a cena curta do cashback`() {
        val scenes = OrderRewards(cashbackPixels = 60).scenes()

        assertEquals(listOf(RewardKind.Cashback), scenes.map { it.kind })
        assertEquals("+60 PIXELS", scenes.single().title)
        assertTrue(scenes.single().opensByItself)
        assertEquals(60, scenes.single().gain)
    }

    @Test
    fun `cashback cota medalhas e nivel tocam nessa ordem`() {
        val rewards = OrderRewards(
            cashbackPixels = 60,
            level = LevelReward(3, "Frequentador", 350),
            quota = QuotaReward("Clube dos 200", 150, "latas", 80),
            medals = listOf(MedalReward("challenge_joined", "Entrou na roda", 10), MedalReward("challenge_closer", "Fechou a cota", 20)),
        )

        val scenes = rewards.scenes()

        assertEquals(listOf(RewardKind.Cashback, RewardKind.Quota, RewardKind.Medal, RewardKind.Level), scenes.map { it.kind })
        assertEquals("150 latas · Clube dos 200", scenes[1].subtitle)
        assertEquals(RewardChest.Green, scenes[1].chest)
        assertEquals("2 MEDALHAS!", scenes[2].title)
        assertEquals("Entrou na roda · Fechou a cota", scenes[2].subtitle)
        assertEquals("+30 XP", scenes[2].prize)
        assertTrue(scenes[2].trophy)
        assertEquals("LEVEL 3", scenes[3].tag)
        assertEquals("Nível 3 · Frequentador", scenes[3].subtitle)
        assertEquals("+350 PIXELS", scenes[3].prize)
        assertFalse(scenes[3].opensByItself)
    }

    @Test
    fun `premio que nao pagou nada nao solta moedas`() {
        val scenes = OrderRewards(level = LevelReward(2, null, 0), quota = QuotaReward("X", 10, "latas", 0)).scenes()

        assertTrue(scenes.all { it.coins == 0 && it.gain == 0 })
        assertEquals("Nível 2", scenes.last().subtitle)
    }

    @Test
    fun `o pedido do servidor traz as recompensas e o servidor antigo traz nenhuma`() {
        val json = """
            {"id":20,"status":"paid","paid_at":"2026-11-15T15:14:20+00:00",
             "rewards":{"cashback_pixels":60,"level":{"level":2,"title":"Explorador","pixels":300},
               "quota":null,"medals":[{"code":"challenge_joined","title":"Entrou na roda","xp":10}]}}
        """.trimIndent()

        val order = apiJson.decodeFromString<OrderDto>(json).toDomain()

        assertEquals(60, order.rewards.cashbackPixels)
        assertEquals(300, order.rewards.level?.pixels)
        assertEquals("Entrou na roda", order.rewards.medals.single().title)
        assertEquals("2026-11-15T15:14:20+00:00", order.paidAt)
        assertTrue(apiJson.decodeFromString<OrderDto>("""{"id":1,"status":"paid"}""").toDomain().rewards.isEmpty)
    }
}
