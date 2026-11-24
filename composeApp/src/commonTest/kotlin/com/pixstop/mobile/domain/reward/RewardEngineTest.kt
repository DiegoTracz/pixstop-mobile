package com.pixstop.mobile.domain.reward

import com.pixstop.mobile.domain.model.LevelReward
import com.pixstop.mobile.domain.model.OrderRewards
import com.pixstop.mobile.domain.model.RewardScene
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A animação de recompensa sem tela: os quatro tempos e a contagem. O que
 * não pode falhar é o número — o contador termina no valor do prêmio.
 */
class RewardEngineTest {

    private val level: RewardScene = OrderRewards(level = LevelReward(3, "Frequentador", 350)).scenes().single()
    private val cashback: RewardScene = OrderRewards(cashbackPixels = 60).scenes().single()

    private fun run(engine: RewardEngine, seconds: Float): List<RewardCue> {
        val cues = mutableListOf<RewardCue>()
        repeat((seconds / FRAME).toInt()) { cues += engine.step(FRAME) }
        return cues
    }

    @Test
    fun `o bau espera o toque e nao anda sozinho`() {
        val engine = RewardEngine(level, Random(1))

        run(engine, 2f)

        assertEquals(RewardPhase.Idle, engine.phase)
        assertTrue(engine.coins.isEmpty())
    }

    @Test
    fun `o toque carrega estoura e todas as moedas chegam com o valor exato`() {
        val engine = RewardEngine(level, Random(1))

        engine.open()
        assertEquals(RewardPhase.Charge, engine.phase)

        val cues = run(engine, 5f)

        assertEquals(RewardPhase.Done, engine.phase)
        assertEquals(30, engine.arrived)
        assertEquals(350, engine.shown)
        assertTrue(engine.revealed)
        assertTrue(cues.any { it is RewardCue.ChargeTick })
        assertEquals(1, cues.count { it == RewardCue.Burst })
        assertEquals(1, cues.count { it == RewardCue.Reveal })
        assertEquals(30, cues.count { it is RewardCue.CoinLanded })
    }

    @Test
    fun `o contador sobe aos poucos e nunca passa do premio`() {
        val engine = RewardEngine(level, Random(3))
        engine.open()

        var previous = 0
        repeat(400) {
            engine.step(FRAME)
            assertTrue(engine.shown >= previous)
            assertTrue(engine.shown <= 350)
            previous = engine.shown
        }

        assertEquals(350, previous)
    }

    @Test
    fun `o cashback nao tem bau e estoura direto no toque`() {
        val engine = RewardEngine(cashback, Random(1))

        assertFalse(engine.hasChest)
        assertEquals(listOf<RewardCue>(RewardCue.Burst), engine.open())

        run(engine, 3f)

        assertEquals(RewardPhase.Done, engine.phase)
        assertEquals(60, engine.shown)
    }

    @Test
    fun `tocar de novo no meio nao recomeca`() {
        val engine = RewardEngine(level, Random(1))
        engine.open()
        run(engine, 1f)

        assertTrue(engine.open().isEmpty())
        assertEquals(RewardPhase.Burst, engine.phase)
        assertEquals(30, engine.coins.size)
    }

    @Test
    fun `sem movimento a cena vai direto ao resultado`() {
        val engine = RewardEngine(level, Random(1))

        engine.skip()

        assertEquals(RewardPhase.Done, engine.phase)
        assertEquals(350, engine.shown)
        assertTrue(engine.revealed)
    }

    private companion object {
        const val FRAME = 1f / 60f
    }
}
