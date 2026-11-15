package com.pixstop.mobile.domain.reward

import com.pixstop.mobile.domain.model.RewardChest
import com.pixstop.mobile.domain.model.RewardScene
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** O que o motor pede ao som e à vibração enquanto anda. */
sealed interface RewardCue {
    data class ChargeTick(val step: Int) : RewardCue
    data object Burst : RewardCue
    data class CoinLanded(val index: Int) : RewardCue
    data object Reveal : RewardCue
}

enum class RewardPhase { Idle, Charge, Burst, Done }

/** Uma moeda da fonte: sobe, cai, quica e depois voa para o contador. */
class RewardCoin(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var delay: Float,
    val hold: Float,
    val size: Float,
    val spin: Float,
) {
    var age: Float = 0f
    var done: Boolean = false
    internal var homeX: Float = 0f
    internal var homeY: Float = 0f
    internal var homing: Boolean = false

    val visible: Boolean get() = !done && delay <= 0f
}

/**
 * A animação de recompensa, sem tela: os quatro tempos e as moedas.
 *
 * Mora fora do Compose para poder ser conferida sem aparelho — que todas as
 * moedas chegam, e que o contador termina exatamente no valor do prêmio. As
 * coordenadas são as de um palco lógico de 180 × 320, que a tela escala.
 */
class RewardEngine(val scene: RewardScene, private val random: Random = Random.Default) {

    var phase: RewardPhase = RewardPhase.Idle
        private set

    /** Segundos desde o começo do tempo atual. */
    var time: Float = 0f
        private set

    /** Segundos desde o começo da cena: o relógio dos brilhos e das fitas. */
    var clock: Float = 0f
        private set

    val coins: MutableList<RewardCoin> = mutableListOf()

    var arrived: Int = 0
        private set

    /** O que o contador mostra agora. */
    var shown: Int = 0
        private set

    var revealed: Boolean = false
        private set

    private var chargeStep = 0

    val hasChest: Boolean get() = scene.chest != RewardChest.None

    /** De onde as moedas saem: a boca do baú, ou o topo do recibo. */
    val mouthY: Float get() = if (hasChest) FLOOR - 48f else FLOOR - 46f

    /** O toque que abre. Só vale na espera. */
    fun open(): List<RewardCue> {
        if (phase != RewardPhase.Idle) {
            return emptyList()
        }

        if (!hasChest) {
            return burst()
        }

        phase = RewardPhase.Charge
        time = 0f

        return emptyList()
    }

    /** Sem movimento: direto ao resultado, com o contador já no fim. */
    fun skip() {
        phase = RewardPhase.Done
        time = 10f
        revealed = true
        arrived = scene.coins
        shown = scene.gain
        coins.clear()
    }

    fun step(dt: Float): List<RewardCue> {
        time += dt
        clock += dt

        val cues = mutableListOf<RewardCue>()

        if (phase == RewardPhase.Charge) {
            val step = (time / CHARGE_TICK_SECONDS).toInt()

            if (step > chargeStep) {
                chargeStep = step
                cues += RewardCue.ChargeTick(step)
            }

            if (time >= CHARGE_SECONDS) {
                cues += burst()
            }
        }

        stepCoins(dt, cues)

        if (phase == RewardPhase.Burst) {
            if (!revealed && time > (if (hasChest) 0.85f else 0.45f)) {
                revealed = true
                cues += RewardCue.Reveal
            }

            if ((arrived >= scene.coins && time > 1.6f) || time > 4f) {
                phase = RewardPhase.Done
                // Uma moeda perdida no caminho não pode deixar o saldo errado na tela.
                shown = scene.gain
            }
        }

        return cues
    }

    private fun burst(): List<RewardCue> {
        phase = RewardPhase.Burst
        time = 0f

        repeat(scene.coins) { index ->
            coins += RewardCoin(
                x = CX + (random.nextFloat() - 0.5f) * 16f,
                y = mouthY,
                vx = (random.nextFloat() - 0.5f) * 150f,
                vy = -150f - random.nextFloat() * 150f,
                delay = index * 0.028f,
                hold = 0.75f + random.nextFloat() * 0.55f,
                size = if (random.nextFloat() < 0.3f) 6f else 5f,
                spin = random.nextFloat() * 6f,
            )
        }

        return listOf(RewardCue.Burst)
    }

    private fun stepCoins(dt: Float, cues: MutableList<RewardCue>) {
        for (coin in coins) {
            if (coin.done) continue

            if (coin.delay > 0f) {
                coin.delay -= dt
                continue
            }

            coin.age += dt

            if (coin.age < coin.hold) {
                coin.vy += GRAVITY * dt
                coin.x += coin.vx * dt
                coin.y += coin.vy * dt

                if (coin.y > FLOOR - 2f && coin.vy > 0f) {
                    coin.y = FLOOR - 2f
                    coin.vy *= -0.45f
                    coin.vx *= 0.7f
                }

                continue
            }

            if (!coin.homing) {
                coin.homing = true
                coin.homeX = coin.x
                coin.homeY = coin.y
            }

            val u = ((coin.age - coin.hold) / HOMING_SECONDS).coerceAtMost(1f)
            val ease = u * u * u

            coin.x = coin.homeX + (HUD_X - coin.homeX) * ease
            coin.y = coin.homeY + (HUD_Y - coin.homeY) * ease - sin(u * PI.toFloat()) * 18f

            if (u >= 1f) {
                coin.done = true
                arrived++
                shown = if (arrived >= scene.coins) scene.gain else (scene.gain.toFloat() * arrived / scene.coins).roundToInt()
                cues += RewardCue.CoinLanded(arrived)
            }
        }
    }

    companion object {
        const val WIDTH = 180f
        const val HEIGHT = 320f
        const val CX = 90f
        const val FLOOR = 214f
        const val HUD_X = 152f
        const val HUD_Y = 16f

        const val CHARGE_SECONDS = 0.6f
        const val CHARGE_TICK_SECONDS = 0.075f
        const val HOMING_SECONDS = 0.42f
        const val GRAVITY = 420f
    }
}
