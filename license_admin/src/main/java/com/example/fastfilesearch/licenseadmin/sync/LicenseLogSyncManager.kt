package com.example.fastfilesearch.licenseadmin.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.fastfilesearch.licenseadmin.BuildConfig
import com.example.fastfilesearch.licenseadmin.data.LicenseAdminDatabase
import com.example.fastfilesearch.licenseadmin.data.PendingUploadEntity
import com.example.fastfilesearch.licenseadmin.engine.AdminLicensePayload
import com.example.fastfilesearch.licenseadmin.engine.ClientDeviceInfo
import com.example.fastfilesearch.licenseadmin.util.IssuerInfoHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.concurrent.TimeUnit

object LicenseLogSyncManager {

    private const val UNIQUE_WORK_NAME = "supabase_license_logs_upload"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun buildPendingUploadEntity(
        context: Context,
        payload: AdminLicensePayload,
        licenseCode: String,
        deviceInfo: ClientDeviceInfo?,
        clientDeviceId: String?,
        planType: String,
        durationDays: Int
    ): PendingUploadEntity {
        val issuedAtIso = Instant.ofEpochSecond(payload.iat).toString()
        val expiresAtIso = if (payload.exp <= 0L) {
            null
        } else {
            Instant.ofEpochSecond(payload.exp).toString()
        }

        return PendingUploadEntity(
            issuedAt = issuedAtIso,
            issuerId = IssuerInfoHelper.getIssuerId(context),
            issuerDevice = IssuerInfoHelper.getIssuerDevice(),
            customerName = payload.n?.trim()?.takeIf { it.isNotBlank() },
            installationId = payload.id,
            clientDeviceId = clientDeviceId?.trim()?.takeIf { it.isNotBlank() },
            clientManufacturer = deviceInfo?.manufacturer?.trim()?.takeIf { it.isNotBlank() },
            clientModel = deviceInfo?.model?.trim()?.takeIf { it.isNotBlank() },
            clientOs = deviceInfo?.osVersion?.trim()?.takeIf { it.isNotBlank() },
            clientAbi = deviceInfo?.abi?.trim()?.takeIf { it.isNotBlank() },
            clientAppVersion = deviceInfo?.appVersion?.trim()?.takeIf { it.isNotBlank() },
            planType = planType,
            durationDays = durationDays.coerceAtLeast(0),
            expiresAt = expiresAtIso,
            maxFolders = payload.mw,
            allowExcel = payload.ex == 1,
            allowAppLock = payload.lk == 1,
            licenseCode = licenseCode,
            attempts = 0
        )
    }

    fun enqueueLicenseUpload(
        context: Context,
        entity: PendingUploadEntity
    ) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                val dao = LicenseAdminDatabase.getInstance(appContext).pendingUploadDao()
                dao.insert(entity)
                enqueueUniqueUploadWork(appContext, ExistingWorkPolicy.REPLACE)
            } catch (_: Throwable) {
                // Silent by design: never surface sync errors to UI
            }
        }
    }

    fun scheduleIfPending(context: Context) {
        if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_PUBLISHABLE_KEY.isBlank()) {
            return
        }
        val appContext = context.applicationContext
        scope.launch {
            try {
                val dao = LicenseAdminDatabase.getInstance(appContext).pendingUploadDao()
                if (dao.countPending() > 0) {
                    enqueueUniqueUploadWork(appContext, ExistingWorkPolicy.KEEP)
                }
            } catch (_: Throwable) {
                // Silent by design
            }
        }
    }

    private fun enqueueUniqueUploadWork(
        appContext: Context,
        policy: ExistingWorkPolicy
    ) {
        if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_PUBLISHABLE_KEY.isBlank()) {
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SupabaseUploadWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(appContext).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            policy,
            request
        )
    }
}
