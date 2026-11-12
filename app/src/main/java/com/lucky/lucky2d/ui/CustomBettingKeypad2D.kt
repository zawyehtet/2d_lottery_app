package com.lucky.lucky2d.ui

import android.content.Context
import com.lucky.lucky2d.utils.CleanAudio2D

/**
 * 🍀 Lucky2D Clean Audio Keypad & UI Actions Binding
 * Wires every button tap to CleanAudio2D.playClick() and voucher commit to CleanAudio2D.playSuccess()
 */
object CustomBettingKeypad2D {
    // Keypad digits 0-9, 00, 000
    fun onDigitClick(digit: String, context: Context? = null) {
        CleanAudio2D.playClick(context)
    }

    // Quick patterns (ပါဝါ, နက္ခတ်, ညီအစ်ကို, အပူး, ထိပ်, နောက်, အခွေ, အပါ, ဘရိတ်)
    fun onPatternClick(patternName: String, context: Context? = null) {
        CleanAudio2D.playClick(context)
    }

    // Reverse toggle 'R'
    fun onReverseClick(context: Context? = null) {
        CleanAudio2D.playClick(context)
    }

    // Delete / Backspace 'ဖျက်'
    fun onDeleteClick(context: Context? = null) {
        CleanAudio2D.playClick(context)
    }

    // OK / Enter button
    fun onOkClick(context: Context? = null) {
        CleanAudio2D.playClick(context)
    }

    // 'ထိုးရန်' / Add bet to draft
    fun onAddBetClick(context: Context? = null) {
        CleanAudio2D.playClick(context)
    }

    // '⚡ အမြန်ထိုး' / Fast Bet
    fun onFastBetClick(context: Context? = null) {
        CleanAudio2D.playClick(context)
    }

    // Confirm & Save Voucher ('အတည်ပြုသိမ်းမည်')
    fun onConfirmVoucherSuccess(context: Context) {
        CleanAudio2D.playSuccess(context)
    }
}
