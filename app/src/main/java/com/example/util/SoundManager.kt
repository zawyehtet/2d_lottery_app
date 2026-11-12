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

    fun playClick(context: Context? = null) {
        AppSoundManager2D.playTouchClick(context)
    }

    fun playTap(context: Context? = null) {
        AppSoundManager2D.playTouchClick(context)
    }

    fun playEdit(context: Context? = null) {
        AppSoundManager2D.playTouchClick(context)
    }

    fun playDelete(context: Context? = null) {
        AppSoundManager2D.playTouchClick(context)
    }

    fun playSave(context: Context? = null) {
        AppSoundManager2D.playTouchClick(context)
    }

    fun playComplete(context: Context) {
        AppSoundManager2D.playComplete(context)
    }

    fun playSuccess(context: Context) {
        AppSoundManager2D.playComplete(context)
    }
}
