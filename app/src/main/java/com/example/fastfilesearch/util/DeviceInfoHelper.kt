package com.example.fastfilesearch.util

import android.content.Context
import android.os.Build
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
     * Builds a compact JSON payload of device and installation details
     * and encodes it with Base64 URL_SAFE (without padding) prefixed with "BAR1."
     */
    fun buildEncodedActivationRequest(context: Context, installationId: String): String {
        val m = getManufacturer()
        val d = getModel()
        val os = getOsVersion()
        val abi = getCpuAbi()
        val v = getAppVersion(context)

        fun escapeJson(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")

        val json = """{"id":"${escapeJson(installationId)}","m":"${escapeJson(m)}","d":"${escapeJson(d)}","os":"${escapeJson(os)}","abi":"${escapeJson(abi)}","v":"${escapeJson(v)}"}"""
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray(StandardCharsets.UTF_8))
        return "BAR1.$encoded"
    }

    fun buildActivationRequestMessage(context: Context, installationId: String): String {
        val requestCode = buildEncodedActivationRequest(context, installationId)
        return "طلب تفعيل تطبيق «بحث آمن»:\n$requestCode"
    }
}
