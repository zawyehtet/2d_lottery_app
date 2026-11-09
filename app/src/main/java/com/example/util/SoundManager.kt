package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import com.example.R

enum class SoundEffect {
    CLICK,
    SAVE,
    EDIT,
    DELETE
}

/**
 * AppSoundManager: Low-latency audio feedback manager strictly simplified
 * to two feedback sounds:
 * - Tap Sound (ခလုတ်နှိပ်သံ)
 * - Complete Sound (ပြီးမြောက်သံ)
 */
object AppSoundManager {
    private var soundPool: SoundPool? = null
    private var tapSoundId: Int = 0
    private var completeSoundId: Int = 0
    @Volatile
    var isSoundEnabled: Boolean = true

    fun init(context: Context) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        tapSoundId = soundPool?.load(context, R.raw.sound_tap, 1) ?: 0
        completeSoundId = soundPool?.load(context, R.raw.sound_complete, 1) ?: 0
    }

    fun playTap() {
        if (isSoundEnabled && tapSoundId != 0) {
            soundPool?.play(tapSoundId, 1f, 1f, 0, 0, 1f)
        }
        SoundManager.recordTap()
    }

    fun playComplete() {
        if (isSoundEnabled && completeSoundId != 0) {
            soundPool?.play(completeSoundId, 1f, 1f, 0, 0, 1f)
        }
        SoundManager.recordComplete()
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        tapSoundId = 0
        completeSoundId = 0
    }
}

object SoundManager {
    private var soundPool: SoundPool? = null
    private var clickSoundId: Int = 0
    private var saveSoundId: Int = 0
    private var isInitialized: Boolean = false
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
        val appContext = context.applicationContext
        AppSoundManager.init(appContext)

        if (isInitialized && soundPool != null) return
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val pool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()

        clickSoundId = pool.load(appContext, R.raw.sound_tap, 1)
        saveSoundId = pool.load(appContext, R.raw.sound_complete, 1)

        soundPool = pool
        isInitialized = true
    }

    fun playTap(): Boolean {
        if (!isSoundEnabled) return false
        recordTap()
        val pool = soundPool
        if (pool != null && clickSoundId != 0) {
            pool.play(clickSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
        }
        return true
    }

    fun playComplete(): Boolean {
        if (!isSoundEnabled) return false
        recordComplete()
        val pool = soundPool
        if (pool != null && saveSoundId != 0) {
            pool.play(saveSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
        }
        return true
    }

    fun playClick(debounceMs: Long = 0L): Boolean {
        if (!isSoundEnabled) return false
        if (debounceMs > 0L) {
            val now = SystemClock.uptimeMillis()
            if (now - lastClickTimestampMs < debounceMs) {
                return false
            }
            lastClickTimestampMs = now
        }
        return playTap()
    }

    fun playSave(): Boolean = playComplete()

    // Simplified to Tap / Complete: play Tap sound while recording specific effect
    fun playEdit(): Boolean {
        if (!isSoundEnabled) return false
        synchronized(_playedEffects) { _playedEffects.add(SoundEffect.EDIT) }
        val pool = soundPool
        if (pool != null && clickSoundId != 0) {
            pool.play(clickSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
        }
        return true
    }

    fun playDelete(): Boolean {
        if (!isSoundEnabled) return false
        synchronized(_playedEffects) { _playedEffects.add(SoundEffect.DELETE) }
        val pool = soundPool
        if (pool != null && clickSoundId != 0) {
            pool.play(clickSoundId, 1.0f, 1.0f, 0, 0, 1.0f)
        }
        return true
    }

    @Synchronized
    fun release() {
        AppSoundManager.release()
        soundPool?.release()
        soundPool = null
        clickSoundId = 0
        saveSoundId = 0
        isInitialized = false
    }
}
