package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import com.example.R
import java.util.concurrent.atomic.AtomicBoolean

enum class SoundEffect {
    CLICK,
    SAVE,
    EDIT,
    DELETE
}

/**
 * AppSoundManager2D: In-App Bundled Audio Assets (SoundPool) for Touch Click & Complete Sounds.
 * - Zero OS system clicks or RingtoneManager phone sounds.
 * - SoundPool loads bundled assets: `res/raw/touch_click.ogg` and `res/raw/success_chime.ogg`.
 * - In-memory AudioTrack synthesized tone fallback if assets are not loaded or SoundPool fails.
 * - Subtle haptic vibration feedback for completions.
 */
object AppSoundManager2D {
    var isSoundEnabled: Boolean
        get() = AppSoundManager.isSoundEnabled
        set(value) {
            AppSoundManager.isSoundEnabled = value
        }

    private var soundPool: SoundPool? = null
    private var touchSoundId: Int = 0
    private var completeSoundId: Int = 0
    private val touchSoundLoaded = AtomicBoolean(false)
    private val completeSoundLoaded = AtomicBoolean(false)

    internal var appContext: Context? = null

    // Pre-computed fallback PCM buffers for zero-latency synthesized audio
    private val clickPcm: ShortArray by lazy {
        val sampleRate = 44100
        val durationMs = 25
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = kotlin.math.exp(-i.toDouble() / (sampleRate * 0.005))
            val sample = kotlin.math.sin(2.0 * kotlin.math.PI * 1800.0 * t) * env
            buffer[i] = (sample * Short.MAX_VALUE * 0.7).toInt().toShort()
        }
        buffer
    }

    private val chimePcm: ShortArray by lazy {
        val sampleRate = 44100
        val durationMs = 300
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val env = kotlin.math.exp(-i.toDouble() / (sampleRate * 0.07))
            val sample = (0.7 * kotlin.math.sin(2.0 * kotlin.math.PI * 1760.0 * t) +
                    0.3 * kotlin.math.sin(2.0 * kotlin.math.PI * 3520.0 * t)) * env
            buffer[i] = (sample * Short.MAX_VALUE * 0.6).toInt().toShort()
        }
        buffer
    }

    @Synchronized
    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        if (soundPool != null) return

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(audioAttributes)
                .build().apply {
                    setOnLoadCompleteListener { _, sampleId, status ->
                        if (status == 0) {
                            if (sampleId == touchSoundId) touchSoundLoaded.set(true)
                            if (sampleId == completeSoundId) completeSoundLoaded.set(true)
                        }
                    }
                }

            touchSoundId = soundPool?.load(app, R.raw.touch_click, 1) ?: 0
            completeSoundId = soundPool?.load(app, R.raw.success_chime, 1) ?: 0
        } catch (_: Throwable) {
            touchSoundId = 0
            completeSoundId = 0
        }
    }

    /**
     * Play in-app mobile touch click sound ("တစ်/တချက်" style).
     * Uses SoundPool touch_click.ogg, falling back to synthesized AudioTrack if not loaded.
     */
    fun playTouchClick(view: View? = null, context: Context? = null) {
        if (!isSoundEnabled) return
        val targetContext = context ?: appContext
        if (targetContext != null && soundPool == null) {
            init(targetContext)
        }

        if (soundPool != null && touchSoundId != 0 && touchSoundLoaded.get()) {
            try {
                soundPool?.play(touchSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
                return
            } catch (_: Throwable) {}
        }

        // Synthesized AudioTrack fallback (no OS click or RingtoneManager)
        playSynthesizedClick()
    }

    /**
     * Play in-app completion chime sound with subtle haptic vibration.
     * Uses SoundPool success_chime.ogg, falling back to synthesized AudioTrack chime.
     */
    fun playComplete(context: Context? = null) {
        if (!isSoundEnabled) return
        val targetContext = context ?: appContext
        if (targetContext != null && soundPool == null) {
            init(targetContext)
        }

        if (soundPool != null && completeSoundId != 0 && completeSoundLoaded.get()) {
            try {
                soundPool?.play(completeSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
            } catch (_: Throwable) {
                playSynthesizedChime()
            }
        } else {
            playSynthesizedChime()
        }

        vibrateSubtle(targetContext)
    }

    private fun playSynthesizedClick() {
        try {
            val sampleRate = 44100
            val pcm = clickPcm
            val track = AudioTrack.Builder()
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
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(pcm, 0, pcm.size)
            track.play()
            Thread {
                try {
                    Thread.sleep(60)
                    track.stop()
                    track.release()
                } catch (_: Throwable) {}
            }.start()
        } catch (_: Throwable) {}
    }

    private fun playSynthesizedChime() {
        try {
            val sampleRate = 44100
            val pcm = chimePcm
            val track = AudioTrack.Builder()
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
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(pcm, 0, pcm.size)
            track.play()
            Thread {
                try {
                    Thread.sleep(350)
                    track.stop()
                    track.release()
                } catch (_: Throwable) {}
            }.start()
        } catch (_: Throwable) {}
    }

    private fun vibrateSubtle(context: Context?) {
        val ctx = context ?: appContext ?: return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(45)
            }
        } catch (_: Throwable) {}
    }

    @Synchronized
    fun release() {
        try {
            soundPool?.release()
        } catch (_: Throwable) {}
        soundPool = null
        touchSoundId = 0
        completeSoundId = 0
        touchSoundLoaded.set(false)
        completeSoundLoaded.set(false)
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
