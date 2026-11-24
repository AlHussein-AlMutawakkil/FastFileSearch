package com.example.fastfilesearch.licenseadmin.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings

/**
 * Provides distributor device identity metadata for silent audit logging.
 */
object IssuerInfoHelper {

    @SuppressLint("HardwareIds")
    fun getIssuerId(context: Context): String {
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (_: Throwable) {
            null
        }
        return if (!androidId.isNullOrBlank()) {
            androidId.trim()
        } else {
            "unknown"
        }
    }

    fun getIssuerDevice(): String {
        return "${Build.MANUFACTURER ?: ""} ${Build.MODEL ?: ""}".trim().ifBlank { "Unknown" }
    }
}
