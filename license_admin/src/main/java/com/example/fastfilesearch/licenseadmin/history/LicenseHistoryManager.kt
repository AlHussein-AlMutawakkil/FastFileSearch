package com.example.fastfilesearch.licenseadmin.history

import android.content.Context
import java.util.UUID

data class IssuedLicenseItem(
    val internalId: String = UUID.randomUUID().toString(),
    val installationId: String,
    val clientName: String?,
    val iat: Long,
    val exp: Long,
    val maxWorkspaces: Int,
    val excelExport: Boolean,
    val appLock: Boolean,
    val bgSync: Boolean,
    val licenseCode: String,
    val manufacturer: String = "",
    val model: String = "",
    val osVersion: String = "",
    val abi: String = "",
    val appVersion: String = "",
    val clientDeviceId: String = ""
)

data class LicenseRenewalDraft(
    val requestId: Long = System.nanoTime(),
    val item: IssuedLicenseItem
)

object LicenseHistoryManager {

    private const val PREFS_NAME = "admin_license_history"
    private const val KEY_HISTORY = "history_entries"

    /**
     * Retrieves all issued licenses, sorted by creation date descending.
     */
    fun getHistory(context: Context): List<IssuedLicenseItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return deserializeList(raw).sortedByDescending { it.iat }
    }

    /**
     * Adds an issued license to history.
     */
    fun addLicense(context: Context, item: IssuedLicenseItem) {
        val current = getHistory(context).toMutableList()
        current.removeAll { it.internalId == item.internalId }
        current.add(0, item)
        saveHistory(context, current)
    }

    /**
     * Deletes a license by internal ID.
     */
    fun deleteLicense(context: Context, internalId: String) {
        val current = getHistory(context).filterNot { it.internalId == internalId }
        saveHistory(context, current)
    }

    /**
     * Clears all history.
     */
    fun clearHistory(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_HISTORY)
            .apply()
    }

    private fun saveHistory(context: Context, list: List<IssuedLicenseItem>) {
        val raw = serializeList(list)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HISTORY, raw)
            .apply()
    }

    private fun serializeList(list: List<IssuedLicenseItem>): String {
        val sb = StringBuilder("[")
        list.forEachIndexed { index, item ->
            sb.append("{")
            sb.append("\"id\":\"").append(escape(item.internalId)).append("\",")
            sb.append("\"instId\":\"").append(escape(item.installationId)).append("\",")
            sb.append("\"client\":\"").append(escape(item.clientName ?: "")).append("\",")
            sb.append("\"iat\":").append(item.iat).append(",")
            sb.append("\"exp\":").append(item.exp).append(",")
            sb.append("\"mw\":").append(item.maxWorkspaces).append(",")
            sb.append("\"ex\":").append(if (item.excelExport) 1 else 0).append(",")
            sb.append("\"lk\":").append(if (item.appLock) 1 else 0).append(",")
            sb.append("\"bg\":").append(if (item.bgSync) 1 else 0).append(",")
            sb.append("\"code\":\"").append(escape(item.licenseCode)).append("\"")
            if (item.manufacturer.isNotBlank()) sb.append(",\"m\":\"").append(escape(item.manufacturer)).append("\"")
            if (item.model.isNotBlank()) sb.append(",\"d\":\"").append(escape(item.model)).append("\"")
            if (item.osVersion.isNotBlank()) sb.append(",\"os\":\"").append(escape(item.osVersion)).append("\"")
            if (item.abi.isNotBlank()) sb.append(",\"abi\":\"").append(escape(item.abi)).append("\"")
            if (item.appVersion.isNotBlank()) sb.append(",\"v\":\"").append(escape(item.appVersion)).append("\"")
            if (item.clientDeviceId.isNotBlank()) sb.append(",\"cdid\":\"").append(escape(item.clientDeviceId)).append("\"")
            sb.append("}")
            if (index < list.size - 1) sb.append(",")
        }
        sb.append("]")
        return sb.toString()
    }

    private fun deserializeList(json: String): List<IssuedLicenseItem> {
        val result = mutableListOf<IssuedLicenseItem>()
        val itemPattern = "\\{([^}]+)\\}".toRegex()
        val matches = itemPattern.findAll(json)

        for (m in matches) {
            val body = m.groupValues[1]

            fun getStr(key: String): String {
                val p = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
                return p.find(body)?.groupValues?.get(1)?.let { unescape(it) } ?: ""
            }

            fun getLong(key: String): Long {
                val p = "\"$key\"\\s*:\\s*(-?\\d+)".toRegex()
                return p.find(body)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            }

            fun getInt(key: String): Int {
                val p = "\"$key\"\\s*:\\s*(-?\\d+)".toRegex()
                return p.find(body)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            }

            val internalId = getStr("id").ifEmpty { UUID.randomUUID().toString() }
            val installationId = getStr("instId")
            val clientName = getStr("client").takeIf { it.isNotBlank() }
            val iat = getLong("iat")
            val exp = getLong("exp")
            val mw = getInt("mw")
            val ex = getInt("ex") == 1
            val lk = getInt("lk") == 1
            val bg = getInt("bg") == 1
            val code = getStr("code")
            val manufacturer = getStr("m")
            val model = getStr("d")
            val osVersion = getStr("os")
            val abi = getStr("abi")
            val appVersion = getStr("v")
            val clientDeviceId = getStr("cdid")

            if (code.isNotBlank()) {
                result.add(
                    IssuedLicenseItem(
                        internalId = internalId,
                        installationId = installationId,
                        clientName = clientName,
                        iat = iat,
                        exp = exp,
                        maxWorkspaces = mw,
                        excelExport = ex,
                        appLock = lk,
                        bgSync = bg,
                        licenseCode = code,
                        manufacturer = manufacturer,
                        model = model,
                        osVersion = osVersion,
                        abi = abi,
                        appVersion = appVersion,
                        clientDeviceId = clientDeviceId
                    )
                )
            }
        }
        return result
    }

    private fun escape(s: String): String {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
    }

    private fun unescape(s: String): String {
        return s.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\")
    }
}
