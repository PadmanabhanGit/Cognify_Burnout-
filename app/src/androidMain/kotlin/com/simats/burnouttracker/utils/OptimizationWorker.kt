package com.simats.burnouttracker.utils

import android.content.Context
import android.content.Intent
import androidx.work.*
import com.simats.burnouttracker.receivers.ActionPlanReceiver
import java.util.Calendar
import java.util.concurrent.TimeUnit

class OptimizationWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        if (!AppData.allowAllNotif || !AppData.burnoutAlerts) {
            return Result.success()
        }
        
        return try {
            val suggestions = listOf(
                "Your focus drops significantly after 90 minutes. Try the Pomodoro technique.",
                "You are most productive between 10AM - 11AM. Schedule your hardest tasks for this window."
            )
            val randomSuggestion = suggestions.random()
            
            val intent = Intent(applicationContext, ActionPlanReceiver::class.java).apply {
                putExtra("EXTRA_TITLE", "End of Day Optimization ✨")
                putExtra("EXTRA_MESSAGE", randomSuggestion)
                putExtra("EXTRA_ID", (System.currentTimeMillis() % 10000).toInt())
            }
            applicationContext.sendBroadcast(intent)
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val currentTime = Calendar.getInstance()
            val targetTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 20) // 8:00 PM
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }

            if (targetTime.before(currentTime)) {
                targetTime.add(Calendar.DAY_OF_MONTH, 1)
            }

            val delay = targetTime.timeInMillis - currentTime.timeInMillis

            val request = PeriodicWorkRequestBuilder<OptimizationWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "OptimizationNotificationWork",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
