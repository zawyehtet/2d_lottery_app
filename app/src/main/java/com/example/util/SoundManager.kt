package com.example.util

import android.content.Context
import com.lucky.lucky2d.utils.AppSoundManager2D

object SoundManager {
    fun init(context: Context) {
        AppSoundManager2D.init(context)
    }

    fun release() {
        AppSoundManager2D.release()
    }

    fun playClick() {
        AppSoundManager2D.playTouchClick()
    }

    fun playTap() {
        AppSoundManager2D.playTouchClick()
    }

    fun playEdit() {
        AppSoundManager2D.playTouchClick()
    }

    fun playDelete() {
        AppSoundManager2D.playTouchClick()
    }

    fun playSave() {
        AppSoundManager2D.playTouchClick()
    }

    fun playComplete(context: Context? = null) {
        if (context != null) {
            AppSoundManager2D.playComplete(context)
        } else {
            AppSoundManager2D.playTouchClick()
        }
    }

    fun playSuccess(context: Context? = null) {
        if (context != null) {
            AppSoundManager2D.playComplete(context)
        } else {
            AppSoundManager2D.playTouchClick()
        }
    }
}
