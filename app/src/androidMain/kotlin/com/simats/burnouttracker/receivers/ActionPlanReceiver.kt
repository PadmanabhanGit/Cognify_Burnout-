package com.simats.burnouttracker.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.simats.burnouttracker.R

class ActionPlanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Wellness Reminder"
        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Take a moment for yourself."
        val notificationId = intent.getIntExtra("EXTRA_ID", 1)

        // Intercept Limits Check (104)
        if (notificationId == 104) {
            checkEntertainmentLimits(context)
            return
        }

        val channelId = "wellness_channel"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Wellness Reminders", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            // Using a standard android icon as fallback, ideal to use R.drawable.app_logo in real environment
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(notificationId, notification)
    }

    private fun checkEntertainmentLimits(context: Context) {
        // Scoped to the active account — see PrefStores. This receiver is often
        // started in a fresh process, which is why resolution keys on the
        // persisted active uid rather than in-memory session state.
        val prefs = com.simats.burnouttracker.utils.PrefStores.open(context, "action_plan")
        val limitSocial = prefs.getBoolean("limit_social", false)
        val socialLimitMins = prefs.getInt("social_limit", 60)
        
        val limitStreaming = prefs.getBoolean("limit_streaming", false)
        val streamingLimitMins = prefs.getInt("streaming_limit", 120)

        val limitGaming = prefs.getBoolean("limit_gaming", false)
        val gamingLimitMins = prefs.getInt("gaming_limit", 60)

        // Fast fail if all are false
        if (!limitSocial && !limitStreaming && !limitGaming) return

        val helper = com.simats.burnouttracker.utils.UsageStatsHelper(context)
        if (!helper.hasUsageStatsPermission()) return // Can't check limits without permission
        
        val stats = helper.fetchDailyUsage()

        if (limitSocial) {
            val socialMins = (stats.socialHours * 60).toInt()
            if (socialMins > socialLimitMins) {
                sendNotification(context, 1001, "Time for a digital break 🌱", "You've enjoyed $socialMins mins of social media today. Consider taking some time to disconnect and recharge.")
            }
        }

        if (limitStreaming) {
            val streamingMins = (stats.streamingHours * 60).toInt()
            if (streamingMins > streamingLimitMins) {
                sendNotification(context, 1002, "Give your eyes a rest 🌿", "You've been streaming for $streamingMins mins. A quick break will do wonders for your focus!")
            }
        }

        // Gaming — same shape as Social and Streaming above, using the
        // gamingHours the existing usage pipeline already produces.
        if (limitGaming) {
            val gamingMins = (stats.gamingHours * 60).toInt()
            if (gamingMins > gamingLimitMins) {
                sendNotification(context, 1003, "Time to step away 🎮", "You've been gaming for $gamingMins mins today. A short break helps you come back sharper.")
            }
        }
    }

    private fun sendNotification(context: Context, id: Int, title: String, msg: String) {
        val channelId = "wellness_channel"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Wellness Reminders", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(msg)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(id, notification)
    }
}
