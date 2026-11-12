package com.example.util

import android.content.Context
import com.lucky.lucky2d.utils.AppSoundManager2D as TargetManager

object AppSoundManager2D {
    var isSoundEnabled: Boolean
        get() = TargetManager.isSoundEnabled
        set(value) {
            TargetManager.isSoundEnabled = value
        }

    fun init(context: Context) {
        TargetManager.init(context)
    }

    fun playTouchClick(context: Context? = null) {
        TargetManager.playTouchClick(context)
    }

    fun playComplete(context: Context) {
        TargetManager.playComplete(context)
    }

    fun release() {
        TargetManager.release()
    }
}
