package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import com.example.R
import kotlin.math.exp
import kotlin.math.sin

enum class SoundEffect {
    CLICK,
    SAVE,
    EDIT,
    DELETE
}

/**
 * AppSoundManager2D: 100% Isolated In-App Audio Assets (Zero Android System Audio Integration)
 *  - ZERO calls to view.playSoundEffect(SoundEffectConstants.CLICK)
 *  - ZERO calls to audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK)
 *  - ZERO calls to RingtoneManager
 *  - ZERO low-frequency metallic sine-wave synthesizers ("ကွမ်" tone)
 *  - Decoupled media stream: AudioAttributes.USAGE_GAME
 *  - Plays strictly bundled assets: `res/raw/touch_click.ogg` and `res/raw/success_chime.ogg` via SoundPool
 *  - Silent-safe fallback: in-memory 20ms micro-click buffer at 2400Hz (no phone OS sounds used)
 */
object AppSoundManager2D {
    var isSoundEnabled: Boolean
        get() = AppSoundManager.isSoundEnabled
        set(value) {
            AppSoundManager.isSoundEnabled = value
        }

    private var soundPool: SoundPool? = null
    private var touchSoundId: Int = 0
    private var successSoundId: Int = 0
    private var isLoaded: Boolean = false

    internal var appContext: Context? = null

    /**
     * Initializes SoundPool and preloads in-app audio assets.
     * Decoupled game/media stream (USAGE_GAME) avoids phone system ringtone/effects.
     */
    @Synchronized
    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        if (soundPool != null) return

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME) // Isolated media stream, decoupled from phone system ringtone/effects
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(attributes)
            .build().apply {
                setOnLoadCompleteListener { _, _, status ->
                    if (status == 0) isLoaded = true
                }
            }

        soundPool?.let { pool ->
            touchSoundId = try {
                pool.load(app, R.raw.touch_click, 1)
            } catch (_: Exception) { 0 }

            successSoundId = try {
                pool.load(app, R.raw.success_chime, 1)
            } catch (_: Exception) { 0 }
        }
    }

    /**
     * In-App Touch Click (Plays ONLY res/raw/touch_click.ogg)
     * Zero dependence on Android System Click / Ringtone.
     */
    fun playTouchClick(view: View? = null, context: Context? = null) {
        if (!isSoundEnabled) return
        val targetContext = context ?: appContext
        if (targetContext != null && soundPool == null) {
            init(targetContext)
        }

        if (soundPool != null && touchSoundId != 0 && isLoaded) {
            try {
                soundPool?.play(touchSoundId, 0.75f, 0.75f, 1, 0, 1.0f)
                return
            } catch (_: Throwable) {}
        }

        // Silent-safe fallback: Clean 20ms micro-click in memory (No phone OS sound used)
        playDirectMicroClick()
    }

    /**
     * In-App Success Chime (Plays ONLY res/raw/success_chime.ogg)
     */
    fun playComplete(context: Context? = null) {
        if (!isSoundEnabled) return
        val targetContext = context ?: appContext
        if (targetContext != null && soundPool == null) {
            init(targetContext)
        }

        if (soundPool != null && successSoundId != 0 && isLoaded) {
            try {
                soundPool?.play(successSoundId, 0.9f, 0.9f, 1, 0, 1.0f)
            } catch (_: Throwable) {
                // High-pitch dual chime fallback
                playDirectSuccessTone()
            }
        } else {
            // High-pitch dual chime fallback
            playDirectSuccessTone()
        }

        // Subtle 30ms haptic feedback
        if (targetContext != null) {
            triggerSubtleHaptic(targetContext)
        }
    }

    private fun triggerSubtleHaptic(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(30)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Direct 20ms High-frequency micro-click (2400Hz steep decay, prevents "ကွမ်" echo)
     */
    private fun playDirectMicroClick() {
        Thread {
            try {
                val sampleRate = 44100
                val durationMs = 20
                val numSamples = (durationMs * sampleRate) / 1000
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val envelope = exp(-10.0 * progress) // Instant decay
                    val sample = sin(2.0 * Math.PI * 2400.0 * (i.toDouble() / sampleRate)) * envelope * 0.35
                    buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }

                @Suppress("DEPRECATION")
                val track = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buffer.size * 2,
                    AudioTrack.MODE_STATIC
                )
                track.write(buffer, 0, buffer.size)
                track.play()
                Thread.sleep(durationMs.toLong() + 10)
                track.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    /**
     * Direct high chime fallback (1046.5Hz -> 1318.5Hz, 180ms total)
     */
    private fun playDirectSuccessTone() {
        Thread {
            try {
                val sampleRate = 44100
                val totalMs = 180
                val numSamples = (totalMs * sampleRate) / 1000
                val buffer = ShortArray(numSamples)
                val split = (0.07 * sampleRate).toInt()

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val sample = if (i < split) {
                        val env = exp(-5.0 * (i.toDouble() / split))
                        sin(2.0 * Math.PI * 1046.5 * t) * env * 0.4
                    } else {
                        val env = exp(-6.0 * ((i - split).toDouble() / (numSamples - split)))
                        sin(2.0 * Math.PI * 1318.5 * t) * env * 0.5
                    }
                    buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }

                @Suppress("DEPRECATION")
                val track = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buffer.size * 2,
                    AudioTrack.MODE_STATIC
                )
                track.write(buffer, 0, buffer.size)
                track.play()
                Thread.sleep(totalMs.toLong() + 20)
                track.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    @Synchronized
    fun release() {
        try {
            soundPool?.release()
        } catch (_: Throwable) {}
        soundPool = null
        touchSoundId = 0
        successSoundId = 0
        isLoaded = false
        appContext = null
    }
}

/**
 * AppSoundManager: Primary interface for sound effects in the 2D app.
 * Retains backward compatibility while delegating directly to AppSoundManager2D.
 */
object AppSoundManager {
    @Volatile
    var isSoundEnabled: Boolean = true

    internal var appContext: Context? = null

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        AppSoundManager2D.init(app)
    }

    // 1. Play In-App Touch Click ("တစ်/တချက်" sound)
    fun playTap(view: View? = null, context: Context? = null, record: Boolean = true) {
        if (!isSoundEnabled) return
        if (record) {
            SoundManager.recordTap()
        }
        AppSoundManager2D.playTouchClick(view = view, context = context ?: appContext)
    }

    // 2. Play In-App Complete Sound + Subtle Haptic
    fun playComplete(context: Context? = null) {
        if (!isSoundEnabled) return
        SoundManager.recordComplete()
        AppSoundManager2D.playComplete(context ?: appContext)
    }

    fun release() {
        AppSoundManager2D.release()
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
        AppSoundManager2D.init(app)
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
        AppSoundManager2D.release()
        appContext = null
    }
}
