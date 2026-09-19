package com.metaldetector.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.metaldetector.app.detection.AlertLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Handles synthesized audio beeps and haptic vibration feedback.
 * Synthesizes pure sine-wave tones dynamically without requiring bundled MP3 assets.
 * Modulation frequency and beep speed increase with alert intensity.
 */
class AlertSoundManager(context: Context) {

    private val appContext = context.applicationContext

    // Vibrator access
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var isSoundEnabled: Boolean = true
    var isVibrationEnabled: Boolean = true

    private var beepJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null

    init {
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
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
                .setBufferSizeInBytes(maxOf(minBufferSize, 4096))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
        } catch (_: Exception) {
            // AudioTrack fallback
        }
    }

    /**
     * Updates alert state. Starts pulse beeps/vibrations if alert is active.
     */
    fun updateAlert(level: AlertLevel, signalStrengthPercent: Float) {
        if (level == AlertLevel.NONE) {
            stopPulse()
            return
        }

        // Adjust pulse interval and frequency based on signal strength
        // Weak: 350ms period, 600 Hz tone
        // Strong: 90ms period, 1400 Hz tone (rapid Geiger-counter style)
        val periodMs = when (level) {
            AlertLevel.WEAK -> (380 - (signalStrengthPercent * 1.5f).toInt()).coerceIn(200, 380)
            AlertLevel.MODERATE -> (200 - (signalStrengthPercent * 0.8f).toInt()).coerceIn(110, 200)
            AlertLevel.STRONG -> (110 - (signalStrengthPercent * 0.4f).toInt()).coerceIn(55, 110)
            AlertLevel.NONE -> 400
        }

        val toneFreq = when (level) {
            AlertLevel.WEAK -> 650.0
            AlertLevel.MODERATE -> 950.0
            AlertLevel.STRONG -> 1350.0
            AlertLevel.NONE -> 500.0
        }

        if (beepJob?.isActive != true) {
            beepJob = scope.launch {
                while (isActive) {
                    if (isSoundEnabled) {
                        playTone(toneFreq, 45)
                    }
                    if (isVibrationEnabled) {
                        triggerVibration(level)
                    }
                    delay(periodMs.toLong())
                }
            }
        }
    }

    private fun playTone(freqHz: Double, durationMs: Int) {
        try {
            val track = audioTrack ?: return
            val numSamples = (durationMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)
            val angularFreq = 2.0 * Math.PI * freqHz / sampleRate

            for (i in 0 until numSamples) {
                // Envelope decay to prevent audio pops
                val envelope = when {
                    i < 100 -> i / 100.0
                    i > numSamples - 100 -> (numSamples - i) / 100.0
                    else -> 1.0
                }
                val sample = (sin(angularFreq * i) * 32767 * 0.6 * envelope).toInt()
                buffer[i] = sample.toShort()
            }
            track.write(buffer, 0, numSamples)
        } catch (_: Exception) {
            // Ignore sound synthesis issues in background
        }
    }

    private fun triggerVibration(level: AlertLevel) {
        try {
            val vib = vibrator ?: return
            if (!vib.hasVibrator()) return

            val duration = when (level) {
                AlertLevel.WEAK -> 25L
                AlertLevel.MODERATE -> 45L
                AlertLevel.STRONG -> 75L
                AlertLevel.NONE -> 0L
            }
            if (duration == 0L) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitude = when (level) {
                    AlertLevel.WEAK -> 80
                    AlertLevel.MODERATE -> 160
                    AlertLevel.STRONG -> 255
                    AlertLevel.NONE -> 0
                }
                vib.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(duration)
            }
        } catch (_: Exception) {
            // Ignore permission / vibrator hardware issues
        }
    }

    private fun stopPulse() {
        beepJob?.cancel()
        beepJob = null
    }

    fun release() {
        stopPulse()
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (_: Exception) {}
    }
}
