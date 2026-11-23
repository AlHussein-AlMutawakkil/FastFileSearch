package com.example.fastfilesearch.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fastfilesearch.SearchEngine

class BackgroundSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "BackgroundSyncWorker"
        const val WORK_NAME = "FastFileSearch_BackgroundSync"
    }

    override suspend fun doWork(): Result {
        val searchEngine = SearchEngine(context)

        // Check user setting
        if (!searchEngine.isBackgroundSyncEnabled()) {
            Log.d(TAG, "Background sync is disabled in settings. Skipping.")
            return Result.success()
        }

        return try {
            Log.i(TAG, "Starting silent incremental background sync across all workspaces...")
            val summary = searchEngine.syncAllWorkspacesIncremental()
            Log.i(TAG, "Background sync completed: ${summary.indexedCount} indexed, ${summary.skippedCount} skipped, ${summary.deletedCount} deleted.")
            Result.success()
        } catch (t: Throwable) {
            Log.e(TAG, "Silent background sync failed", t)
            // Do not fail aggressively; retry if appropriate
            Result.retry()
        }
    }
}
