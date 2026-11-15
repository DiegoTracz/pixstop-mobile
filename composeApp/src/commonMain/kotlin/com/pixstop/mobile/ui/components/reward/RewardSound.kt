package com.pixstop.mobile.ui.components.reward

import androidx.compose.runtime.Composable

/** Uma nota de 8 bits: onda quadrada (ou dente de serra) que some sozinha. */
data class RewardTone(
    val frequency: Float,
    val durationMillis: Int,
    val delayMillis: Int = 0,
    val volume: Float = 0.5f,
    val saw: Boolean = false,
)

/**
 * O som das recompensas, sintetizado na hora: não há arquivo de áudio no
 * app. Tocar nunca falha para quem chama — sem som, a animação segue.
 */
interface RewardSound {
    fun play(tones: List<RewardTone>)
}

/** As frases do som: a carga subindo, o estouro, a moeda chegando e a fanfarra. */
object RewardTunes {
    fun chargeTick(step: Int) = listOf(RewardTone(220f + step * 45f, 60, volume = 0.4f))

    val burst = listOf(RewardTone(110f, 320, volume = 0.6f, saw = true)) +
        listOf(523f, 659f, 784f, 1047f).mapIndexed { index, frequency -> RewardTone(frequency, 120, delayMillis = 50 + index * 70) }

    fun coin(index: Int) = listOf(RewardTone(880f + index * 22f, 50, volume = 0.35f))

    val reveal = listOf(784f, 988f, 1175f, 1568f).mapIndexed { index, frequency -> RewardTone(frequency, 160, delayMillis = index * 90, volume = 0.45f) }
}

@Composable
expect fun rememberRewardSound(): RewardSound
