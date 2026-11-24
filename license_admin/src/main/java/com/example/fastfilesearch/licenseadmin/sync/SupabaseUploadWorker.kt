package com.example.fastfilesearch.licenseadmin.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fastfilesearch.licenseadmin.BuildConfig
import com.example.fastfilesearch.licenseadmin.data.LicenseAdminDatabase
import com.example.fastfilesearch.licenseadmin.data.PendingUploadEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

sealed class UploadAttemptOutcome {
    data object Success : UploadAttemptOutcome()
    data class PermanentClientError(val statusCode: Int) : UploadAttemptOutcome()
    data class TransientFailure(val reason: String) : UploadAttemptOutcome()
}

class SupabaseUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val rawUrl = BuildConfig.SUPABASE_URL.trim()
        val apiKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()
        if (rawUrl.isBlank() || apiKey.isBlank()) {
            return Result.success()
        }

        val endpoint = buildLicenseLogsEndpoint(rawUrl)
        val dao = LicenseAdminDatabase.getInstance(applicationContext).pendingUploadDao()
        val pendingItems = dao.getAllPending()
        if (pendingItems.isEmpty()) {
            return Result.success()
        }

        var shouldRetry = false

        for (item in pendingItems) {
            val payloadJson = toSupabaseJson(item)
            val outcome = withContext(Dispatchers.IO) {
                postJsonRecord(
                    endpoint = endpoint,
                    apiKey = apiKey,
                    payloadJson = payloadJson
                )
            }

            when (outcome) {
                is UploadAttemptOutcome.Success -> {
                    dao.deleteById(item.localId)
                }
                is UploadAttemptOutcome.PermanentClientError -> {
                    val nextAttempts = item.attempts + 1
                    if (nextAttempts >= MAX_4XX_ATTEMPTS) {
                        // Cap permanent 4xx retries at 5 attempts to prevent endless battery/data drain
                        dao.deleteById(item.localId)
                    } else {
                        dao.incrementAttempts(item.localId)
                        shouldRetry = true
                    }
                }
                is UploadAttemptOutcome.TransientFailure -> {
                    dao.incrementAttempts(item.localId)
                    shouldRetry = true
                }
            }
        }

        return if (shouldRetry) Result.retry() else Result.success()
    }

    companion object {
        const val MAX_4XX_ATTEMPTS = 5
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 15_000

        internal fun buildLicenseLogsEndpoint(rawBaseUrl: String): String {
            val cleanBase = rawBaseUrl
                .trim()
                .trimEnd('/')
                .removeSuffix("/rest/v1")
                .trimEnd('/')
            return "$cleanBase/rest/v1/license_logs"
        }

        internal fun buildRequestHeaders(apiKey: String): Map<String, String> {
            val headers = linkedMapOf(
                "apikey" to apiKey,
                "Content-Type" to "application/json; charset=utf-8",
                "Prefer" to "return=minimal"
            )
            if (apiKey.startsWith("eyJ")) {
                headers["Authorization"] = "Bearer $apiKey"
            }
            return headers
        }

        internal fun classifyHttpStatus(statusCode: Int): UploadAttemptOutcome {
            return when (statusCode) {
                in 200..299 -> UploadAttemptOutcome.Success
                in 400..499 -> UploadAttemptOutcome.PermanentClientError(statusCode)
                else -> UploadAttemptOutcome.TransientFailure("HTTP $statusCode")
            }
        }

        internal fun toSupabaseJson(item: PendingUploadEntity): String {
            fun jsonStringOrNull(value: String?): String {
                if (value == null) return "null"
                val sb = StringBuilder(value.length + 2)
                sb.append('"')
                for (ch in value) {
                    when (ch) {
                        '\\' -> sb.append("\\\\")
                        '"' -> sb.append("\\\"")
                        '\n' -> sb.append("\\n")
                        '\r' -> sb.append("\\r")
                        '\t' -> sb.append("\\t")
                        '\b' -> sb.append("\\b")
                        '\u000C' -> sb.append("\\f")
                        else -> {
                            if (ch.code < 0x20) {
                                sb.append(String.format("\\u%04x", ch.code))
                            } else {
                                sb.append(ch)
                            }
                        }
                    }
                }
                sb.append('"')
                return sb.toString()
            }

            return buildString {
                append('{')
                append("\"issued_at\":").append(jsonStringOrNull(item.issuedAt)).append(',')
                append("\"issuer_id\":").append(jsonStringOrNull(item.issuerId)).append(',')
                append("\"issuer_device\":").append(jsonStringOrNull(item.issuerDevice)).append(',')
                append("\"customer_name\":").append(jsonStringOrNull(item.customerName)).append(',')
                append("\"installation_id\":").append(jsonStringOrNull(item.installationId)).append(',')
                append("\"client_device_id\":").append(jsonStringOrNull(item.clientDeviceId)).append(',')
                append("\"client_manufacturer\":").append(jsonStringOrNull(item.clientManufacturer)).append(',')
                append("\"client_model\":").append(jsonStringOrNull(item.clientModel)).append(',')
                append("\"client_os\":").append(jsonStringOrNull(item.clientOs)).append(',')
                append("\"client_abi\":").append(jsonStringOrNull(item.clientAbi)).append(',')
                append("\"client_app_version\":").append(jsonStringOrNull(item.clientAppVersion)).append(',')
                append("\"plan_type\":").append(jsonStringOrNull(item.planType)).append(',')
                append("\"duration_days\":").append(item.durationDays).append(',')
                append("\"expires_at\":").append(jsonStringOrNull(item.expiresAt)).append(',')
                append("\"max_folders\":").append(item.maxFolders).append(',')
                append("\"allow_excel\":").append(item.allowExcel).append(',')
                append("\"allow_app_lock\":").append(item.allowAppLock).append(',')
                append("\"license_code\":").append(jsonStringOrNull(item.licenseCode))
                append('}')
            }
        }

        internal fun postJsonRecord(
            endpoint: String,
            apiKey: String,
            payloadJson: String
        ): UploadAttemptOutcome {
            var connection: HttpURLConnection? = null
            return try {
                val url = URL(endpoint)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    requestMethod = "POST"
                    doOutput = true
                    useCaches = false
                    for ((key, value) in buildRequestHeaders(apiKey)) {
                        setRequestProperty(key, value)
                    }
                }

                OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                    writer.write(payloadJson)
                    writer.flush()
                }

                val code = connection.responseCode
                classifyHttpStatus(code)
            } catch (t: Throwable) {
                UploadAttemptOutcome.TransientFailure(t.message ?: "Network error")
            } finally {
                connection?.disconnect()
            }
        }
    }
}
