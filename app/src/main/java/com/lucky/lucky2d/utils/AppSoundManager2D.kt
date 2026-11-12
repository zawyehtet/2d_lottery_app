package com.lucky.lucky2d.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.lucky.lucky2d.R

object AppSoundManager2D {
    var isSoundEnabled: Boolean = true

    private var soundPool: SoundPool? = null
    private var touchSoundId: Int = 0
    private var successSoundId: Int = 0
    private var isTouchLoaded: Boolean = false
    private var isSuccessLoaded: Boolean = false

    fun init(context: Context) {
        if (soundPool != null) return
        try {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(attributes)
                .build().apply {
                    setOnLoadCompleteListener { _, sampleId, status ->
                        if (status == 0) {
                            if (sampleId == touchSoundId) isTouchLoaded = true
                            if (sampleId == successSoundId) isSuccessLoaded = true
                        }
                    }
                }

            // Hard-bind directly to res/raw/touch_click and res/raw/success_chime
            touchSoundId = soundPool?.load(context.applicationContext, R.raw.touch_click, 1) ?: 0
            successSoundId = soundPool?.load(context.applicationContext, R.raw.success_chime, 1) ?: 0
        } catch (e: Exception) {
            Log.e("AppSoundManager2D", "SoundPool direct load failed", e)
        }
    }

    fun playTouchClick() {
        if (!isSoundEnabled) return
        if (soundPool != null && touchSoundId != 0 && isTouchLoaded) {
            soundPool?.play(touchSoundId, 0.9f, 0.9f, 1, 0, 1.0f)
        }
    }

    fun playComplete(context: Context) {
        if (!isSoundEnabled) return
        if (soundPool != null && successSoundId != 0 && isSuccessLoaded) {
            soundPool?.play(successSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
        triggerSubtleHaptic(context)
    }

    private fun triggerSubtleHaptic(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(25)
            }
        } catch (_: Exception) {}
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        isTouchLoaded = false
        isSuccessLoaded = false
    }
}
