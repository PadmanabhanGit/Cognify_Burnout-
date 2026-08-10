package com.simats.burnouttracker.utils

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.simats.burnouttracker.data.database.*
import java.util.*
import java.text.SimpleDateFormat

class SleepMonitoringEngine(private val context: Context) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val sleepDao = SleepDatabase.getDatabase(context).sleepDao()

    private val categories = mapOf(
        "com.instagram.android" to "SOCIAL",
        "com.facebook.katana" to "SOCIAL",
        "com.facebook.orca" to "MESSAGING",
        "com.whatsapp" to "MESSAGING",
        "com.twitter.android" to "SOCIAL",
        "com.reddit.frontpage" to "SOCIAL",
        "com.snapchat.android" to "SOCIAL",
        "com.zhiliaoapp.musically" to "SOCIAL",
        "com.google.android.youtube" to "VIDEO",
        "com.netflix.mediaclient" to "VIDEO",
        "com.amazon.avod.thirdpartyclient" to "VIDEO",
        "com.disney.hotstar" to "VIDEO",
        "com.jio.media.ondemand" to "VIDEO",
        "org.telegram.messenger" to "MESSAGING",
        "com.android.chrome" to "PRODUCTIVITY",
        "com.google.android.gm" to "PRODUCTIVITY",
        "com.google.android.apps.docs" to "PRODUCTIVITY"
    )

    private fun getCategory(packageName: String): String {
        return categories[packageName] ?: "OTHER"
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            packageName.split(".").last()
        }
    }

    suspend fun analyzeNight(date: Date) {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 21)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        val endTime = calendar.timeInMillis

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val eventList = mutableListOf<UsageEvents.Event>()
        while (events.hasNextEvent()) {
            val event = UsageEvents.Event()
            events.getNextEvent(event)
            eventList.add(event)
        }

        var sleepStart = startTime
        var sleepEnd = endTime

        if (eventList.isEmpty()) {
            // No activity in the entire window -> sleep is the whole window
            sleepStart = startTime + 60 * 60 * 1000 // Assume they slept at 10:00 PM if no activity
        } else {
            // 1. Detect Sleep Start
            // Find first gap of at least 45 mins
            var gapFound = false
            for (i in 0 until eventList.size - 1) {
                val current = eventList[i]
                val next = eventList[i+1]
                
                if (next.timeStamp - current.timeStamp > 45 * 60 * 1000) {
                    sleepStart = current.timeStamp + 15 * 60 * 1000 // 15 mins after last activity
                    gapFound = true
                    break
                }
            }
            
            if (!gapFound) {
                // Check gap between last event and endTime
                if (endTime - eventList.last().timeStamp > 45 * 60 * 1000) {
                    sleepStart = eventList.last().timeStamp + 15 * 60 * 1000
                } else {
                    return // No sleep detected
                }
            }

            // Detect Sleep End (First interaction after 04:00 AM)
            val fourAmCal = Calendar.getInstance()
            fourAmCal.timeInMillis = startTime
            fourAmCal.add(Calendar.DAY_OF_YEAR, 1)
            fourAmCal.set(Calendar.HOUR_OF_DAY, 4)
            val fourAm = fourAmCal.timeInMillis

            var wakeFound = false
            for (event in eventList) {
                if (event.timeStamp > fourAm && event.timeStamp > sleepStart) {
                    if (event.eventType == UsageEvents.Event.USER_INTERACTION || event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                        sleepEnd = event.timeStamp
                        wakeFound = true
                        break
                    }
                }
            }
            if (!wakeFound) {
                // Or end of final gap if no morning usage
                sleepEnd = eventList.last().timeStamp
                if (sleepEnd < sleepStart) sleepEnd = endTime
            }
        }
        
        // 2. Identify Awakenings
        val awakenings = mutableListOf<WakeEvent>()
        val usageLogs = mutableListOf<AppUsageLog>()
        
        var currentSessionStart = -1L
        var currentPackage = ""
        
        for (event in eventList) {
            if (event.timeStamp < sleepStart) continue
            
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                currentSessionStart = event.timeStamp
                currentPackage = event.packageName
            } else if (event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND || event.eventType == UsageEvents.Event.USER_INTERACTION) {
                if (currentSessionStart != -1L && event.packageName == currentPackage) {
                    val duration = event.timeStamp - currentSessionStart
                    if (duration > 0) {
                        usageLogs.add(AppUsageLog(
                            sessionId = 0,
                            startTime = currentSessionStart,
                            endTime = event.timeStamp,
                            duration = duration,
                            appName = getAppName(currentPackage),
                            packageName = currentPackage,
                            category = getCategory(currentPackage)
                        ))
                        
                        // It's an awakening if it happened during sleep and lasted > 3 minutes
                        if (currentSessionStart >= sleepStart && event.timeStamp <= sleepEnd && duration > 3 * 60 * 1000) {
                            awakenings.add(WakeEvent(
                                sessionId = 0,
                                timestamp = currentSessionStart,
                                duration = duration,
                                appName = getAppName(currentPackage),
                                packageName = currentPackage,
                                category = getCategory(currentPackage)
                            ))
                        }
                    }
                    currentSessionStart = -1L
                }
            }
        }

        // 3. Scoring
        var penalty = 0
        var socialMins = 0L
        
        usageLogs.forEach { log ->
            if (log.category == "SOCIAL") socialMins += (log.duration / 60000)
            
            // Continuous usage
            if (log.duration > 60 * 60000) penalty += 20
            else if (log.duration > 30 * 60000) penalty += 10
            
            // Time of usage penalty
            val cal = Calendar.getInstance()
            cal.timeInMillis = log.startTime
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            if (hour in 4..5) penalty += 30
            else if (hour in 2..3) penalty += 20
            else if (hour in 0..1) penalty += 10
        }
        
        // Social media penalty
        penalty += when {
            socialMins > 60 -> 35
            socialMins > 30 -> 20
            socialMins > 15 -> 10
            socialMins > 0 -> 5
            else -> 0
        }
        
        // Awakening penalty
        penalty += awakenings.size * 5
        penalty += awakenings.count { it.duration > 10 * 60000 } * 5
        
        // Unlock Frequency (simplified from event count)
        val unlockCount = eventList.count { it.eventType == 18 || it.eventType == UsageEvents.Event.USER_INTERACTION }
        penalty += when {
            unlockCount > 10 -> 20
            unlockCount > 5 -> 10
            unlockCount > 0 -> 5
            else -> 0
        }

        val quality = (100 - penalty).coerceIn(0, 100)
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        val session = SleepSession(
            date = df.format(date),
            sleepStart = sleepStart,
            sleepEnd = sleepEnd,
            totalSleepMinutes = Math.max(0, ((sleepEnd - sleepStart) / 60000).toInt() - (awakenings.sumOf { it.duration } / 60000).toInt()),
            awakeningCount = awakenings.size,
            sleepQuality = quality,
            disturbanceScore = penalty
        )

        val sessionId = sleepDao.insertSession(session)
        awakenings.forEach { sleepDao.insertWakeEvent(it.copy(sessionId = sessionId)) }
        usageLogs.forEach { sleepDao.insertUsageLog(it.copy(sessionId = sessionId)) }

        // Sync to backend automatically
        try {
            com.simats.burnouttracker.data.api.ApiClient.saveSleepMoodLog(
                com.simats.burnouttracker.data.models.SleepMoodLogRequest(
                    sleepDuration = session.totalSleepMinutes / 60.0,
                    sleepQuality = session.sleepQuality,
                    mood = "Auto-detected",
                    moodScore = 5,
                    date = session.date,
                    sleepStart = session.sleepStart,
                    sleepEnd = session.sleepEnd,
                    awakeningCount = session.awakeningCount,
                    disturbanceScore = session.disturbanceScore
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
