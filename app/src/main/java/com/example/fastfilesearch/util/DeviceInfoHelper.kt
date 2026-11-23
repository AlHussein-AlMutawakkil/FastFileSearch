package com.example.fastfilesearch.util

import android.content.Context
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
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
            val vName = pInfo.versionName ?: "1.0"
            val vCode = PackageInfoCompat.getLongVersionCode(pInfo)
            "$vName ($vCode)"
        } catch (_: Exception) {
            "1.0 (1)"
        }
    }

    fun getCpuAbi(): String {
        return Build.SUPPORTED_ABIS.firstOrNull() ?: "غير محدد"
    }

    fun buildActivationRequestMessage(context: Context, installationId: String): String {
        val device = getDeviceDisplayName()
        val os = getOsVersion()
        val app = getAppVersion(context)
        return """
            السلام عليكم ورحمة الله،
            طلب تفعيل تطبيق «بحث آمن»:
            • معرّف التثبيت: $installationId
            • نوع وموديل الهاتف: $device
            • إصدار النظام: $os
            • إصدار التطبيق: $app
        """.trimIndent()
    }
}
