package com.simats.burnouttracker.utils

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.simats.burnouttracker.data.database.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.text.SimpleDateFormat

class SleepMonitoringEngine(private val context: Context) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val sleepDao = SleepDatabase.getDatabase(context).sleepDao()

    companion object {
        /**
         * Process-wide serialization for night analysis.
         *
         * The duplicate-date guard below (getSessionByDate -> return) and the
         * insert at the end of analyzeNightLocked() are separated by the whole
         * analysis body. Without this lock, two coroutines analysing the same
         * date could BOTH pass the guard before either inserted, producing two
         * identical rows for one night — the confirmed cause of the observed
         * "Aug 9 x6" history.
         *
         * This lives in the companion object, not on an instance, because there
         * is more than one SleepMonitoringEngine in the process: SleepWorker
         * constructs its own (SleepWorker.kt), separately from the one held by
         * the AndroidSleepRepository singleton. An instance-level lock would not
         * serialize those two against each other.
         *
         * It guards only ordering. It does not read, alter, or reinterpret any
         * detection input or scoring output.
         */
        private val analysisMutex = Mutex()

        // Night window: 21:00 the previous evening → 09:00 on the morning of the sleep date.
        private const val NIGHT_START_HOUR = 21
        private const val NIGHT_END_HOUR = 9

        // A gap only counts as sleep if the device was untouched this long.
        // 90 min deliberately exceeds the old 45 min so that a 45–60 min evening
        // lull can never be mistaken for the start of a night.
        private const val MIN_INACTIVITY_MS = 90 * 60 * 1000L

        // Existing settling adjustment: the user put the phone down, then fell asleep.
        private const val SETTLE_MS = 15 * 60 * 1000L

        // Activity events closer together than this belong to the same cluster.
        // 30 min < MIN_INACTIVITY_MS, so clustering can never swallow a real sleep gap,
        // yet it still keeps a foreground→background pair (e.g. a 30 min video) together.
        private const val CLUSTER_GAP_MS = 30 * 60 * 1000L

        // A cluster must span at least this long to count as a genuine wake-up.
        private const val WAKE_CONFIRM_MS = 10 * 60 * 1000L

        // Sleep cannot be confirmed as ended before this hour.
        private const val EARLIEST_WAKE_HOUR = 4

        // Gaps starting outside 21:00–05:00 are not bedtimes.
        private const val BEDTIME_WINDOW_END_HOUR = 5

        // Anything shorter than this is a nap, not a night.
        private const val MIN_SESSION_MS = 120 * 60 * 1000L

        // KEYGUARD_HIDDEN (device unlocked). Literal for API-24 compatibility;
        // the same literal is already used by the scoring section below.
        private const val EVENT_KEYGUARD_HIDDEN = 18
    }

    /** Events that indicate the user was present at the device. */
    private fun isActivityEvent(type: Int): Boolean =
        type == UsageEvents.Event.MOVE_TO_FOREGROUND ||
        type == UsageEvents.Event.MOVE_TO_BACKGROUND ||
        type == UsageEvents.Event.USER_INTERACTION ||
        type == EVENT_KEYGUARD_HIDDEN

    private fun hourOf(timestamp: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return cal.get(Calendar.HOUR_OF_DAY)
    }

    /** True when a timestamp falls in the plausible bedtime range 21:00–05:00. */
    private fun isPlausibleBedtime(timestamp: Long): Boolean {
        val hour = hourOf(timestamp)
        return hour >= NIGHT_START_HOUR || hour < BEDTIME_WINDOW_END_HOUR
    }

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

    /**
     * Analyse the night that ENDED on the morning of [date].
     *
     * Window: 21:00 the previous evening → 09:00 on [date].
     * (The previous implementation anchored 21:00 on [date] itself, so when the
     * worker ran at 06:01 the whole window was in the FUTURE, queryEvents returned
     * nothing, and the empty-window branch fabricated a 22:00→09:00 / 11h / 100%
     * session. Both of those causes are removed here.)
     *
     * State model: ACTIVE → sustained inactivity → confirmed sleep → sleeping →
     * sustained activity → awake. Brief interruptions stay inside the session.
     *
     * Produces no session at all rather than guessing.
     *
     * Serialized process-wide via [analysisMutex] so that the duplicate-date
     * guard inside is actually effective under concurrent callers. The analysis
     * itself is unchanged and lives in analyzeNightLocked().
     */
    suspend fun analyzeNight(date: Date) = analysisMutex.withLock {
        analyzeNightLocked(date)
    }

    private suspend fun analyzeNightLocked(date: Date) {
        // ── Window: previous evening 21:00 → morning 09:00 ───────────────────
        val morning = Calendar.getInstance()
        morning.time = date
        morning.set(Calendar.HOUR_OF_DAY, NIGHT_END_HOUR)
        morning.set(Calendar.MINUTE, 0)
        morning.set(Calendar.SECOND, 0)
        morning.set(Calendar.MILLISECOND, 0)
        val endTime = morning.timeInMillis

        val evening = morning.clone() as Calendar
        evening.add(Calendar.DAY_OF_YEAR, -1)
        evening.set(Calendar.HOUR_OF_DAY, NIGHT_START_HOUR)
        val startTime = evening.timeInMillis

        // Never finalise a night that has not finished yet. This makes the engine
        // safe no matter who calls it (SleepWorker, refreshSleepData, manual).
        if (System.currentTimeMillis() < endTime) {
            println("[SLEEP] Window not complete yet (ends ${Date(endTime)}); skipping.")
            return
        }

        // Skip nights already analysed — prevents duplicate Room rows and duplicate
        // Firestore POSTs when refreshSleepData() re-scans the last 3 days.
        val dateLabel = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
        if (sleepDao.getSessionByDate(dateLabel) != null) return

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val eventList = mutableListOf<UsageEvents.Event>()
        while (events.hasNextEvent()) {
            val event = UsageEvents.Event()
            events.getNextEvent(event)
            eventList.add(event)
        }

        // Presence timeline used for both boundaries.
        val activity = eventList
            .filter { isActivityEvent(it.eventType) }
            .map { it.timeStamp }
            .sorted()

        // No usable data → no detected sleep. NEVER fabricate a session.
        if (activity.isEmpty()) {
            println("[SLEEP] No usage events in $dateLabel window; no session recorded.")
            return
        }

        // ── 1. Sleep START: longest sustained inactivity beginning at a plausible bedtime ──
        var gapFrom = -1L
        var gapLength = 0L
        for (i in 0 until activity.size - 1) {
            val from = activity[i]
            val length = activity[i + 1] - from
            if (length >= MIN_INACTIVITY_MS && isPlausibleBedtime(from) && length > gapLength) {
                gapFrom = from
                gapLength = length
            }
        }
        // Trailing gap: last interaction of the night → window close.
        val lastActivity = activity.last()
        val trailing = endTime - lastActivity
        if (trailing >= MIN_INACTIVITY_MS && isPlausibleBedtime(lastActivity) && trailing > gapLength) {
            gapFrom = lastActivity
            gapLength = trailing
        }

        if (gapFrom < 0) {
            println("[SLEEP] No sustained inactivity >= 90m on $dateLabel; no session recorded.")
            return
        }

        val sleepStart = gapFrom + SETTLE_MS

        // ── 2. Sleep END: first cluster of SUSTAINED activity after 04:00 ────
        val earliestWakeCal = Calendar.getInstance()
        earliestWakeCal.timeInMillis = endTime
        earliestWakeCal.set(Calendar.HOUR_OF_DAY, EARLIEST_WAKE_HOUR)
        earliestWakeCal.set(Calendar.MINUTE, 0)
        earliestWakeCal.set(Calendar.SECOND, 0)
        earliestWakeCal.set(Calendar.MILLISECOND, 0)
        val earliestWake = earliestWakeCal.timeInMillis

        // Window already closed (guarded above), so this is a real past boundary,
        // not a fabricated future timestamp.
        var sleepEnd = endTime
        val post = activity.filter { it > sleepStart }
        var i = 0
        while (i < post.size) {
            var j = i
            while (j + 1 < post.size && post[j + 1] - post[j] <= CLUSTER_GAP_MS) j++
            val clusterStart = post[i]
            val clusterSpan = post[j] - clusterStart
            // A lone tap is an interruption; only sustained activity ends the night.
            if (clusterSpan >= WAKE_CONFIRM_MS && clusterStart >= earliestWake) {
                sleepEnd = clusterStart
                break
            }
            i = j + 1
        }

        // ── 3. Reject implausibly short sessions rather than padding the UI ──
        if (sleepEnd - sleepStart < MIN_SESSION_MS) {
            println("[SLEEP] Session on $dateLabel shorter than 2h; rejected as a nap.")
            return
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
                    disturbanceScore = session.disturbanceScore,
                    source = "automatic"
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
