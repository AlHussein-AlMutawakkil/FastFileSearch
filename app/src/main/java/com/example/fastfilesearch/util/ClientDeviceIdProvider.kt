package com.example.fastfilesearch.util

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaDrm
import android.os.Build
import android.provider.Settings
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

/**
 * Computes a persistent, completely offline hardware/device fingerprint (`client_device_id`).
 *
 * Strategy:
 * 1. Tries Widevine MediaDrm unique hardware ID -> "WV-<first 16 bytes SHA-256 hex>"
 * 2. Falls back to Settings.Secure.ANDROID_ID -> "AD-<first 16 bytes SHA-256 hex>"
 */
object ClientDeviceIdProvider {

    private val WIDEVINE_UUID: UUID = UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed")
    private const val HEX_DIGITS = "0123456789abcdef"

    @Volatile
    private var cachedClientDeviceId: String? = null

    fun getClientDeviceId(context: Context): String {
        cachedClientDeviceId?.let { return it }

        val computed = computeWidevineId() ?: computeAndroidIdFallback(context)
        cachedClientDeviceId = computed
        return computed
    }

    private fun computeWidevineId(): String? {
        var mediaDrm: MediaDrm? = null
        return try {
            mediaDrm = MediaDrm(WIDEVINE_UUID)
            val deviceUniqueId: ByteArray? = mediaDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
            if (deviceUniqueId != null && deviceUniqueId.isNotEmpty()) {
                formatWidevineId(deviceUniqueId)
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        } finally {
            if (mediaDrm != null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        mediaDrm.close()
                    } else {
                        @Suppress("DEPRECATION")
                        mediaDrm.release()
                    }
                } catch (_: Throwable) {
                }
            }
        }
    }

    @SuppressLint("HardwareIds")
    private fun computeAndroidIdFallback(context: Context): String {
        val rawAndroidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (_: Throwable) {
            null
        }
        val source = if (!rawAndroidId.isNullOrBlank()) {
            rawAndroidId.trim()
        } else {
            "${Build.BOARD}|${Build.BRAND}|${Build.DEVICE}|${Build.HARDWARE}"
        }
        return formatAndroidId(source)
    }

    internal fun formatWidevineId(rawBytes: ByteArray): String {
        return "WV-${sha256First16BytesHex(rawBytes)}"
    }

    internal fun formatAndroidId(rawAndroidId: String): String {
        return "AD-${sha256First16BytesHex(rawAndroidId.toByteArray(StandardCharsets.UTF_8))}"
    }

    internal fun sha256First16BytesHex(input: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input)
        val chars = CharArray(32)
        for (i in 0 until 16) {
            val v = digest[i].toInt() and 0xFF
            chars[i * 2] = HEX_DIGITS[v ushr 4]
            chars[i * 2 + 1] = HEX_DIGITS[v and 0x0F]
        }
        return String(chars)
    }
}
