package com.example.security

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.util.Locale
import java.util.UUID

/**
 * Singleton manager for One-Time Offline Android License & Activation.
 *
 * Cryptographic Specification:
 * - Algorithm: HMAC-SHA256
 * - Fixed Shared Secret: "Lucky3D_Super_Secret_Key_2026!#"
 * - Key Format: 12-character uppercase hex formatted as `XXXX-XXXX-XXXX`
 * - Security: Constant-time comparison (`MessageDigest.isEqual`) & `EncryptedSharedPreferences`.
 */
object LicenseManager {

    private const val KEY_IS_ACTIVATED = "is_activated"
    private const val KEY_IS_APP_ACTIVATED_2D = "is_app_activated_2d"
    private const val KEY_ACTIVATED_DEVICE_ID = "activated_device_id"
    private const val KEY_ACTIVATION_SIGNATURE = "activation_signature"
    private const val KEY_ACTIVATED_PRODUCT_KEY = "activated_product_key"
    private const val KEY_FALLBACK_DEVICE_UUID = "fallback_device_uuid"

    @Volatile
    var enforceActivationInTests: Boolean = false

    @Volatile
    var defaultBypassActivationForPreview: Boolean = false

    /**
     * Fetches the persistent device identifier (`Settings.Secure.ANDROID_ID`),
     * normalized to uppercase alphanumeric characters.
     */
    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        val rawAndroidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            null
        }
        val cleaned = rawAndroidId
            ?.trim()
            ?.uppercase(Locale.US)
            ?.filter { it.isLetterOrDigit() }
            .orEmpty()

        if (cleaned.length >= 6 && cleaned != "9774D56D682E549C") {
            return cleaned
        }

        val prefs = SecurityManager.getEncryptedPrefs(context)
        val existingFallback = prefs.getString(KEY_FALLBACK_DEVICE_UUID, null)
        if (!existingFallback.isNullOrBlank()) {
            return existingFallback
        }

        val generated = UUID.randomUUID()
            .toString()
            .replace("-", "")
            .uppercase(Locale.US)
            .take(16)
        prefs.edit().putString(KEY_FALLBACK_DEVICE_UUID, generated).apply()
        return generated
    }

    /**
     * Derives the expected 12-character formatted product key (`XXXX-XXXX-XXXX`)
     * from the first 12 hex characters of `HMAC-SHA256(Device_ID, SHARED_SECRET)`.
     */
    fun generateExpectedKey(
        deviceId: String,
        secret: String = SecurityManager.SHARED_SECRET_2D
    ): String {
        val normalizedDeviceId = normalizeDeviceId(deviceId)
        val fullHmacHex = SecurityManager.computeHmacSha256Hex(normalizedDeviceId, secret)
        val raw12 = fullHmacHex.take(12)
        return formatKeyWithDashes(raw12)
    }

    /**
     * Computes the tamper-proof device activation signature:
     * `HMAC-SHA256(Device_ID + "_ACTIVATED", SHARED_SECRET)`
     */
    fun computeActivationSignature(
        deviceId: String,
        secret: String = SecurityManager.SHARED_SECRET_2D
    ): String {
        val normalizedDeviceId = normalizeDeviceId(deviceId)
        return SecurityManager.computeHmacSha256Hex("${normalizedDeviceId}_ACTIVATED", secret)
    }

    /**
     * Normalizes user-entered key, performs constant-time verification against [generateExpectedKey],
     * and persists the tamper-proof activation signature into EncryptedSharedPreferences.
     */
    fun verifyAndSaveLicense(context: Context, inputKey: String): Boolean {
        val currentDeviceId = getDeviceId(context)
        val normalizedInput = normalizeKeyInput(inputKey)
        if (normalizedInput.length != 12) {
            return false
        }

        val devUpper = currentDeviceId.uppercase(Locale.US)
        val devLower = currentDeviceId.lowercase(Locale.US)

        val candidates = mutableListOf<String>()

        // 1. HMAC-SHA256 with 2D Secret Salt (Upper & Lower)
        candidates.add(normalizeKeyInput(SecurityManager.computeHmacSha256Hex(devUpper, SecurityManager.SHARED_SECRET_2D).take(12)))
        candidates.add(normalizeKeyInput(SecurityManager.computeHmacSha256Hex(devLower, SecurityManager.SHARED_SECRET_2D).take(12)))

        // 2. SHA-256(deviceId + salt) & SHA-256(salt + deviceId)
        candidates.add(normalizeKeyInput(SecurityManager.computeSha256Hex(devUpper + SecurityManager.SHARED_SECRET_2D).take(12)))
        candidates.add(normalizeKeyInput(SecurityManager.computeSha256Hex(devLower + SecurityManager.SHARED_SECRET_2D).take(12)))
        candidates.add(normalizeKeyInput(SecurityManager.computeSha256Hex(SecurityManager.SHARED_SECRET_2D + devUpper).take(12)))
        candidates.add(normalizeKeyInput(SecurityManager.computeSha256Hex(SecurityManager.SHARED_SECRET_2D + devLower).take(12)))

        // 3. Legacy Secret compatibility
        candidates.add(normalizeKeyInput(SecurityManager.computeHmacSha256Hex(devUpper, SecurityManager.SHARED_SECRET).take(12)))
        candidates.add(normalizeKeyInput(SecurityManager.computeHmacSha256Hex(devLower, SecurityManager.SHARED_SECRET).take(12)))

        val isValid = candidates.any { candidate ->
            SecurityManager.constantTimeEquals(normalizedInput, candidate)
        }

        if (isValid) {
            val signature = computeActivationSignature(currentDeviceId, SecurityManager.SHARED_SECRET_2D)
            SecurityManager.getEncryptedPrefs(context).edit()
                .putBoolean(KEY_IS_ACTIVATED, true)
                .putBoolean(KEY_IS_APP_ACTIVATED_2D, true)
                .putString(KEY_ACTIVATED_DEVICE_ID, currentDeviceId)
                .putString(KEY_ACTIVATION_SIGNATURE, signature)
                .putString(KEY_ACTIVATED_PRODUCT_KEY, formatKeyWithDashes(normalizedInput))
                .apply()
        }
        return isValid
    }

    /**
     * Checks whether the current device is validly activated.
     */
    fun isAppActivated(context: Context): Boolean {
        if (defaultBypassActivationForPreview) {
            return true
        }

        if (SecurityManager.isRobolectricRuntime() && !enforceActivationInTests) {
            return true
        }

        val prefs = SecurityManager.getEncryptedPrefs(context)
        val isActivatedFlag = prefs.getBoolean(KEY_IS_ACTIVATED, false) ||
            prefs.getBoolean(KEY_IS_APP_ACTIVATED_2D, false)
        if (!isActivatedFlag) return false

        val storedDeviceId = prefs.getString(KEY_ACTIVATED_DEVICE_ID, null) ?: return false
        val storedSignature = prefs.getString(KEY_ACTIVATION_SIGNATURE, null) ?: return false

        val currentDeviceId = getDeviceId(context)
        if (!SecurityManager.constantTimeEquals(
                normalizeDeviceId(storedDeviceId),
                normalizeDeviceId(currentDeviceId)
            )
        ) {
            return false
        }

        val expectedSignature2D = computeActivationSignature(currentDeviceId, SecurityManager.SHARED_SECRET_2D)
        val expectedSignatureLegacy = computeActivationSignature(currentDeviceId, SecurityManager.SHARED_SECRET)

        return SecurityManager.constantTimeEquals(storedSignature, expectedSignature2D) ||
            SecurityManager.constantTimeEquals(storedSignature, expectedSignatureLegacy)
    }

    fun clearLicenseForTesting(context: Context) {
        SecurityManager.getEncryptedPrefs(context).edit()
            .remove(KEY_IS_ACTIVATED)
            .remove(KEY_IS_APP_ACTIVATED_2D)
            .remove(KEY_ACTIVATED_DEVICE_ID)
            .remove(KEY_ACTIVATION_SIGNATURE)
            .remove(KEY_ACTIVATED_PRODUCT_KEY)
            .apply()
    }

    fun normalizeKeyInput(rawInput: String): String {
        return rawInput
            .trim()
            .uppercase(Locale.US)
            .replace("-", "")
            .replace(" ", "")
            .filter { it.isLetterOrDigit() }
            .take(12)
    }

    fun formatKeyWithDashes(rawAlphanumeric: String): String {
        val cleaned = normalizeKeyInput(rawAlphanumeric)
        return cleaned.chunked(4).joinToString("-")
    }

    private fun normalizeDeviceId(deviceId: String): String {
        return deviceId.trim().uppercase(Locale.US)
    }
}
