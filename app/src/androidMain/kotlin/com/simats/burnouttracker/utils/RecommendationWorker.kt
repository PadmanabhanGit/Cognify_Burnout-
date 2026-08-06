package com.simats.burnouttracker.utils

import android.content.Context
import android.content.Intent
import androidx.work.*
import com.simats.burnouttracker.receivers.ActionPlanReceiver
import java.util.concurrent.TimeUnit

class RecommendationWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        if (!AppData.allowAllNotif || !AppData.dailyReminders) {
            return Result.success()
        }
        
        return try {
            val score = AppData.predictedScore
            val recommendations = RecommendationEngine.generate(score)
            
            if (recommendations.isNotEmpty()) {
                val randomRec = recommendations.random()
                val intent = Intent(applicationContext, ActionPlanReceiver::class.java).apply {
                    putExtra("EXTRA_TITLE", randomRec.title)
                    putExtra("EXTRA_MESSAGE", randomRec.subtitle)
                    putExtra("EXTRA_ID", (System.currentTimeMillis() % 10000).toInt())
                }
                applicationContext.sendBroadcast(intent)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<RecommendationWorker>(30, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "RecommendationNotificationWork",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
