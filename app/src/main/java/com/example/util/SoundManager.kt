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
 * AppSoundManager2D: Native System Touch Click Sound ("တစ်/တချက်" click) exclusively.
 * Zero overlapping synthetic AudioTracks, zero audio latency.
 */
object AppSoundManager2D {
    var isSoundEnabled: Boolean
        get() = AppSoundManager.isSoundEnabled
        set(value) {
            AppSoundManager.isSoundEnabled = value
        }

    /**
     * Native System Touch Click Sound ("တစ်/တချက်" click)
     * Directly triggers Android's native sound effect with zero latency and zero overlapping layers.
     */
    fun playTouchClick(view: View? = null, context: Context? = null) {
        if (!isSoundEnabled) return

        if (view != null) {
            view.playSoundEffect(SoundEffectConstants.CLICK)
            return
        }

        val targetContext = context ?: AppSoundManager.appContext
        targetContext?.let { ctx ->
            val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 1.0f)
        }
    }

    /**
     * Complete sound for bet submission and settlement save actions.
     */
    fun playComplete(context: Context? = null) {
        if (!isSoundEnabled) return
        val targetContext = context ?: AppSoundManager.appContext ?: return
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(targetContext, notificationUri)
            ringtone?.play()

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = targetContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                targetContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * AppSoundManager: Primary interface for sound effects in the 2D app.
 * Retains backward compatibility while delegating directly to native AppSoundManager2D.
 */
object AppSoundManager {
    @Volatile
    var isSoundEnabled: Boolean = true

    internal var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // 1. Play Native System Touch Click ("တစ်/တချက်" click)
    fun playTap(view: View? = null, context: Context? = null, record: Boolean = true) {
        if (!isSoundEnabled) return
        if (record) {
            SoundManager.recordTap()
        }
        AppSoundManager2D.playTouchClick(view = view, context = context ?: appContext)
    }

    // 2. Play Complete Sound + Subtle Haptic
    fun playComplete(context: Context? = null) {
        if (!isSoundEnabled) return
        SoundManager.recordComplete()
        AppSoundManager2D.playComplete(context ?: appContext)
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
