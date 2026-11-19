package com.lucky.lucky2d

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.lucky.lucky2d.ui.Lucky2DApp
import com.lucky.lucky2d.ui.theme.Lucky2DTheme
import com.lucky.lucky2d.utils.CleanAudio2D

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        CleanAudio2D.init(applicationContext)
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        window.statusBarColor = Color.parseColor("#065240")
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.parseColor("#F5F7F6")
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = true
        }

        setContent {
            Lucky2DTheme {
                Lucky2DApp()
            }
        }
    }
}
