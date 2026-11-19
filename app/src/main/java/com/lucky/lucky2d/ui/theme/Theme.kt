package com.lucky.lucky2d.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LuckyGreen = Color(0xFF065240)
val LuckyGreenDark = Color(0xFF043E30)
val LuckyBg = Color(0xFFF5F7F6)
val LuckyCardSoft = Color(0xFFEDF3F0)
val LuckyRowAlt = Color(0xFFF1F6F4)
val LuckyBorder = Color(0xFFDCE5E1)
val LuckyInputBorder = Color(0xFFCBD6D1)
val LuckySuccess = Color(0xFF0A8819)
val LuckyDanger = Color(0xFFF42222)
val LuckyOrange = Color(0xFFFA7400)
val LuckyGold = Color(0xFFFEC72E)
val LuckyKeyBg = Color(0xFFE7EDEA)
val LuckyKeyBorder = Color(0xFFD4DDD8)
val LuckyViber = Color(0xFF7360F2)
val LuckyMessenger = Color(0xFF0084FF)
val LuckyTextPrimary = Color(0xFF111827)
val LuckyTextSecondary = Color(0xFF475569)
val LuckyTextMuted = Color(0xFF64748B)

private val LuckyColorScheme = lightColorScheme(
    primary = LuckyGreen,
    onPrimary = Color.White,
    primaryContainer = LuckyCardSoft,
    onPrimaryContainer = LuckyGreen,
    secondary = LuckySuccess,
    onSecondary = Color.White,
    tertiary = LuckyOrange,
    onTertiary = Color.White,
    background = LuckyBg,
    onBackground = LuckyTextPrimary,
    surface = Color.White,
    onSurface = LuckyTextPrimary,
    surfaceVariant = LuckyCardSoft,
    onSurfaceVariant = LuckyTextSecondary,
    error = LuckyDanger,
    onError = Color.White,
    outline = LuckyInputBorder
)

@Composable
fun Lucky2DTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LuckyColorScheme,
        content = content
    )
}
