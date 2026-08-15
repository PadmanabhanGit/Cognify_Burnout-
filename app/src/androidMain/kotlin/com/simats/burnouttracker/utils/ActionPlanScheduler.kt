package com.simats.burnouttracker.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar
import com.simats.burnouttracker.receivers.ActionPlanReceiver

object ActionPlanScheduler {

    /**
     * Every alarm id this object registers. Kept in one list so session teardown
     * cannot cancel some and leave others firing for a signed-out account.
     * 101 sleep · 102 mindfulness · 103 hydration · 104 entertainment limits ·
     * 105 study timer.
     */
    private val ALL_ALARM_IDS = listOf(101, 102, 103, 104, 105)

    fun scheduleAlarms(context: Context) {
        // Scoped store: the action plan is one person's wellness settings, so a
        // signed-out or newly switched account must not inherit the previous
        // account's bedtime, reminders or app limits.
        val prefs = PrefStores.open(context, "action_plan")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Sleep Reminder
        if (prefs.getBoolean("sleepReminderEnabled", true)) {
            val h = prefs.getInt("sleepHour", 23)
            val m = prefs.getInt("sleepMinute", 0)
            scheduleDailyAlarm(context, alarmManager, 101, h, m, "Prioritize Sleep", "It's almost bedtime. Time to wind down.")
        } else {
            cancelAlarm(context, alarmManager, 101)
        }

        // Mindfulness Reminder
        if (prefs.getBoolean("mindfulnessEnabled", true)) {
            val h = prefs.getInt("mindfulnessHour", 14)
            val m = prefs.getInt("mindfulnessMinute", 0)
            scheduleDailyAlarm(context, alarmManager, 102, h, m, "Mindfulness Break", "Take 10 minutes to clear your mind and breathe.")
        } else {
            cancelAlarm(context, alarmManager, 102)
        }
        
        // Hydration Reminder (Interval)
        if (prefs.getBoolean("hydrationEnabled", true)) {
            val interval = prefs.getInt("hydrationInterval", 2)
            scheduleRepeatingAlarm(context, alarmManager, 103, interval, "Hydration Reminder", "Don't forget to drink some water!")
        } else {
            cancelAlarm(context, alarmManager, 103)
        }
        // Entertainment Limits Monitor (Check every hour)
        if (prefs.getBoolean("limit_social", false) || prefs.getBoolean("limit_streaming", false) || prefs.getBoolean("limit_gaming", false)) {
            scheduleRepeatingAlarm(context, alarmManager, 104, 1, "Entertainment Check", "LIMIT_CHECK")
        } else {
            cancelAlarm(context, alarmManager, 104)
        }
    }

    fun scheduleStudyTimer(context: Context, durationMins: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ActionPlanReceiver::class.java).apply {
            putExtra("EXTRA_ID", 105)
            putExtra("EXTRA_TITLE", "Focus Session Complete!")
            putExtra("EXTRA_MESSAGE", "Great job! It's time to take your scheduled break.")
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 105, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val triggerAt = System.currentTimeMillis() + (durationMins * 60 * 1000L)
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun cancelStudyTimer(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelAlarm(context, alarmManager, 105)
    }

    /**
     * Cancels every alarm this object can register, regardless of what the
     * stored plan currently says.
     *
     * [scheduleAlarms] cancels an alarm only when its feature is switched off,
     * so it cannot be used for teardown: after sign-out the store still says
     * "bedtime reminder on", and re-running it would faithfully re-register the
     * reminder for a user who is no longer signed in.
     */
    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        ALL_ALARM_IDS.forEach { id ->
            try {
                cancelAlarm(context, alarmManager, id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun scheduleDailyAlarm(context: Context, alarmManager: AlarmManager, id: Int, hour: Int, minute: Int, title: String, msg: String) {
        val intent = Intent(context, ActionPlanReceiver::class.java).apply {
            putExtra("EXTRA_ID", id)
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_MESSAGE", msg)
        }
        val pendingIntent = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun scheduleRepeatingAlarm(context: Context, alarmManager: AlarmManager, id: Int, intervalHours: Int, title: String, msg: String) {
        val intent = Intent(context, ActionPlanReceiver::class.java).apply {
            putExtra("EXTRA_ID", id)
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_MESSAGE", msg)
        }
        val pendingIntent = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val intervalMillis = intervalHours * 3600 * 1000L
        val triggerAt = System.currentTimeMillis() + intervalMillis
        try {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                intervalMillis,
                pendingIntent
            )
        } catch(e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun cancelAlarm(context: Context, alarmManager: AlarmManager, id: Int) {
        val intent = Intent(context, ActionPlanReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
    }
}
