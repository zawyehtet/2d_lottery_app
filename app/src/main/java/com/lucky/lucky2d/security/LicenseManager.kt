package com.lucky.lucky2d.security

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object LicenseManager {
    const val SHARED_SECRET_2D = "LUCKY2D_SHWEKHINMYAY_OFFLINE_SECRET_SALT_2026"
    const val SHARED_SECRET = "Lucky3D_Super_Secret_Key_2026!#"
    const val BYPASS_ACTIVATION_FOR_REVIEW = false

    private const val PREF_NAME = "lucky2d_license_prefs"
    private const val KEY_ACTIVATED = "lucky2d_is_activated"
    private const val KEY_ACTIVATED_KEY = "lucky2d_activated_product_key"
    private const val KEY_DEVICE_ID = "lucky2d_device_id"

    private val NON_ALNUM_REGEX = Regex("[^A-Z0-9]")

    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val cached = prefs.getString(KEY_DEVICE_ID, null)
        if (!cached.isNullOrBlank() && cached.length == 16) {
            return cached
        }

        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (_: Exception) {
            null
        }

        val deterministicId = if (!androidId.isNullOrBlank()) {
            computeSha256Hex(androidId.trim()).take(16)
        } else {
            val fallbackSeed = "${android.os.Build.BOARD}_${android.os.Build.BRAND}_${android.os.Build.MODEL}_${System.currentTimeMillis()}"
            computeSha256Hex(fallbackSeed).take(16)
        }

        prefs.edit().putString(KEY_DEVICE_ID, deterministicId).apply()
        return deterministicId
    }

    fun normalizeKeyInput(raw: String): String {
        return NON_ALNUM_REGEX.replace(raw.trim().uppercase(), "").take(12)
    }

    fun formatKeyWithDashes(raw: String): String {
        val norm = normalizeKeyInput(raw)
        if (norm.isEmpty()) return ""
        return norm.chunked(4).joinToString("-")
    }

    fun computeHmacSha256Hex(payload: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(keySpec)
        val bytes = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02X".format(it) }
    }

    fun computeSha256Hex(payload: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(payload.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02X".format(it) }
    }

    fun verifyAndSaveLicense(context: Context, inputKey: String): Boolean {
        val currentDeviceId = getDeviceId(context)
        val normalizedInput = normalizeKeyInput(inputKey)
        if (normalizedInput.length != 12) return false

        val devUpper = currentDeviceId.uppercase()
        val devLower = currentDeviceId.lowercase()

        val candidates = listOf(
            // 1. HMAC-SHA256 with 2D Secret Salt (Upper & Lower)
            normalizeKeyInput(computeHmacSha256Hex(devUpper, SHARED_SECRET_2D).take(12)),
            normalizeKeyInput(computeHmacSha256Hex(devLower, SHARED_SECRET_2D).take(12)),
            // 2. SHA-256(deviceId + salt) & SHA-256(salt + deviceId)
            normalizeKeyInput(computeSha256Hex(devUpper + SHARED_SECRET_2D).take(12)),
            normalizeKeyInput(computeSha256Hex(devLower + SHARED_SECRET_2D).take(12)),
            normalizeKeyInput(computeSha256Hex(SHARED_SECRET_2D + devUpper).take(12)),
            normalizeKeyInput(computeSha256Hex(SHARED_SECRET_2D + devLower).take(12)),
            // 3. Legacy Secret compatibility
            normalizeKeyInput(computeHmacSha256Hex(devUpper, SHARED_SECRET).take(12)),
            normalizeKeyInput(computeHmacSha256Hex(devLower, SHARED_SECRET).take(12))
        )

        val isValid = candidates.any { it == normalizedInput }
        if (isValid) {
            val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(KEY_ACTIVATED, true)
                .putString(KEY_ACTIVATED_KEY, formatKeyWithDashes(normalizedInput))
                .apply()
        }
        return isValid
    }

    fun isAppActivated(context: Context): Boolean {
        if (BYPASS_ACTIVATION_FOR_REVIEW) return true
        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ACTIVATED, false)
    }

    fun setAppActivated(context: Context, activated: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (activated) {
            prefs.edit().putBoolean(KEY_ACTIVATED, true).apply()
        } else {
            prefs.edit()
                .remove(KEY_ACTIVATED)
                .remove(KEY_ACTIVATED_KEY)
                .apply()
        }
    }
}
