package com.pixstop.mobile.domain.model

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A roda desenhada: 16 fatias, o prêmio maior uma vez só, as repetições espalhadas. */
class PrizeWheelRingTest {

    private val gold = listOf(
        PrizeWheelSlice("50 moedas", 50, false, weight = 65.0),
        PrizeWheelSlice("100 moedas", 100, false, weight = 26.0),
        PrizeWheelSlice("300 moedas", 300, false, weight = 7.0),
        PrizeWheelSlice("Um energético por nossa conta", 1200, true, weight = 2.0),
    )

    @Test
    fun `a roda de ouro tem 16 fatias e o premio maior aparece uma vez`() {
        val ring = PrizeWheelRing.build(gold)

        assertEquals(16, ring.size)
        assertEquals(listOf(10, 4, 1, 1), gold.indices.map { prize -> ring.count { it == prize } })
    }

    @Test
    fun `as repeticoes ficam espalhadas, sem tres iguais seguidas`() {
        val ring = PrizeWheelRing.build(gold)

        assertTrue(ring.indices.none { ring[it] == ring[(it + 1) % 16] && ring[it] == ring[(it + 2) % 16] })
    }

    @Test
    fun `a roda para numa fatia do premio sorteado`() {
        val ring = PrizeWheelRing.build(gold)

        repeat(20) { seed ->
            val slot = PrizeWheelRing.slotFor(ring, prize = 1, random = Random(seed))
            assertEquals(1, ring[slot])
        }
    }

    @Test
    fun `a roda de compra tambem fecha 16`() {
        val purchase = listOf(45.0, 30.0, 15.0, 7.0, 2.5).mapIndexed { i, w -> PrizeWheelSlice("p$i", 10, false, weight = w) } +
            PrizeWheelSlice("maior", 1200, true, weight = 0.5)

        val ring = PrizeWheelRing.build(purchase)

        assertEquals(16, ring.size)
        assertEquals(listOf(6, 5, 2, 1, 1, 1), purchase.indices.map { prize -> ring.count { it == prize } })
    }
}
