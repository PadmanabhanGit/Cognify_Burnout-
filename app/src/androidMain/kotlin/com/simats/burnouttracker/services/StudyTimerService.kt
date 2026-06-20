package com.simats.burnouttracker.services

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.simats.burnouttracker.MainActivity
import com.simats.burnouttracker.utils.AppData

class StudyTimerService : Service() {

    private val CHANNEL_ID = "study_timer_channel"
    private val NOTIFICATION_ID = 1001
    private val handler = Handler(Looper.getMainLooper())
    private var sessionName = ""

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateNotification()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        sessionName = intent?.getStringExtra("SESSION_NAME") ?: AppData.activeSessionName ?: "Study Session"

        when (action) {
            "START" -> {
                startForeground(NOTIFICATION_ID, createNotification(sessionName, "00:00"))
                handler.post(updateRunnable)
            }
            "STOP" -> {
                handler.removeCallbacks(updateRunnable)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun updateNotification() {
        val start = AppData.sessionStartTime
        if (start != null) {
            val seconds = (System.currentTimeMillis() - start) / 1000
            val timeStr = formatTime(seconds)
            val notification = createNotification(sessionName, "Running for $timeStr")
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun formatTime(seconds: Long): String {
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(sessionName: String, contentText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Study Session: $sessionName")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Study Timer Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
