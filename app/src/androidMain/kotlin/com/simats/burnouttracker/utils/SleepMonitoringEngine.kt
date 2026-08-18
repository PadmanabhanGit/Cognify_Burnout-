package com.simats.burnouttracker.utils

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.simats.burnouttracker.data.database.*
import com.simats.burnouttracker.data.models.SyncStateMachine
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
         *
         * Shared with [SleepHistoryRestore]: detection and cloud restore both
         * check "does this account already hold this date?" and then insert, and
         * that check-then-act is only safe if the two cannot interleave. Restore
         * runs from SessionManager.begin while screens are already firing
         * refreshSleepData(), so they genuinely do overlap in practice. A lock
         * private to detection would have left exactly that pair unserialized.
         */
        internal val sleepWriteMutex = Mutex()

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
    suspend fun analyzeNight(date: Date) = sleepWriteMutex.withLock {
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
        // Account isolation: a night that started before the active account
        // existed on this device is not that account's data. Only the query's
        // lower bound moves — NIGHT_START_HOUR, every threshold, the gap and
        // cluster detection and the scoring formula are all unchanged. With a
        // truncated window the existing guards simply find no qualifying gap or
        // wake cluster and record nothing, which is the correct outcome.
        val startTime = AccountScope.clampWindowStart(context, evening.timeInMillis)

        // ── Upper bound for THIS pass: post-wake analysis ────────────────────
        //
        // The night window still officially closes at NIGHT_END_HOUR (09:00).
        // Previously ANY call before that returned immediately, so a user who
        // genuinely woke at 06:02 could not see their night until 09:00 even
        // though the evidence for it already existed in UsageStats.
        //
        // An earlier pass is now permitted, bounded by the current time. This
        // does NOT reintroduce any assumption: the clock never supplies a wake
        // time. The cluster search below is unchanged and still has to find
        // >= WAKE_CONFIRM_MS of sustained activity after EARLIEST_WAKE_HOUR,
        // and `sleepEnd < 0` still means "no confirmed wake -> no session"
        // (the safety fix). If the user has not actually become active yet,
        // there is no cluster, and nothing is recorded — exactly as before.
        //
        // For yesterday and the day before (the other dates refreshSleepData()
        // scans) endTime is already in the past, so effectiveEnd == endTime and
        // their behaviour is bit-for-bit identical to before this change.
        val now = System.currentTimeMillis()
        val effectiveEnd = minOf(endTime, now)

        if (effectiveEnd <= startTime) {
            println("[SLEEP] Window has not started yet (starts ${Date(startTime)}); skipping.")
            return
        }

        // Skip nights already analysed — prevents duplicate Room rows and duplicate
        // Firestore POSTs when refreshSleepData() re-scans the last 3 days.
        val dateLabel = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
        // Scoped to the active account: another account's night for this date is
        // not this account's night, and must not suppress its analysis.
        if (sleepDao.getSessionByDate(dateLabel, AccountScope.activeUid(context)) != null) return

        val events = usageStatsManager.queryEvents(startTime, effectiveEnd)
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
        // Bounded by effectiveEnd so an early pass cannot count time that has
        // not elapsed yet as observed inactivity.
        val lastActivity = activity.last()
        val trailing = effectiveEnd - lastActivity
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

        // -1 means "no wake cluster confirmed yet", NOT a time.
        //
        // This was previously seeded with `endTime` (the 09:00 window close). If
        // the loop below found no qualifying cluster, that boundary survived and
        // was stored as though it were a detected wake time — so an absence of
        // wake evidence rendered as "you slept until 9:00 AM", with the maximum
        // possible duration and, because no events existed to penalise, a 100%
        // quality score. The cluster search itself is unchanged; only the
        // no-evidence outcome changes, at the guard below.
        var sleepEnd = -1L
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

        // ── 2b. No confirmed wake → no session, same as the other no-evidence exits ──
        //
        // Consistent with the three guards already above (no usage events, no
        // sustained inactivity gap, session too short): when the evidence isn't
        // there, the engine records nothing rather than substituting a
        // plausible-looking value. Every downstream surface already renders an
        // honest unavailable state for "no session".
        //
        // NOTE: this tests for WAKE EVIDENCE only. It is deliberately not
        // related to awakeningCount — a night with a real sleep start, a real
        // confirmed wake cluster and zero awakenings is perfectly legitimate and
        // is still recorded exactly as before.
        if (sleepEnd < 0) {
            println("[SLEEP] No confirmed wake cluster after ${EARLIEST_WAKE_HOUR}:00 on $dateLabel; no session recorded (window boundary is not wake evidence).")
            return
        }

        // ── 3. Reject implausibly short sessions rather than padding the UI ──
        if (sleepEnd - sleepStart < MIN_SESSION_MS) {
            println("[SLEEP] Session on $dateLabel shorter than 2h; rejected as a nap.")
            return
        }

        // 2. Identify Awakenings
        val awakenings = mutableListOf<WakeEvent>()
        val usageLogs = mutableListOf<AppUsageLog>()

        // Per-package open sessions, not a single current-session variable.
        // The previous single-variable version discarded a session outright
        // whenever a DIFFERENT package's FOREGROUND event arrived before the
        // first package's own close: `currentSessionStart`/`currentPackage`
        // were overwritten, so that package's start time was gone by the time
        // its real BACKGROUND event showed up (packageName no longer matched,
        // so the event was silently ignored). That stretch of real overnight
        // usage — and any awakening inside it — never made it into usageLogs
        // or awakenings. Each package now tracks its own open session
        // independently, the same structure already used in
        // UsageEventAccounting for the daytime usage totals. "Keep the
        // earliest open" on a repeated FOREGROUND matches that same file, so
        // a duplicate resume (e.g. multiple Activity instances of one
        // package) cannot restart the clock and shorten the session either.
        val openSessions = mutableMapOf<String, Long>()

        for (event in eventList.sortedBy { it.timeStamp }) {
            if (event.timeStamp < sleepStart) continue

            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                if (!openSessions.containsKey(event.packageName)) {
                    openSessions[event.packageName] = event.timeStamp
                }
            } else if (event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND || event.eventType == UsageEvents.Event.USER_INTERACTION) {
                // No open session for this package (never opened, or already
                // closed by an earlier event) — nothing to pair with, and
                // nothing is fabricated for it, same as before.
                val sessionStart = openSessions.remove(event.packageName) ?: continue
                val duration = event.timeStamp - sessionStart
                if (duration > 0) {
                    usageLogs.add(AppUsageLog(
                        sessionId = 0,
                        startTime = sessionStart,
                        endTime = event.timeStamp,
                        duration = duration,
                        appName = getAppName(event.packageName),
                        packageName = event.packageName,
                        category = getCategory(event.packageName)
                    ))

                    // It's an awakening if it happened during sleep and lasted > 3 minutes
                    if (sessionStart >= sleepStart && event.timeStamp <= sleepEnd && duration > 3 * 60 * 1000) {
                        awakenings.add(WakeEvent(
                            sessionId = 0,
                            timestamp = sessionStart,
                            duration = duration,
                            appName = getAppName(event.packageName),
                            packageName = event.packageName,
                            category = getCategory(event.packageName)
                        ))
                    }
                }
            }
        }

        // 3. Scoring
        var penalty = 0
        var socialMins = 0L

        // A touch shorter than this is noise — an accidental screen wake, a
        // notification auto-dismissed without being read — not a real return
        // to the phone. Without this floor, every one of these still drew the
        // full continuous-usage AND time-of-night penalty (up to 50 points
        // each), so a handful of sub-minute touches concentrated in the
        // highest-penalty hours (2-6am) could push the total into the
        // hundreds despite none of them being long enough to register as an
        // awakening (> 3 min, checked separately below). That mismatch — zero
        // awakenings, "Very Poor" quality — is exactly what this guards
        // against; the awakening threshold itself is untouched.
        val MEANINGFUL_USAGE_MS = 60 * 1000L

        usageLogs.forEach { log ->
            if (log.category == "SOCIAL") socialMins += (log.duration / 60000)

            if (log.duration < MEANINGFUL_USAGE_MS) return@forEach

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

        // The sources above have no natural upper bound and can all stack in
        // the same night (long sessions, heavy social time, several
        // awakenings, high unlock count). Capping here is what keeps `quality`
        // a genuine 0-100 reading rather than however negative the uncapped
        // total happened to go, and keeps `disturbanceScore` — stored and
        // displayed as this SAME penalty value, uncapped, on both Android and
        // the web — a number on a fixed 0-100 scale instead of an unbounded
        // one with nothing to read it against.
        penalty = penalty.coerceIn(0, 100)

        val quality = (100 - penalty).coerceIn(0, 100)
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        val session = SleepSession(
            date = df.format(date),
            sleepStart = sleepStart,
            sleepEnd = sleepEnd,
            totalSleepMinutes = Math.max(0, ((sleepEnd - sleepStart) / 60000).toInt() - (awakenings.sumOf { it.duration } / 60000).toInt()),
            awakeningCount = awakenings.size,
            sleepQuality = quality,
            disturbanceScore = penalty,
            // Ownership only. Every detection value above is produced exactly as
            // before; this records which account the night was detected for.
            ownerUid = AccountScope.activeUid(context)
        )

        val sessionId = sleepDao.insertSession(session)
        awakenings.forEach { sleepDao.insertWakeEvent(it.copy(sessionId = sessionId)) }
        usageLogs.forEach { sleepDao.insertUsageLog(it.copy(sessionId = sessionId)) }

        // Sync the night that was just written. The outcome is persisted, so a
        // failure here is retried later rather than lost — see [syncSession].
        syncSession(session.copy(id = sessionId))
    }

    /**
     * Sends one detected night to the backend and records whether it landed.
     *
     * The SAME canonical `session` object held in Room is sent here. There is no
     * separate or simplified recalculation for sync, so date / sleepStart /
     * sleepEnd / duration / quality / awakenings / disturbance / source are
     * identical in Room and Firestore by construction.
     *
     * ApiClient.saveSleepMoodLog already catches its own transport errors and
     * returns success=false, so the real failure signal is the RETURN VALUE. The
     * outer try/catch is kept only for anything the client itself might rethrow.
     *
     * STATE TRANSITIONS. The row is moved to SYNCING before the request goes
     * out, then to SYNCED or FAILED by the outcome — so a night is observably
     * in-flight rather than appearing to sit untouched, and a failure leaves a
     * persisted reason instead of a log line that dies with the process.
     *
     * On success the row's [SleepSession.syncedAt] is stamped, which is what
     * takes it out of the retry set. On failure the row is left at 0, marked
     * FAILED and retained — local data is never rolled back because a network
     * call failed, and no placeholder record is created. [retryUnsyncedSessions]
     * picks it up on a later refresh.
     *
     * @return true when the backend accepted the night.
     */
    suspend fun syncSession(session: SleepSession): Boolean {
        // Before the request, not after: a row that is merely PENDING and one
        // whose upload is actually in flight have to be distinguishable, and if
        // the process dies here the row stays retryable (getUnsyncedSessions
        // selects on `<> SYNCED`, so SYNCING is picked up again).
        sleepDao.markSessionSyncing(session.id, System.currentTimeMillis())

        val response = try {
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
            com.simats.burnouttracker.data.models.SleepMoodLogResponse(
                success = false,
                message = "${e::class.simpleName}: ${e.message}"
            )
        }

        if (response.success) {
            sleepDao.markSessionSynced(session.id, System.currentTimeMillis())
            println("[SLEEP SYNC] ${session.date} SYNCING -> SYNCED (source=automatic).")
        } else {
            // The persisted reason, not just a printed one. A dead network and a
            // DTO that no longer matches the backend are both success=false here,
            // but only one of them is fixable by waiting — so the kind is recorded
            // on the row rather than inferred later from a log nobody kept.
            val error = SyncStateMachine.describeFailure(response.message)
            sleepDao.markSessionFailed(session.id, error, System.currentTimeMillis())
            println(
                "[SLEEP SYNC] ${session.date} SYNCING -> FAILED: $error. " +
                "Room session retained; it will be retried on a later refresh."
            )
        }
        return response.success
    }

    /**
     * Re-sends nights this account holds locally that the backend never
     * confirmed, within [sinceDate] (`yyyy-MM-dd`) onwards.
     *
     * This is the half of sync that was missing. Detection already skips nights
     * already present in Room, so a night whose POST failed was never revisited
     * — it stayed on the phone and never appeared on the web. Retrying from the
     * stored row (rather than re-analysing) means the resent values are exactly
     * the ones Room holds, so a successful retry makes web agree with the phone
     * by construction.
     *
     * Safe to run on every refresh: the server keys automatic writes by
     * (user, date) and merges, so a retry updates that night's document instead
     * of adding another.
     *
     * @return how many nights were accepted by the backend on this pass.
     */
    /**
     * Recovers rows stranded in SYNCING by a process death, for [uid].
     *
     * Run once per session start, before anything can be in flight. See
     * [SleepDao.reclaimStrandedSyncing] for why relabelling — rather than
     * re-sending or leaving them alone — is the correct recovery.
     *
     * @return how many rows were reclaimed.
     */
    suspend fun reclaimStrandedSyncs(uid: String): Int {
        val reclaimed = sleepDao.reclaimStrandedSyncing(
            uid,
            "INTERRUPTED: upload did not complete (process ended mid-sync); retryable"
        )
        if (reclaimed > 0) {
            println("[SLEEP SYNC] Reclaimed $reclaimed night(s) stranded in SYNCING -> FAILED (retryable).")
        }
        return reclaimed
    }

    suspend fun retryUnsyncedSessions(sinceDate: String): Int {
        val ownerUid = AccountScope.activeUid(context)
        val pending = sleepDao.getUnsyncedSessions(ownerUid, sinceDate)
        if (pending.isEmpty()) return 0

        println("[SLEEP SYNC] ${pending.size} unsynced night(s) since $sinceDate; retrying.")
        var synced = 0
        for (session in pending) {
            // The session belongs to the account that was active when it was
            // read. If the user signs out mid-retry the remaining nights are
            // simply left pending rather than sent under a new identity.
            if (AccountScope.activeUid(context) != ownerUid) {
                println("[SLEEP SYNC] Account changed mid-retry; ${pending.size - synced} night(s) left pending.")
                break
            }
            if (syncSession(session)) synced++
        }
        return synced
    }
}
