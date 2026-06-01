package ca.taplog.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class ConnectivityReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (isConnected(context)) {
            scheduleSyncIfNeeded(context)
        }
    }

    private fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

fun scheduleSyncIfNeeded(context: Context) {
    val constraints = androidx.work.Constraints.Builder()
        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
        .build()

    val syncRequest = androidx.work.OneTimeWorkRequestBuilder<SyncWorker>()
        .setConstraints(constraints)
        .build()

    androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
        "taplog_sync",
        androidx.work.ExistingWorkPolicy.KEEP,
        syncRequest
    )
}