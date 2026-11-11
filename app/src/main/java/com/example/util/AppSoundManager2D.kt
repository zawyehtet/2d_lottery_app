package com.example.util

import android.content.Context
import com.lucky.lucky2d.utils.AppSoundManager2D

object AppSoundManager2D {
    var isSoundEnabled: Boolean
        get() = com.lucky.lucky2d.utils.AppSoundManager2D.isSoundEnabled
        set(value) {
            com.lucky.lucky2d.utils.AppSoundManager2D.isSoundEnabled = value
        }

    fun init(context: Context) {
        com.lucky.lucky2d.utils.AppSoundManager2D.init(context)
    }

    fun playTouchClick() {
        com.lucky.lucky2d.utils.AppSoundManager2D.playTouchClick()
    }

    fun playComplete(context: Context) {
        com.lucky.lucky2d.utils.AppSoundManager2D.playComplete(context)
    }
}
