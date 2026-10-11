package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class SoundManager(private val context: Context) {

    private val sampleRate = 22050
    private val sfxExecutor = Executors.newFixedThreadPool(2)
    private val musicScope = CoroutineScope(Dispatchers.Default)
    private var musicJob: Job? = null

    var sfxEnabled: Boolean = true
    var musicEnabled: Boolean = true
    var sfxVolume: Float = 0.8f
    var musicVolume: Float = 0.6f

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun vibrate(durationMs: Long = 30) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun playPcm(samples: ShortArray, volume: Float) {
        if (!sfxEnabled || volume <= 0.01f) return
        sfxExecutor.execute {
            try {
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = maxOf(minBuf, samples.size * 2)

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, samples.size)
                audioTrack.setVolume(volume * sfxVolume)
                audioTrack.play()

                val durationMs = (samples.size * 1000L) / sampleRate + 50
                Thread.sleep(durationMs)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {}
        }
    }

    fun playJump() {
        if (!sfxEnabled) return
        vibrate(25)
        val duration = 0.22f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val freq = 220f + 650f * progress
            val env = 1f - progress
            val sample = sin(2.0 * PI * freq * i / sampleRate).toFloat() * env
            buffer[i] = (sample * 32767 * 0.7f).toInt().coerceIn(-32768, 32767).toShort()
        }
        playPcm(buffer, 0.7f)
    }

    fun playCoin() {
        if (!sfxEnabled) return
        vibrate(20)
        val duration = 0.16f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        val half = numSamples / 2

        for (i in 0 until numSamples) {
            val freq = if (i < half) 987.77f else 1318.51f // B5 then E6
            val env = 1f - (i.toFloat() / numSamples)
            val sample = (sin(2.0 * PI * freq * i / sampleRate) * 0.8f +
                    sin(4.0 * PI * freq * i / sampleRate) * 0.2f).toFloat() * env
            buffer[i] = (sample * 32767 * 0.65f).toInt().coerceIn(-32768, 32767).toShort()
        }
        playPcm(buffer, 0.75f)
    }

    fun playBoost() {
        if (!sfxEnabled) return
        vibrate(40)
        val duration = 0.35f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val freq = 300f + 900f * (progress * progress)
            val env = if (progress < 0.2f) progress / 0.2f else 1f - (progress - 0.2f) / 0.8f
            val sample = (sin(2.0 * PI * freq * i / sampleRate) * 0.7f +
                    (Random.nextFloat() * 2f - 1f) * 0.3f) * env
            buffer[i] = (sample * 32767 * 0.6f).toInt().coerceIn(-32768, 32767).toShort()
        }
        playPcm(buffer, 0.75f)
    }

    fun playCollision() {
        if (!sfxEnabled) return
        vibrate(80)
        val duration = 0.45f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val decay = (1f - progress) * (1f - progress)
            val noise = (Random.nextFloat() * 2f - 1f) * 0.6f
            val lowBoom = sin(2.0 * PI * (90f - 60f * progress) * i / sampleRate).toFloat() * 0.4f
            val sample = (noise + lowBoom) * decay
            buffer[i] = (sample * 32767 * 0.85f).toInt().coerceIn(-32768, 32767).toShort()
        }
        playPcm(buffer, 0.85f)
    }

    fun playLevelComplete() {
        if (!sfxEnabled) return
        vibrate(100)
        val notes = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f) // C5, E5, G5, C6
        val noteDuration = 0.15f
        val numSamples = (sampleRate * noteDuration * notes.size).toInt()
        val buffer = ShortArray(numSamples)
        val noteSamples = (sampleRate * noteDuration).toInt()

        for (n in notes.indices) {
            val freq = notes[n]
            val offset = n * noteSamples
            for (i in 0 until noteSamples) {
                val idx = offset + i
                if (idx < numSamples) {
                    val progress = i.toFloat() / noteSamples
                    val env = 1f - progress * 0.7f
                    val sample = (sin(2.0 * PI * freq * i / sampleRate) * 0.7f +
                            sin(4.0 * PI * freq * i / sampleRate) * 0.3f).toFloat() * env
                    buffer[idx] = (sample * 32767 * 0.75f).toInt().coerceIn(-32768, 32767).toShort()
                }
            }
        }
        playPcm(buffer, 0.9f)
    }

    fun playButtonClick() {
        if (!sfxEnabled) return
        val duration = 0.05f
        val numSamples = (sampleRate * duration).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val env = 1f - (i.toFloat() / numSamples)
            val sample = sin(2.0 * PI * 800.0 * i / sampleRate).toFloat() * env
            buffer[i] = (sample * 32767 * 0.5f).toInt().coerceIn(-32768, 32767).toShort()
        }
        playPcm(buffer, 0.5f)
    }

    fun startMusic() {
        if (musicJob != null && musicJob?.isActive == true) return
        musicJob = musicScope.launch {
            // Procedural synthwave bassline & melody loop
            val chordBass = floatArrayOf(130.81f, 130.81f, 164.81f, 146.83f) // C3, C3, E3, D3
            val melodyNotes = floatArrayOf(
                261.63f, 329.63f, 392.00f, 523.25f,
                392.00f, 493.88f, 587.33f, 392.00f
            )
            var step = 0

            while (isActive) {
                if (musicEnabled && musicVolume > 0.01f) {
                    val bassFreq = chordBass[(step / 4) % chordBass.size]
                    val melFreq = melodyNotes[step % melodyNotes.size]
                    val noteDuration = 0.18f
                    val numSamples = (sampleRate * noteDuration).toInt()
                    val buffer = ShortArray(numSamples)

                    for (i in 0 until numSamples) {
                        val progress = i.toFloat() / numSamples
                        val env = (1f - progress * 0.8f)
                        val bass = sin(2.0 * PI * bassFreq * i / sampleRate).toFloat() * 0.45f
                        val lead = (sin(2.0 * PI * melFreq * i / sampleRate) * 0.4f +
                                sin(4.0 * PI * melFreq * i / sampleRate) * 0.15f).toFloat() * env
                        val beat = if (step % 2 == 0 && i < numSamples / 3) {
                            (Random.nextFloat() * 2f - 1f) * (1f - (i.toFloat() / (numSamples / 3))) * 0.2f
                        } else 0f
                        val sample = (bass + lead + beat) * musicVolume * 0.45f
                        buffer[i] = (sample * 32767).toInt().coerceIn(-32768, 32767).toShort()
                    }
                    playPcm(buffer, musicVolume * 0.5f)
                    step = (step + 1) % 32
                }
                delay(180)
            }
        }
    }

    fun stopMusic() {
        musicJob?.cancel()
        musicJob = null
    }

    fun release() {
        stopMusic()
        sfxExecutor.shutdown()
    }
}
