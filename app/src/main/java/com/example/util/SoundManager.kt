package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.SoundEffectConstants
import android.view.View

enum class SoundEffect {
    CLICK,
    SAVE,
    EDIT,
    DELETE
}

/**
 * AppSoundManager: Native Mobile Phone System Tap Sound and Complete Sound
 * - Tap Sound (ဖုန်းမူလ ကလစ်သံ):
 *   Native View.playSoundEffect(SoundEffectConstants.CLICK) or AudioManager.playSoundEffect(AudioManager.FX_KEY_CLICK)
 * - Complete Sound (ပြီးမြောက်သည့် အသံ + အသံတုန်ခါမှု):
 *   Native notification/success chime (RingtoneManager) + subtle 60ms haptic feedback
 */
object AppSoundManager {
    @Volatile
    var isSoundEnabled: Boolean = true

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // 1. Mobile Phone Tap Sound (ဖုန်းမူလ ကလစ်သံ)
    fun playTap(view: View? = null, context: Context? = null, record: Boolean = true) {
        if (!isSoundEnabled) return
        if (record) {
            SoundManager.recordTap()
        }

        try {
            // Preferred: View-based touch sound
            if (view != null) {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                return
            }

            // Fallback: AudioManager system key click
            val targetContext = context ?: appContext
            targetContext?.let { ctx ->
                val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.8f)
            }
        } catch (_: Exception) {
        }
    }

    // 2. Mobile Phone Complete Sound (ပြီးမြောက်သည့် အသံ + အသံတုန်ခါမှု)
    fun playComplete(context: Context? = null) {
        if (!isSoundEnabled) return
        SoundManager.recordComplete()

        val targetContext = context ?: appContext ?: return
        try {
            // Play system notification chime
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(targetContext, notificationUri)
            ringtone?.play()

            // Subtle Haptic Feedback (ဖုန်းတုန်ခါမှု အနည်းငယ် ထည့်သွင်းခြင်း)
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = targetContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                targetContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(60)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
