package com.lucky.lucky2d

import android.app.Activity
import android.os.Bundle
import com.lucky.lucky2d.utils.CleanAudio2D

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // CleanAudio2D initialization as the very first line
        CleanAudio2D.init(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
