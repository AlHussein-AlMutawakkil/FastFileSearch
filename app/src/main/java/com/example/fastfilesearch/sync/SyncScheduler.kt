package com.example.fastfilesearch.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncScheduler {
    private const val TAG = "SyncScheduler"
    private const val SYNC_INTERVAL_HOURS = 6L

    fun schedulePeriodicSync(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Fully offline app
                .build()

            val workRequest = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(
                SYNC_INTERVAL_HOURS,
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                BackgroundSyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            Log.i(TAG, "Periodic sync scheduled every $SYNC_INTERVAL_HOURS hours.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed scheduling periodic sync", e)
        }
    }

    fun cancelPeriodicSync(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(BackgroundSyncWorker.WORK_NAME)
            Log.i(TAG, "Periodic sync cancelled.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed cancelling periodic sync", e)
        }
    }
}
