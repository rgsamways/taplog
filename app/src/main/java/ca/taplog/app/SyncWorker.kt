package ca.taplog.app

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

private const val TAG = "SyncWorker"

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "SyncWorker started")

        val app = applicationContext as TapLogApplication

        return try {
            val result = app.syncRepository.syncAll()
            if (result.shouldRetry) {
                Log.w(TAG, "Sync had failures — retrying")
                Result.retry()
            } else {
                Log.i(TAG, "Sync complete — synced=${result.synced} conflicts=${result.conflicts}")
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker exception: ${e.message}")
            Result.retry()
        }
    }
}