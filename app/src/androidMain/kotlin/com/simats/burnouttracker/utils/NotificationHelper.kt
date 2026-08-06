package com.simats.burnouttracker.utils

import android.content.Context
import androidx.work.WorkManager

object NotificationHelper {

    fun updateWorkers(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // 1. Burnout Alerts (OptimizationWorker)
        if (AppData.allowAllNotif && AppData.burnoutAlerts) {
            OptimizationWorker.enqueue(context)
        } else {
            workManager.cancelUniqueWork("OptimizationNotificationWork")
        }

        // 2. Daily Reminders (RecommendationWorker)
        if (AppData.allowAllNotif && AppData.dailyReminders) {
            RecommendationWorker.enqueue(context)
        } else {
            workManager.cancelUniqueWork("RecommendationNotificationWork")
        }

        // 3. Sleep / Study Prompts (SleepWorker)
        if (AppData.allowAllNotif && AppData.studyPrompts) {
            SleepWorker.enqueue(context)
        } else {
            workManager.cancelUniqueWork("SleepAnalysisWork")
        }
        
        // Note: Weekly reports currently don't have a specific worker.
        // If added in the future, it would be toggled here based on AppData.weeklyReports
    }
}
