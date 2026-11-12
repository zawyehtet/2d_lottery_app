package com.lucky.lucky2d

import android.app.Activity
import android.os.Bundle
import com.lucky.lucky2d.utils.AppSoundManager2D

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enforce SoundPool initialization before any UI rendering or touch events
        AppSoundManager2D.init(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        AppSoundManager2D.release()
    }
}
