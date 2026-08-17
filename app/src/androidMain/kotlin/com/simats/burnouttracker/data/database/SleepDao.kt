package com.simats.burnouttracker.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** Ownership of one night, with no sleep values attached. See [SleepDao.getOwnersForDates]. */
data class SessionDateOwner(
    val date: String,
    val ownerUid: String
)

@Dao
interface SleepDao {
    @Insert
    suspend fun insertSession(session: SleepSession): Long

    @Insert
    suspend fun insertWakeEvent(event: WakeEvent)

    @Insert
    suspend fun insertUsageLog(log: AppUsageLog)

    /**
     * Every sleep_sessions query below takes the active account's [ownerUid] and
     * filters on it. That filter — not deletion — is what keeps one account from
     * seeing another's nights, so an account change no longer has to destroy the
     * outgoing account's history. See [SleepSession.ownerUid].
     */
    @Query("SELECT * FROM sleep_sessions WHERE ownerUid = :ownerUid ORDER BY sleepStart DESC")
    fun getAllSessions(ownerUid: String): Flow<List<SleepSession>>

    @Query("SELECT * FROM sleep_sessions WHERE date = :date AND ownerUid = :ownerUid LIMIT 1")
    suspend fun getSessionByDate(date: String, ownerUid: String): SleepSession?

    /**
     * Who owns each of [dates], across ALL accounts.
     *
     * Deliberately NOT scoped to one account — it is the one query here that must
     * see other accounts, because restore has to know that a date is already held
     * by somebody else in order to leave that row alone and say so in the log.
     * It returns ownership only, never sleep values, so it cannot become a route
     * for one account to read another's history.
     */
    @Query("SELECT date, ownerUid FROM sleep_sessions WHERE date IN (:dates)")
    suspend fun getOwnersForDates(dates: List<String>): List<SessionDateOwner>

    /**
     * The ACTIVE account's own rows for [dates], with the fields restore compares.
     *
     * Split from [getOwnersForDates] deliberately. That query spans all accounts
     * and therefore returns ownership only; this one returns values and is
     * scoped to a single [ownerUid]. Keeping them apart means no query in this
     * DAO can hand one account another account's sleep values.
     */
    @Query("SELECT * FROM sleep_sessions WHERE ownerUid = :ownerUid AND date IN (:dates)")
    suspend fun getSessionsForDates(ownerUid: String, dates: List<String>): List<SleepSession>

    /**
     * Scoped through the parent session's ownerUid, even though [sessionId] is
     * currently only ever sourced from the active account's own already-filtered
     * session list (see AndroidSleepRepository/SleepViewModel.selectSession) —
     * so this join is not closing a reachable leak today. It exists so that
     * stays true structurally rather than by convention: every other query in
     * this DAO enforces ownerUid itself rather than trusting the caller to have
     * sourced the id correctly, and this pair was the one exception.
     */
    @Query("""
        SELECT wake_events.* FROM wake_events
        INNER JOIN sleep_sessions ON sleep_sessions.id = wake_events.sessionId
        WHERE wake_events.sessionId = :sessionId AND sleep_sessions.ownerUid = :ownerUid
    """)
    suspend fun getWakeEventsForSession(sessionId: Long, ownerUid: String): List<WakeEvent>

    @Query("""
        SELECT app_usage_logs.* FROM app_usage_logs
        INNER JOIN sleep_sessions ON sleep_sessions.id = app_usage_logs.sessionId
        WHERE app_usage_logs.sessionId = :sessionId AND sleep_sessions.ownerUid = :ownerUid
    """)
    suspend fun getUsageLogsForSession(sessionId: Long, ownerUid: String): List<AppUsageLog>

    /**
     * Rows belonging to the 7 most recent distinct sleep DATES.
     *
     * Previously `ORDER BY sleepStart DESC LIMIT 7` — a cap on ROWS being used to
     * derive NIGHTS. That is the same rows-vs-nights confusion already fixed in
     * the UI: whenever a date carries more than one row, the cap is consumed by
     * duplicates and genuinely older nights fall out of the window entirely, so
     * Sleep History could show fewer nights than the device actually holds while
     * the web (which windows by date) showed more.
     *
     * The inner query groups by date first, so the limit now counts nights. Row
     * ordering is unchanged (`sleepStart DESC`), so every existing consumer that
     * reads `firstOrNull()` or filters by today behaves exactly as before; only
     * the tail of the list can grow. No row is created, altered or removed.
     *
     * Both the inner and outer query are scoped to [ownerUid], so "7 most recent
     * nights" means seven of the ACTIVE account's nights — another account's
     * dates can neither fill the window nor appear in the result.
     */
    @Query("""
        SELECT * FROM sleep_sessions
        WHERE ownerUid = :ownerUid
          AND date IN (
            SELECT date FROM sleep_sessions
            WHERE ownerUid = :ownerUid
            GROUP BY date ORDER BY date DESC LIMIT 7
          )
        ORDER BY sleepStart DESC
    """)
    fun getRecentSessions(ownerUid: String): Flow<List<SleepSession>>

    /**
     * Removes ONLY redundant rows that are field-for-field identical to the
     * row being retained for the same date.
     *
     * Retention rule: for each date, the row with the lowest id (the first one
     * ever recorded for that night) is kept. A row is deleted only when EVERY
     * detection/scoring field matches the retained row exactly — sleepStart,
     * sleepEnd, totalSleepMinutes, awakeningCount, sleepQuality and
     * disturbanceScore. If any field differs, the row is left completely
     * untouched and reported instead (see [countRedundantSessionRows]); those
     * are a data decision, not a safe automatic delete.
     *
     * Wake events and usage logs: each duplicate insert wrote its OWN copies,
     * keyed to its own sessionId, so the retained row already owns a complete,
     * self-consistent set. The deleted rows' copies are removed with them by
     * the existing ForeignKey.CASCADE on wake_events and app_usage_logs
     * (SleepEntities.kt) — no wake event belonging to the retained session is
     * touched, and no wake event is ever orphaned.
     *
     * Returns the number of rows removed.
     *
     * Scoped to [ownerUid]: duplicates are a per-account, per-date notion, so a
     * night belonging to another account can never be the retained row for, or
     * be deleted as a duplicate of, this account's night. Another account's own
     * duplicates are cleaned the next time that account is the active one.
     */
    @Query("""
        DELETE FROM sleep_sessions
        WHERE ownerUid = :ownerUid
          AND id IN (
            SELECT d.id
            FROM sleep_sessions d
            JOIN (
                SELECT date AS grpDate, MIN(id) AS keepId FROM sleep_sessions
                WHERE ownerUid = :ownerUid
                GROUP BY date
            ) g ON d.date = g.grpDate
            JOIN sleep_sessions k ON k.id = g.keepId
            WHERE d.ownerUid = :ownerUid
              AND d.id <> g.keepId
              AND d.sleepStart = k.sleepStart
              AND d.sleepEnd = k.sleepEnd
              AND d.totalSleepMinutes = k.totalSleepMinutes
              AND d.awakeningCount = k.awakeningCount
              AND d.sleepQuality = k.sleepQuality
              AND d.disturbanceScore = k.disturbanceScore
        )
    """)
    suspend fun deleteExactDuplicateSessions(ownerUid: String): Int

    /**
     * Attaches rows written before ownership existed to [ownerUid].
     *
     * Called once, by AccountScope, for the first account this device adopts —
     * the account those nights were in fact recorded by. Anything already owned
     * is left alone, so this can never move a night between accounts and is safe
     * to re-run.
     *
     * This replaces the previous `deleteAllSessions()`, which AccountScope ran on
     * every account change: it kept accounts apart only by destroying the
     * outgoing account's nights permanently, so A → B → A left A with nothing.
     * Isolation is now the ownerUid filter on every query.
     */
    @Query("UPDATE sleep_sessions SET ownerUid = :ownerUid WHERE ownerUid = ''")
    suspend fun claimUnownedSessions(ownerUid: String): Int

    /**
     * How many rows remain beyond one-per-date. After
     * [deleteExactDuplicateSessions] runs, any non-zero result means duplicate
     * dates exist whose values DIFFER — reported, never auto-deleted.
     *
     * Scoped to [ownerUid] for the same reason as the delete above: without it,
     * two accounts each holding one row for the same date would be counted as a
     * duplicate pair.
     */
    @Query("SELECT COUNT(*) - COUNT(DISTINCT date) FROM sleep_sessions WHERE ownerUid = :ownerUid")
    suspend fun countRedundantSessionRows(ownerUid: String): Int

    /**
     * An upload is starting: PENDING or FAILED → SYNCING.
     *
     * Guarded with `syncState <> 'SYNCED'` so a stray call can never move a
     * confirmed night back into an in-flight state. [SleepSession.lastSyncError]
     * is left in place on purpose — while a retry runs, the most useful thing to
     * show is still why the previous attempt failed.
     */
    @Query("""
        UPDATE sleep_sessions
        SET syncState = 'SYNCING', lastSyncAttemptAt = :now
        WHERE id = :id AND syncState <> 'SYNCED'
    """)
    suspend fun markSessionSyncing(id: Long, now: Long)

    /**
     * Records that the backend has accepted this night, so it is not re-sent.
     *
     * Written only after a POST that actually returned success — a transport
     * failure must leave [SleepSession.syncedAt] at 0, because the point of the
     * column is to remember what did NOT get through.
     *
     * The only statement that sets SYNCED, the only one that sets `syncedAt`,
     * and the only one that clears `lastSyncError`. Success is the sole way out
     * of the retry set.
     */
    @Query("""
        UPDATE sleep_sessions
        SET syncedAt = :syncedAt, syncState = 'SYNCED', lastSyncError = NULL
        WHERE id = :id
    """)
    suspend fun markSessionSynced(id: Long, syncedAt: Long)

    /**
     * An attempt finished unsuccessfully: SYNCING → FAILED, with the reason kept.
     *
     * `syncedAt` is deliberately not in the SET list — a failure cannot invent a
     * confirmation, and a record that was already SYNCED is excluded outright by
     * the guard. The row's sleep values are never touched: this records a fact
     * about the upload, not about the night, so a failed POST can never roll back
     * local data.
     */
    @Query("""
        UPDATE sleep_sessions
        SET syncState = 'FAILED', lastSyncError = :error, lastSyncAttemptAt = :now
        WHERE id = :id AND syncState <> 'SYNCED'
    """)
    suspend fun markSessionFailed(id: Long, error: String, now: Long)

    /**
     * Moves rows stranded in SYNCING back to a retryable, HONEST state.
     *
     * A row is left in SYNCING only when the process died between the request
     * going out and its outcome being recorded. The retry query already picks
     * such a row up — it selects on `<> SYNCED` — but the LABEL would stay
     * SYNCING forever, so anyone reading the row (or a future conflict report)
     * would be told an upload is in flight that no longer exists. That is
     * exactly the kind of untraceable state this phase is meant to eliminate.
     *
     * FAILED with an explicit reason is the truthful description: an attempt was
     * made and never completed. Nothing is re-sent by this statement and no
     * record is created — it only relabels, so a stranded row becomes both
     * retryable and diagnosable.
     *
     * Called at session start, before any sync is in flight, so it cannot
     * mislabel a genuinely running upload. Even if it raced one, the outcome
     * would still win: markSessionSynced has no state guard and always lands.
     *
     * Scoped to [ownerUid] like everything else here.
     */
    @Query("""
        UPDATE sleep_sessions
        SET syncState = 'FAILED', lastSyncError = :reason
        WHERE ownerUid = :ownerUid AND syncState = 'SYNCING'
    """)
    suspend fun reclaimStrandedSyncing(ownerUid: String, reason: String): Int

    /**
     * Nights this account holds locally that the backend has never confirmed,
     * limited to those on or after [sinceDate] (a `yyyy-MM-dd` label).
     *
     * Mirrors `SyncStateMachine.isRetryable`, which is the specification for this
     * query and is unit-tested; the two are stated together so the isolation rule
     * cannot drift silently between SQL and Kotlin.
     *
     * `syncState <> 'SYNCED'` rather than the old `syncedAt = 0`. After the v3→v4
     * migration those select exactly the same rows, so retry behaviour is
     * unchanged — but this form also picks up a row left in SYNCING by a process
     * killed mid-upload, which the timestamp form would have stranded forever.
     * Re-sending such a row is safe: automatic writes merge onto a deterministic
     * server-side document id.
     *
     * Bounded on purpose. Every unsynced night in history would otherwise be
     * re-sent on the first refresh after upgrading, which for a long-installed
     * device is a burst of writes to fix nights nobody is looking at. The window
     * matches the one refreshSleepData() already re-scans, so retries stay
     * proportional to the work that pass was already doing.
     *
     * Scoped to [ownerUid]: this is the account-isolation guarantee for sync.
     * Without it a retry pass would re-POST another account's nights under the
     * signed-in account's token.
     *
     * Oldest first, so a partial success still advances chronologically.
     */
    @Query("""
        SELECT * FROM sleep_sessions
        WHERE ownerUid = :ownerUid AND syncState <> 'SYNCED' AND date >= :sinceDate
        ORDER BY date ASC
    """)
    suspend fun getUnsyncedSessions(ownerUid: String, sinceDate: String): List<SleepSession>
}
