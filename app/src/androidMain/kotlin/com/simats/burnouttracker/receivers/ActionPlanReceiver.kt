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
        val prefs = context.getSharedPreferences("action_plan", Context.MODE_PRIVATE)
        val limitSocial = prefs.getBoolean("limit_social", false)
        val socialLimitMins = prefs.getInt("social_limit", 60)
        
        val limitStreaming = prefs.getBoolean("limit_streaming", false)
        val streamingLimitMins = prefs.getInt("streaming_limit", 120)

        // Fast fail if both are false
        if (!limitSocial && !limitStreaming) return

        val helper = com.simats.burnouttracker.utils.UsageStatsHelper(context)
        if (!helper.hasUsageStatsPermission()) return // Can't check limits without permission
        
        val stats = helper.fetchDailyUsage()

        if (limitSocial) {
            val socialMins = (stats.socialHours * 60).toInt()
            if (socialMins > socialLimitMins) {
                sendNotification(context, 1001, "Social Limit Exceeded", "You've spent $socialMins minutes on social media. Time to disconnect!")
            }
        }

        if (limitStreaming) {
            val streamingMins = (stats.streamingHours * 60).toInt()
            if (streamingMins > streamingLimitMins) {
                sendNotification(context, 1002, "Streaming Limit Exceeded", "You've spent $streamingMins minutes streaming. Rest your eyes!")
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
