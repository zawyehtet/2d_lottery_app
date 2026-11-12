package com.lucky.lucky2d.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.lucky.lucky2d.R

object CleanAudio2D {
    var isSoundEnabled: Boolean = true

    private var soundPool: SoundPool? = null
    private var clickId: Int = 0
    private var chimeId: Int = 0
    private var clickReady: Boolean = false
    private var chimeReady: Boolean = false

    fun init(context: Context) {
        if (soundPool != null) return
        val appContext = context.applicationContext
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attrs)
            .build().apply {
                setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0) {
                        if (sampleId == clickId) clickReady = true
                        if (sampleId == chimeId) chimeReady = true
                    }
                }
            }

        clickId = soundPool?.load(appContext, R.raw.touch_click, 1) ?: 0
        chimeId = soundPool?.load(appContext, R.raw.success_chime, 1) ?: 0
    }

    fun playClick(context: Context? = null) {
        if (!isSoundEnabled) return
        if (soundPool == null && context != null) init(context)
        if (clickId != 0 && clickReady) {
            soundPool?.play(clickId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }

    fun playSuccess(context: Context) {
        if (!isSoundEnabled) return
        if (soundPool == null) init(context)
        if (chimeId != 0 && chimeReady) {
            soundPool?.play(chimeId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
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
}
