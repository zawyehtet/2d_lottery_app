package com.example.security

import android.content.Context

/**
 * Convenience utility for 2D Lottery device identification and offline license activation.
 */
object DeviceSecurityUtil2D {

    const val SECRET_SALT = SecurityManager.SHARED_SECRET_2D

    fun getDeviceId(context: Context): String = LicenseManager.getDeviceId(context)

    fun generateExpectedKey(deviceId: String): String = LicenseManager.generateExpectedKey(deviceId)

    fun verifyAndSaveKey(context: Context, key: String): Boolean = LicenseManager.verifyAndSaveLicense(context, key)

    fun isAppActivated(context: Context): Boolean = LicenseManager.isAppActivated(context)

    fun clearActivation(context: Context) = LicenseManager.clearLicenseForTesting(context)
}
