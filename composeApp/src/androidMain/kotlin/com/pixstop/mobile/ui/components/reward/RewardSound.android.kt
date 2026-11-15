package com.pixstop.mobile.ui.components.reward

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@Composable
actual fun rememberRewardSound(): RewardSound {
    val sound = remember { AndroidRewardSound() }

    DisposableEffect(sound) {
        onDispose { sound.release() }
    }

    return sound
}

/**
 * Gera as notas em PCM e toca por um `AudioTrack` de vida curta.
 *
 * Usa o canal de sons do sistema: com o aparelho no silencioso, a recompensa
 * fica muda — ninguém quer um baú tocando fanfarra no meio do escritório sem
 * ter pedido.
 */
private class AndroidRewardSound : RewardSound {

    private val worker = Executors.newSingleThreadScheduledExecutor()

    override fun play(tones: List<RewardTone>) {
        if (tones.isEmpty() || worker.isShutdown) {
            return
        }

        runCatching {
            worker.execute {
                runCatching {
                    val samples = render(tones)
                    val track = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build(),
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build(),
                        )
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .setBufferSizeInBytes(samples.size * 2)
                        .build()

                    track.write(samples, 0, samples.size)
                    track.play()

                    val millis = samples.size * 1000L / SAMPLE_RATE + 80
                    worker.schedule({ runCatching { track.release() } }, millis, TimeUnit.MILLISECONDS)
                }
            }
        }
    }

    fun release() {
        runCatching { worker.shutdown() }
    }

    private fun render(tones: List<RewardTone>): ShortArray {
        val totalMillis = tones.maxOf { it.delayMillis + it.durationMillis }
        val mix = FloatArray(SAMPLE_RATE * totalMillis / 1000 + 1)

        for (tone in tones) {
            val start = SAMPLE_RATE * tone.delayMillis / 1000
            val length = SAMPLE_RATE * tone.durationMillis / 1000
            val period = SAMPLE_RATE / tone.frequency

            for (i in 0 until length) {
                val phase = (i % period) / period
                val wave = if (tone.saw) phase * 2f - 1f else if (phase < 0.5f) 1f else -1f
                val envelope = 1f - i.toFloat() / length

                mix[start + i] += wave * envelope * tone.volume * MASTER
            }
        }

        return ShortArray(mix.size) { (mix[it].coerceIn(-1f, 1f) * Short.MAX_VALUE).roundToInt().toShort() }
    }

    private companion object {
        const val SAMPLE_RATE = 22_050
        const val MASTER = 0.35f
    }
}
