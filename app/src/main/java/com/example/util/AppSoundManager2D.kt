package com.example.util

import android.content.Context
import com.lucky.lucky2d.utils.AppSoundManager2D as LuckyAppSoundManager2D

object AppSoundManager2D {
    var isSoundEnabled: Boolean
        get() = LuckyAppSoundManager2D.isSoundEnabled
        set(value) {
            LuckyAppSoundManager2D.isSoundEnabled = value
        }

    fun init(context: Context) {
        LuckyAppSoundManager2D.init(context)
    }

    fun playTouchClick() {
        LuckyAppSoundManager2D.playTouchClick()
    }

    fun playComplete(context: Context) {
        LuckyAppSoundManager2D.playComplete(context)
    }

    fun release() {
        LuckyAppSoundManager2D.release()
    }
}
