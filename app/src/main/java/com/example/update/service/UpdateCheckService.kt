package com.example.update.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Background service for periodic update checks
 */
class UpdateCheckService : Service() {
    companion object {
        private const val TAG = "UpdateCheckService"
        private const val UPDATE_CHECK_WORK_TAG = "update_check_work"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "UpdateCheckService started")
        scheduleUpdateCheck()
        return START_STICKY
    }

    private fun scheduleUpdateCheck() {
        val updateCheckRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
            24, TimeUnit.HOURS
        ).apply {
            // Add some randomness to avoid thundering herd
            setInitialDelay(1, TimeUnit.HOURS)
            addTag(UPDATE_CHECK_WORK_TAG)
        }.build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            UPDATE_CHECK_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            updateCheckRequest
        )

        Log.d(TAG, "Update check scheduled")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "UpdateCheckService destroyed")
    }
}

/**
 * Worker for background update checks
 */
class UpdateCheckWorker(
    context: android.content.Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("UpdateCheckWorker", "Performing background update check")
            // TODO: Implement actual update check using repository
            Result.success()
        } catch (e: Exception) {
            Log.e("UpdateCheckWorker", "Error in update check", e)
            Result.retry()
        }
    }
}
