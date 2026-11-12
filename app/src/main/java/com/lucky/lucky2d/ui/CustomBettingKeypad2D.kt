package com.lucky.lucky2d.ui

import android.content.Context
import com.lucky.lucky2d.utils.AppSoundManager2D

/**
 * 🍀 Lucky2D Keypad and Screen Sound Binding Reference
 * Enforces strictly in-app audio assets via AppSoundManager2D (Zero system sounds).
 */
object CustomBettingKeypad2D {
    // Keypad digits 0-9
    fun onDigitClick(digit: String) {
        AppSoundManager2D.playTouchClick()
    }

    // Quick bet patterns (ပါဝါ, နက္ခတ်, ညီအစ်ကို, အပူး, ထိပ်, နောက်, အခွေ, etc.)
    fun onPatternClick(patternName: String) {
        AppSoundManager2D.playTouchClick()
    }

    // Reverse toggle 'R'
    fun onReverseClick() {
        AppSoundManager2D.playTouchClick()
    }

    // Delete / Backspace 'ဖျက်'
    fun onDeleteClick() {
        AppSoundManager2D.playTouchClick()
    }

    // OK / Enter button
    fun onOkClick() {
        AppSoundManager2D.playTouchClick()
    }

    // 'ထိုးရန်' / Add bet to draft
    fun onAddBetClick() {
        AppSoundManager2D.playTouchClick()
    }

    // '⚡ အမြန်ထိုး' / Fast Bet
    fun onFastBetClick() {
        AppSoundManager2D.playTouchClick()
    }

    // Confirm & Save Voucher ('အတည်ပြုသိမ်းမည်')
    fun onConfirmVoucherSuccess(context: Context) {
        AppSoundManager2D.playComplete(context)
    }
}
