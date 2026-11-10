package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.SoundEffectConstants
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class SoundEffect {
    CLICK,
    SAVE,
    EDIT,
    DELETE
}

/**
 * AppSoundManager: Built-in Procedural "Soft Pop" Tap Sound and "Bell Ding" Complete Sound
 * - Tap Sound ("Soft Pop"):
 *   Lightweight, organic bubble pop sound generated dynamically using a short sinewave
 *   pitch-drop (1100 Hz down to 250 Hz over ~45ms) with a smooth exponential decay envelope.
 * - Complete Sound ("Bell Ding"):
 *   Clear, pleasant metallic bell resonance generated using harmonic frequencies
 *   (Primary 1760 Hz [A6] + secondary 3520 Hz overtone over ~450ms) with exponential decay,
 *   accompanied by a subtle 50ms haptic feedback vibration.
 * Zero external audio files or asset dependencies required.
 */
object AppSoundManager {
    @Volatile
    var isSoundEnabled: Boolean = true

    private var appContext: Context? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Pre-computed audio PCM buffers for zero-latency instant playback
    private val softPopPcm: ShortArray by lazy { generateSoftPopBuffer() }
    private val bellDingPcm: ShortArray by lazy { generateBellDingBuffer() }

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // 1. Play "Soft Pop" Tap Sound
    fun playTap(view: View? = null, context: Context? = null, record: Boolean = true) {
        if (!isSoundEnabled) return
        if (record) {
            SoundManager.recordTap()
        }

        try {
            if (view != null) {
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
        } catch (_: Throwable) {}

        scope.launch {
            playRawPcm(softPopPcm, sampleRate = 44100)
        }
    }

    // 2. Play "Bell Ding" Complete Sound + Subtle Haptic
    fun playComplete(context: Context? = null) {
        if (!isSoundEnabled) return
        SoundManager.recordComplete()

        scope.launch {
            playRawPcm(bellDingPcm, sampleRate = 44100)
        }

        // Trigger subtle haptic feedback (50ms)
        val targetContext = context ?: appContext
        targetContext?.let { triggerHaptic(it) }
    }

    // Generate short pitch-drop "Soft Pop" (45ms)
    private fun generateSoftPopBuffer(): ShortArray {
        val sampleRate = 44100
        val durationMs = 45
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        var phase = 0.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / numSamples
            // Pitch sweeps from 1100 Hz down to 250 Hz
            val freq = 1100.0 * (1.0 - t * 0.77)
            phase += 2.0 * PI * freq / sampleRate

            // Fast exponential decay envelope
            val envelope = exp(-t * 6.5)
            val sample = (sin(phase) * envelope * Short.MAX_VALUE * 0.75).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    // Generate resonant "Bell Ding" chime (450ms)
    private fun generateBellDingBuffer(): ShortArray {
        val sampleRate = 44100
        val durationMs = 450
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        val fundamentalFreq = 1760.0 // A6 high clear tone
        val harmonicFreq = 3520.0    // A7 shimmering overtone

        for (i in 0 until numSamples) {
            val timeSec = i.toDouble() / sampleRate

            // Natural bell sound envelope decay
            val envFund = exp(-timeSec * 8.0)
            val envHarm = exp(-timeSec * 16.0)

            val wave = 0.75 * sin(2.0 * PI * fundamentalFreq * timeSec) * envFund +
                       0.25 * sin(2.0 * PI * harmonicFreq * timeSec) * envHarm

            val sample = (wave * Short.MAX_VALUE * 0.70).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    // Playback helper via AudioTrack
    private fun playRawPcm(pcmData: ShortArray, sampleRate: Int) {
        try {
            val audioTrack = AudioTrack.Builder()
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
                .setBufferSizeInBytes(pcmData.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(pcmData, 0, pcmData.size)
            audioTrack.play()

            // Release after completion
            scope.launch {
                delay(500)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {
            // Graceful fallback in environments without physical audio hardware
        }
    }

    private fun triggerHaptic(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        } catch (_: Throwable) {}
    }

    fun release() {
        appContext = null
    }
}

object SoundManager {
    private var appContext: Context? = null
    private var lastClickTimestampMs: Long = 0L

    var isSoundEnabled: Boolean
        get() = AppSoundManager.isSoundEnabled
        set(value) {
            AppSoundManager.isSoundEnabled = value
        }

    private val _playedEffects = mutableListOf<SoundEffect>()
    val playedEffects: List<SoundEffect>
        get() = synchronized(_playedEffects) { _playedEffects.toList() }

    val clickCount: Int
        get() = synchronized(_playedEffects) { _playedEffects.count { it == SoundEffect.CLICK } }

    val saveCount: Int
        get() = synchronized(_playedEffects) { _playedEffects.count { it == SoundEffect.SAVE } }

    fun clearRecordedEffects() {
        synchronized(_playedEffects) {
            _playedEffects.clear()
        }
        lastClickTimestampMs = 0L
    }

    internal fun recordTap() {
        synchronized(_playedEffects) {
            _playedEffects.add(SoundEffect.CLICK)
        }
    }

    internal fun recordComplete() {
        synchronized(_playedEffects) {
            _playedEffects.add(SoundEffect.SAVE)
        }
    }

    @Synchronized
    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        AppSoundManager.init(app)
    }

    fun playTap(view: View? = null, context: Context? = null): Boolean {
        if (!isSoundEnabled) return false
        AppSoundManager.playTap(view, context ?: appContext, record = true)
        return true
    }

    fun playComplete(context: Context? = null): Boolean {
        if (!isSoundEnabled) return false
        AppSoundManager.playComplete(context ?: appContext)
        return true
    }

    fun playClick(debounceMs: Long = 0L, view: View? = null, context: Context? = null): Boolean {
        if (!isSoundEnabled) return false
        if (debounceMs > 0L) {
            val now = SystemClock.uptimeMillis()
            if (now - lastClickTimestampMs < debounceMs) {
                return false
            }
            lastClickTimestampMs = now
        }
        return playTap(view, context)
    }

    fun playSave(context: Context? = null): Boolean = playComplete(context)

    fun playEdit(view: View? = null, context: Context? = null): Boolean {
        if (!isSoundEnabled) return false
        synchronized(_playedEffects) { _playedEffects.add(SoundEffect.EDIT) }
        AppSoundManager.playTap(view, context ?: appContext, record = false)
        return true
    }

    fun playDelete(view: View? = null, context: Context? = null): Boolean {
        if (!isSoundEnabled) return false
        synchronized(_playedEffects) { _playedEffects.add(SoundEffect.DELETE) }
        AppSoundManager.playTap(view, context ?: appContext, record = false)
        return true
    }

    @Synchronized
    fun release() {
        AppSoundManager.release()
        appContext = null
    }
}
