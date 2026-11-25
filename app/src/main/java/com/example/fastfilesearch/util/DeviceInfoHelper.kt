package com.example.fastfilesearch.util

import android.content.Context
import android.os.Build
import com.example.fastfilesearch.licensing.InstallationIdManager
import com.example.fastfilesearch.licensing.V2LicenseCodec
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Locale

object DeviceInfoHelper {

    fun getManufacturer(): String {
        val manufacturer = Build.MANUFACTURER ?: "Unknown"
        return manufacturer.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
    }

    fun getModel(): String {
        val model = Build.MODEL ?: ""
        val manufacturer = Build.MANUFACTURER ?: ""
        return if (manufacturer.isNotBlank() && model.startsWith(manufacturer, ignoreCase = true)) {
            model.substring(manufacturer.length).trim()
        } else {
            model
        }
    }

    fun getDeviceDisplayName(): String {
        val m = getManufacturer()
        val model = getModel()
        return if (model.isBlank()) m else "$m $model".trim()
    }

    fun getOsVersion(): String {
        return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }

    fun getOsVersionShort(): String {
        return "Android ${Build.VERSION.RELEASE} - API ${Build.VERSION.SDK_INT}"
    }

    fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "5.0.0"
        } catch (_: Exception) {
            "5.0.0"
        }
    }

    fun getCpuAbi(): String {
        return Build.SUPPORTED_ABIS.firstOrNull() ?: "غير محدد"
    }

    /**
     * Builds the compact binary activation request encoded with Base62 prefixed with "R2" (~24-28 chars).
     */
    fun buildEncodedActivationRequest(context: Context, installationId: String = ""): String {
        return buildR2ActivationRequest(context)
    }

    fun getAppVersionCode(context: Context): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (_: Exception) {
            500
        }
    }

    fun buildR2ActivationRequest(context: Context): String {
        val fp = ClientDeviceIdProvider.getFingerprintInfo(context)
        val nonce = InstallationIdManager.getOrCreateInstallNonce(context)
        val appVer = getAppVersion(context)
        val compactVer = V2LicenseCodec.encodeAppVersionCompact(
            appVersion = appVer,
            versionCodeFallback = getAppVersionCode(context)
        )
        val abiCode = V2LicenseCodec.encodeAbiCode(getCpuAbi())
        return encodeR2ActivationRequest(
            platform = V2LicenseCodec.PLATFORM_ANDROID,
            fpType = fp.fpType,
            abiCode = abiCode,
            deviceHash6 = fp.digestPrefix6,
            installNonce = nonce,
            osVersionInt = Build.VERSION.SDK_INT,
            appVersionCompact = compactVer,
            manufacturer = getManufacturer(),
            model = getModel()
        )
    }

    fun encodeR2ActivationRequest(
        platform: Int = V2LicenseCodec.PLATFORM_ANDROID,
        fpType: Int,
        abiCode: Int,
        deviceHash6: ByteArray,
        installNonce: Int,
        osVersionInt: Int,
        appVersionCompact: Int,
        manufacturer: String,
        model: String
    ): String {
        return V2LicenseCodec.encodeR2Request(
            platform = platform,
            fpType = fpType,
            abiCode = abiCode,
            deviceHash6 = deviceHash6,
            installNonce = installNonce,
            osVersionInt = osVersionInt,
            appVersionCompact = appVersionCompact,
            manufacturer = manufacturer,
            model = model
        )
    }

    /**
     * Legacy BAR1 encoder preserved for backward-compatibility tests.
     */
    fun buildLegacyBar1ActivationRequest(context: Context, installationId: String): String {
        val m = getManufacturer()
        val d = getModel()
        val os = getOsVersion()
        val abi = getCpuAbi()
        val v = getAppVersion(context)
        val clientDeviceId = ClientDeviceIdProvider.getClientDeviceId(context)

        return encodeActivationRequest(
            installationId = installationId,
            manufacturer = m,
            model = d,
            osVersion = os,
            abi = abi,
            appVersion = v,
            clientDeviceId = clientDeviceId
        )
    }

    internal fun encodeActivationRequest(
        installationId: String,
        manufacturer: String,
        model: String,
        osVersion: String,
        abi: String,
        appVersion: String,
        clientDeviceId: String?
    ): String {
        fun escapeJson(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")

        val sb = StringBuilder("{")
        sb.append("\"id\":\"").append(escapeJson(installationId)).append("\",")
        sb.append("\"m\":\"").append(escapeJson(manufacturer)).append("\",")
        sb.append("\"d\":\"").append(escapeJson(model)).append("\",")
        sb.append("\"os\":\"").append(escapeJson(osVersion)).append("\",")
        sb.append("\"abi\":\"").append(escapeJson(abi)).append("\",")
        sb.append("\"v\":\"").append(escapeJson(appVersion)).append("\"")
        if (!clientDeviceId.isNullOrBlank()) {
            sb.append(",\"client_device_id\":\"").append(escapeJson(clientDeviceId)).append("\"")
        }
        sb.append("}")
        val encoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(sb.toString().toByteArray(StandardCharsets.UTF_8))
        return "BAR1.$encoded"
    }

    fun buildActivationRequestMessage(context: Context, installationId: String = ""): String {
        return buildEncodedActivationRequest(context, installationId)
    }
}
