package com.simats.burnouttracker.data

import android.content.Context
import com.simats.burnouttracker.data.database.*
import com.simats.burnouttracker.data.models.*
import com.simats.burnouttracker.utils.AccountScope
import com.simats.burnouttracker.utils.SleepMonitoringEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.*

class AndroidSleepRepository(private val context: Context) : SleepRepository {
    private val database = SleepDatabase.getDatabase(context)
    private val dao = database.sleepDao()
    private val engine = SleepMonitoringEngine(context)

    /**
     * Scoped to the account that is active when the Flow is built. Screens build
     * it from composition, which happens after AccountScope has synced, and each
     * new subscription re-reads the uid — so a switch cannot leave a screen
     * serving the previous account's nights.
     */
    override fun getRecentSessions(): Flow<List<SleepSessionData>> {
        return dao.getRecentSessions(AccountScope.activeUid(context)).map { sessions ->
            sessions.map { it.toData() }
        }
    }

    override suspend fun getSessionDetails(sessionId: Long): SleepSessionData? {
        // Find session by ID - need to add this to DAO
        // For now, return null or implement
        return null 
    }

    override suspend fun getWakeEvents(sessionId: Long): List<WakeEventData> {
        return dao.getWakeEventsForSession(sessionId).map { it.toData() }
    }

    override suspend fun getUsageLogs(sessionId: Long): List<AppUsageLogData> {
        return dao.getUsageLogsForSession(sessionId).map { it.toData() }
    }

    /**
     * Single-flight refresh.
     *
     * Three screens each build their own SleepViewModel and each fire
     * refreshData() from a LaunchedEffect(Unit) — SleepMoodScreen,
     * SleepMoodDashboardScreen and SleepMoodAnalyticsScreen. Because the
     * "dashboard" route is navigated without launchSingleTop, moving between
     * them re-composes and re-fires repeatedly, so a single user session can
     * queue many overlapping full 3-day rescans.
     *
     * [refreshInFlight] makes concurrent callers return immediately instead of
     * queueing behind the lock, so a burst of navigation results in exactly one
     * scan rather than one per screen entry. Correctness does not depend on
     * this — SleepMonitoringEngine.analysisMutex already makes duplicate
     * inserts impossible — this only removes the redundant work.
     */
    override suspend fun refreshSleepData() {
        if (refreshInFlight) return
        refreshMutex.withLock {
            if (refreshInFlight) return
            refreshInFlight = true
            try {
                // Self-healing cleanup of rows created before the concurrency
                // fix existed. Only removes rows proven field-for-field
                // identical to the one retained for the same date; anything
                // that differs is left alone and reported below.
                val ownerUid = AccountScope.activeUid(context)
                val removed = dao.deleteExactDuplicateSessions(ownerUid)
                if (removed > 0) {
                    println("[SLEEP] Removed $removed redundant duplicate sleep session row(s).")
                }
                val remaining = dao.countRedundantSessionRows(ownerUid)
                if (remaining > 0) {
                    println("[SLEEP] $remaining duplicate-date row(s) remain with DIFFERING values; left untouched for manual review.")
                }

                // Refresh last 3 days to be sure — unchanged.
                val cal = Calendar.getInstance()
                for (i in 0..2) {
                    engine.analyzeNight(cal.time)
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                }

                // Re-send nights the backend never confirmed. analyzeNight above
                // skips any night already in Room, so without this a night whose
                // POST failed would stay on the phone forever and never reach the
                // web app — the phone and the web would keep showing different
                // numbers for the same account with nothing to reconcile them.
                //
                // `cal` has just been walked back across the same 3-day window,
                // so it already sits on the oldest day of that window; the retry
                // set is bounded to exactly the range this pass re-scanned.
                val sinceDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                val resynced = engine.retryUnsyncedSessions(sinceDate)
                if (resynced > 0) {
                    println("[SLEEP] Re-synced $resynced previously unsynced night(s) to the backend.")
                }
            } finally {
                refreshInFlight = false
            }
        }
    }

    companion object {
        private val refreshMutex = Mutex()

        @Volatile
        private var refreshInFlight = false
    }

    private fun SleepSession.toData() = SleepSessionData(
        id, date, sleepStart, sleepEnd, totalSleepMinutes, awakeningCount, sleepQuality, disturbanceScore
    )

    private fun WakeEvent.toData() = WakeEventData(
        id, sessionId, timestamp, duration, appName, packageName, category
    )

    private fun AppUsageLog.toData() = AppUsageLogData(
        id, sessionId, startTime, endTime, duration, appName, packageName, category
    )
}
