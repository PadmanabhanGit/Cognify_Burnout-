package com.simats.burnouttracker.utils

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import java.util.Date

class SleepWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            val engine = SleepMonitoringEngine(applicationContext)
            engine.analyzeNight(Date())
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresCharging(false)
                .build()

            val request = PeriodicWorkRequestBuilder<SleepWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setInitialDelay(calculateDelayUntilMorning(), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "SleepAnalysisWork",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        private fun calculateDelayUntilMorning(): Long {
            // Delay until 6:01 AM
            val now = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 6)
            calendar.set(java.util.Calendar.MINUTE, 1)
            calendar.set(java.util.Calendar.SECOND, 0)
            
            if (calendar.timeInMillis <= now) {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis - now
        }
    }
}
