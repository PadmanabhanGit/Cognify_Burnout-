package com.simats.burnouttracker.utils

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import java.util.Date

class SleepWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        if (!AppData.allowAllNotif || !AppData.studyPrompts) {
            return Result.success()
        }
        
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
                .setInitialDelay(calculateDelayUntilAnalysisTime(), TimeUnit.MILLISECONDS)
                .build()

            // UPDATE, not KEEP: existing installs already have this unique work
            // scheduled for 06:01. With KEEP they would stay on that schedule
            // forever, and since the engine now refuses to analyse an incomplete
            // window, they would never record a sleep session again.
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "SleepAnalysisWork",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        private fun calculateDelayUntilAnalysisTime(): Long {
            // Delay until 9:15 AM.
            //
            // The night window closes at 09:00, so running at the old 06:01 meant
            // finalising a night while the user could still be asleep — the engine
            // now refuses to analyse an incomplete window, so a 06:01 run would
            // simply do nothing. 09:15 gives the window time to close first.
            val now = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 9)
            calendar.set(java.util.Calendar.MINUTE, 15)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)

            if (calendar.timeInMillis <= now) {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis - now
        }
    }
}
