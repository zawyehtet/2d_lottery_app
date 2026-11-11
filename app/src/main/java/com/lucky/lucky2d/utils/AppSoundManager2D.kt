package com.lucky.lucky2d.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlin.math.exp
import kotlin.math.sin

object AppSoundManager2D {
    var isSoundEnabled: Boolean = true

    private var soundPool: SoundPool? = null
    private var touchSoundId: Int = 0
    private var successSoundId: Int = 0
    private var isLoaded: Boolean = false

    fun init(context: Context) {
        if (soundPool != null) return
        try {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(attributes)
                .build().apply {
                    setOnLoadCompleteListener { _, sampleId, status ->
                        if (status == 0) isLoaded = true
                    }
                }

            soundPool?.let { pool ->
                val touchRes = context.resources.getIdentifier("touch_click", "raw", context.packageName)
                if (touchRes != 0) touchSoundId = pool.load(context, touchRes, 1)

                val successRes = context.resources.getIdentifier("success_chime", "raw", context.packageName)
                if (successRes != 0) successSoundId = pool.load(context, successRes, 1)
            }
        } catch (e: Exception) {
            Log.e("SoundManager2D", "Init failed", e)
        }
    }

    /**
     * Mobile UI Tap ("တစ်" အသံတိုလေး - Strictly NO "ကွမ်" sound)
     */
    fun playTouchClick() {
        if (!isSoundEnabled) return
        if (soundPool != null && touchSoundId != 0 && isLoaded) {
            soundPool?.play(touchSoundId, 0.75f, 0.75f, 1, 0, 1.0f)
            return
        }
        playCrispPop()
    }

    /**
     * Voucher Saved Chime ("တီး-လင်" ဘောင်ချာအောင်မြင်သံ)
     */
    fun playComplete(context: Context) {
        if (!isSoundEnabled) return
        if (soundPool != null && successSoundId != 0 && isLoaded) {
            soundPool?.play(successSoundId, 0.9f, 0.9f, 1, 0, 1.0f)
        } else {
            playDualChime()
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

    private fun playCrispPop() {
        Thread {
            try {
                val sampleRate = 44100
                val durationMs = 15
                val numSamples = (durationMs * sampleRate) / 1000
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val env = exp(-12.0 * progress)
                    val s = sin(2.0 * Math.PI * 2500.0 * (i.toDouble() / sampleRate)) * env * 0.3
                    buffer[i] = (s * Short.MAX_VALUE).toInt().toShort()
                }
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
                Thread.sleep(durationMs.toLong() + 5)
                track.release()
            } catch (_: Exception) {}
        }.start()
    }

    private fun playDualChime() {
        Thread {
            try {
                val sampleRate = 44100
                val totalMs = 150
                val numSamples = (totalMs * sampleRate) / 1000
                val buffer = ShortArray(numSamples)
                val split = (0.05 * sampleRate).toInt()
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val sample = if (i < split) {
                        val env = exp(-6.0 * (i.toDouble() / split))
                        sin(2.0 * Math.PI * 1046.5 * t) * env * 0.35
                    } else {
                        val env = exp(-7.0 * ((i - split).toDouble() / (numSamples - split)))
                        sin(2.0 * Math.PI * 1318.5 * t) * env * 0.4
                    }
                    buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }
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
                Thread.sleep(totalMs.toLong() + 10)
                track.release()
            } catch (_: Exception) {}
        }.start()
    }
}
