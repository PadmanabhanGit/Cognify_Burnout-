package com.simats.burnouttracker.utils

import android.content.Context
import com.simats.burnouttracker.data.api.ApiClient
import com.simats.burnouttracker.data.database.SleepDatabase
import com.simats.burnouttracker.data.database.SleepSession
import com.simats.burnouttracker.data.models.SyncState
import kotlinx.coroutines.sync.withLock

/**
 * The read half of sleep sync: pulls an account's own nights back down from the
 * backend into Room when that account starts a session on this device.
 *
 * WHY THIS EXISTS
 * Sync was write-only. SleepMonitoringEngine POSTed each detected night to
 * Firestore and AndroidSleepRepository retried the ones that failed, but nothing
 * ever read them back, so Room was the only thing that remembered a night. A
 * reinstall, a cleared app storage, or simply signing in on a second phone left
 * an account with a permanently empty Sleep History while its records sat intact
 * in Firestore, visible on the web app but nowhere on Android.
 *
 * WHAT THIS IS NOT
 * This is NOT an isolation mechanism, and it must not be mistaken for one. The
 * leak that motivated [AccountScope]'s data-start clamp is that
 * SleepMonitoringEngine DERIVES nights from Android UsageStats, a device-level
 * OS database with no notion of accounts — any signed-in account can read the
 * whole device's history. Restoring from the cloud does not touch that: without
 * the clamp, an account would still re-derive a previous user's nights locally
 * and then, because sync is bidirectional, PUSH them to Firestore under its own
 * userId, spreading the contamination to every device that account touches.
 * The clamp is what makes attribution correct at the source; this is continuity,
 * and the two are complementary.
 *
 * WHY IT CANNOT ITSELF LEAK
 * The request carries the signed-in account's Firebase ID token and the server
 * filters on the uid it verifies from that token. There is no uid parameter for
 * this code to get wrong. Every row written here is additionally stamped with
 * the uid this restore was started for, and that uid is re-checked against the
 * active account before each insert, so an account switch mid-restore stops the
 * pass rather than filing the remainder under whoever signed in next.
 */
internal object SleepHistoryRestore {

    /**
     * How many records to ask the backend for.
     *
     * A bound on RECORDS, not nights: GET /logs sorts all of an account's
     * sleepMoodLogs by recency — manual mood entries included — before slicing,
     * so manual logging consumes this budget. Asking for well beyond the seven
     * nights the UI windows to is what keeps a chatty mood-logger from pushing
     * their own detected nights out of the restore set.
     */
    private const val RESTORE_RECORD_LIMIT = 60

    /**
     * Restores [uid]'s nights into Room. Returns how many rows were written.
     *
     * LOCAL WINS. A date this account already holds locally is skipped, never
     * overwritten. The local row is the one the phone derived from its own
     * UsageStats and is the source the backend copy came from in the first
     * place; letting a round-tripped copy overwrite it would put lossy data
     * (the server keeps no wake events and stores duration only in hours) on
     * top of the original, and would fight with any unsynced local edit.
     *
     * Restored rows are stamped [SleepSession.syncedAt], because the backend is
     * where they just came from — leaving them at 0 would make the retry pass
     * in AndroidSleepRepository.refreshSleepData() immediately POST them all
     * straight back.
     *
     * Restored nights carry no wake_events or app_usage_logs: the backend never
     * stored them, so per-night detail for a restored night is genuinely absent
     * rather than reconstructed. Nothing is fabricated to fill the gap.
     */
    suspend fun restoreFor(context: Context, uid: String): Int {
        if (uid.isBlank()) return 0

        val app = context.applicationContext
        val response = try {
            ApiClient.getSleepMoodLogs(RESTORE_RECORD_LIMIT)
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }

        // ApiClient already converts transport failures into success=false, so a
        // network outage lands here and leaves local history untouched. Both
        // outcomes are logged: a restore that silently does nothing is
        // indistinguishable from one that had nothing to do, and that ambiguity
        // cost real debugging time.
        if (!response.success) {
            println("[RESTORE] fetch failed -> no changes (see preceding [API] line for cause)")
            return 0
        }
        if (response.logs.isEmpty()) {
            println("[RESTORE] backend returned no records for this account -> nothing to restore")
            return 0
        }

        val dao = SleepDatabase.getDatabase(app).sleepDao()
        val now = System.currentTimeMillis()
        var restored = 0

        // Everything from here is read-then-write on sleep_sessions, so it runs
        // under the same lock detection uses. Without it, analyzeNight() could
        // insert the very date this pass just found absent — the plan would still
        // be correct, and the database would still end up with two rows.
        SleepMonitoringEngine.sleepWriteMutex.withLock {
            val dates = response.logs.mapNotNull { it.date }.distinct()
            if (dates.isEmpty()) return@withLock

            // Ownership of these dates across ALL accounts: the planner needs to
            // see other owners in order to leave them alone knowingly.
            val local = dao.getOwnersForDates(dates)
                .map { SleepRestorePlanner.LocalRow(it.date, it.ownerUid) }
            // Values for THIS account's own rows only, so a same-date record can
            // be compared rather than blindly skipped or blindly overwritten.
            val ownRows = dao.getSessionsForDates(uid, dates).map {
                SleepRestorePlanner.OwnRow(
                    date = it.date,
                    sleepStart = it.sleepStart,
                    sleepEnd = it.sleepEnd,
                    totalSleepMinutes = it.totalSleepMinutes,
                    localId = it.id,
                    syncState = it.syncState,
                    lastSyncError = it.lastSyncError
                )
            }

            for (decision in SleepRestorePlanner.plan(uid, response.logs, local, ownRows)) {
                // The account can change while this runs (sign-out, switch). Every
                // insert below is owned by `uid`, so a changed active account means
                // the rest belong to a session that is no longer current: stop
                // rather than file them under whoever signed in next.
                if (AccountScope.activeUid(app) != uid) {
                    println("[RESTORE] account changed mid-restore -> stopping after $restored insert(s)")
                    return@withLock
                }

                when (decision) {
                    is SleepRestorePlanner.Decision.Unusable -> Unit // manual/mood entries, expected

                    is SleepRestorePlanner.Decision.AlreadyOwned ->
                        println("[RESTORE] date=${decision.date} existingOwner=self -> skip")

                    is SleepRestorePlanner.Decision.Conflict ->
                        // Never resolved automatically. The local row keeps its
                        // wake events and its own detection values; the divergence
                        // is surfaced instead of being silently picked.
                        // Everything needed to identify BOTH records without a
                        // further investigation: the cloud document id, the owning
                        // account, the logical date, the local row's primary key,
                        // both durations and both timestamp pairs, and the local
                        // row's sync state and last error.
                        //
                        // The Aug-10 conflict took several rounds to resolve
                        // because the report named the remote record only by
                        // (account, date) — which does not locate a legacy
                        // `.add()` document — and named the local row not at all.
                        println(
                            "[RESTORE] CONFLICT date=${decision.date} owner=$uid -> keep local, no write" +
                                " | local id=${decision.local.localId}" +
                                " ${decision.local.totalSleepMinutes}m" +
                                " start=${decision.local.sleepStart} end=${decision.local.sleepEnd}" +
                                " syncState=${decision.local.syncState}" +
                                " lastSyncError=${decision.local.lastSyncError ?: "-"}" +
                                " | remote docId=${decision.remote.remoteId}" +
                                " ${decision.remote.totalSleepMinutes}m" +
                                " start=${decision.remote.sleepStart} end=${decision.remote.sleepEnd}"
                        )

                    is SleepRestorePlanner.Decision.Insert -> {
                        if (decision.otherOwners.isNotEmpty()) {
                            println(
                                "[RESTORE] date=${decision.row.date} existingOwner=other" +
                                    "(${decision.otherOwners.size}) -> preserve existing row, insert own"
                            )
                        } else {
                            println("[RESTORE] date=${decision.row.date} no local row -> insert")
                        }
                        try {
                            dao.insertSession(decision.row.toEntity(syncedAt = now))
                            restored++
                        } catch (e: Exception) {
                            // One bad row must not abandon the rest of the history.
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        if (restored > 0) {
            println("[SLEEP RESTORE] Restored $restored night(s) from the backend for this account.")
        }
        return restored
    }

    /**
     * The only place a planned row becomes a database row.
     *
     * [SleepRestorePlanner.PlannedRow.ownerUid] is carried through unchanged — it
     * is always the restoring account, never anything taken from the payload.
     *
     * `syncedAt` is stamped because the backend is where this row just came from.
     * Leaving it at 0 would put the row straight into the retry set and have
     * AndroidSleepRepository.refreshSleepData() POST it back where it came from.
     */
    private fun SleepRestorePlanner.PlannedRow.toEntity(syncedAt: Long) = SleepSession(
        date = date,
        sleepStart = sleepStart,
        sleepEnd = sleepEnd,
        totalSleepMinutes = totalSleepMinutes,
        awakeningCount = awakeningCount,
        sleepQuality = sleepQuality,
        disturbanceScore = disturbanceScore,
        ownerUid = ownerUid,
        syncedAt = syncedAt,
        // SYNCED, not PENDING: this row was just read back FROM the backend, so
        // the backend demonstrably has it. Leaving it PENDING would put every
        // restored night straight into the retry set and POST the whole history
        // back where it came from on the next refresh.
        syncState = SyncState.SYNCED.name
    )
}
