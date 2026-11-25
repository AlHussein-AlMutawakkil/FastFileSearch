package com.example.fastfilesearch.util

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaDrm
import android.os.Build
import android.provider.Settings
import com.example.fastfilesearch.licensing.V2LicenseCodec
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

    data class DeviceFingerprint(
        val fpType: Int,
        val prefix: String,
        val digestPrefix6: ByteArray,
        val clientDeviceId: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DeviceFingerprint) return false
            return fpType == other.fpType &&
                prefix == other.prefix &&
                digestPrefix6.contentEquals(other.digestPrefix6) &&
                clientDeviceId == other.clientDeviceId
        }

        override fun hashCode(): Int {
            var result = fpType
            result = 31 * result + prefix.hashCode()
            result = 31 * result + digestPrefix6.contentHashCode()
            result = 31 * result + clientDeviceId.hashCode()
            return result
        }
    }

    private val WIDEVINE_UUID: UUID = UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed")
    private const val HEX_DIGITS = "0123456789abcdef"

    @Volatile
    private var cachedFingerprint: DeviceFingerprint? = null

    fun getClientDeviceId(context: Context): String {
        return getFingerprintInfo(context).clientDeviceId
    }

    fun getFingerprintInfo(context: Context): DeviceFingerprint {
        cachedFingerprint?.let { return it }

        val computed = computeWidevineFingerprint() ?: computeAndroidIdFingerprint(context)
        cachedFingerprint = computed
        return computed
    }

    private fun computeWidevineFingerprint(): DeviceFingerprint? {
        var mediaDrm: MediaDrm? = null
        return try {
            mediaDrm = MediaDrm(WIDEVINE_UUID)
            val deviceUniqueId: ByteArray? = mediaDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
            if (deviceUniqueId != null && deviceUniqueId.isNotEmpty()) {
                val digest16 = sha256First16Bytes(deviceUniqueId)
                DeviceFingerprint(
                    fpType = V2LicenseCodec.FP_TYPE_WV,
                    prefix = "WV",
                    digestPrefix6 = digest16.copyOfRange(0, 6),
                    clientDeviceId = "WV-${bytesToHexLower(digest16)}"
                )
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
    private fun computeAndroidIdFingerprint(context: Context): DeviceFingerprint {
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
        val digest16 = sha256First16Bytes(source.toByteArray(StandardCharsets.UTF_8))
        return DeviceFingerprint(
            fpType = V2LicenseCodec.FP_TYPE_AD,
            prefix = "AD",
            digestPrefix6 = digest16.copyOfRange(0, 6),
            clientDeviceId = "AD-${bytesToHexLower(digest16)}"
        )
    }

    internal fun formatWidevineId(rawBytes: ByteArray): String {
        return "WV-${sha256First16BytesHex(rawBytes)}"
    }

    internal fun formatAndroidId(rawAndroidId: String): String {
        return "AD-${sha256First16BytesHex(rawAndroidId.toByteArray(StandardCharsets.UTF_8))}"
    }

    internal fun sha256First16Bytes(input: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256").digest(input)
        return digest.copyOfRange(0, 16)
    }

    internal fun bytesToHexLower(bytes: ByteArray): String {
        val chars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            chars[i * 2] = HEX_DIGITS[v ushr 4]
            chars[i * 2 + 1] = HEX_DIGITS[v and 0x0F]
        }
        return String(chars)
    }

    internal fun sha256First16BytesHex(input: ByteArray): String {
        return bytesToHexLower(sha256First16Bytes(input))
    }
}
