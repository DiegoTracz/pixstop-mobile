package com.pixstop.mobile.ui.components.reward

import androidx.compose.runtime.Composable

/**
 * No iPhone a recompensa ainda é muda: a animação e a vibração tocam, e o
 * som sintetizado (AVAudioEngine) fica para quando houver como testar lá.
 */
@Composable
actual fun rememberRewardSound(): RewardSound = SilentRewardSound

private object SilentRewardSound : RewardSound {
    override fun play(tones: List<RewardTone>) = Unit
}
