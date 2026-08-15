package com.simats.burnouttracker.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT * FROM wake_events WHERE sessionId = :sessionId")
    suspend fun getWakeEventsForSession(sessionId: Long): List<WakeEvent>

    @Query("SELECT * FROM app_usage_logs WHERE sessionId = :sessionId")
    suspend fun getUsageLogsForSession(sessionId: Long): List<AppUsageLog>

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
     * Records that the backend has accepted this night, so it is not re-sent.
     *
     * Written only after a POST that actually returned success — a transport
     * failure must leave [SleepSession.syncedAt] at 0, because the point of the
     * column is to remember what did NOT get through.
     */
    @Query("UPDATE sleep_sessions SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSessionSynced(id: Long, syncedAt: Long)

    /**
     * Nights this account holds locally that the backend has never confirmed,
     * limited to those on or after [sinceDate] (a `yyyy-MM-dd` label).
     *
     * Bounded on purpose. Every unsynced night in history would otherwise be
     * re-sent on the first refresh after upgrading, which for a long-installed
     * device is a burst of writes to fix nights nobody is looking at. The window
     * matches the one refreshSleepData() already re-scans, so retries stay
     * proportional to the work that pass was already doing.
     *
     * Oldest first, so a partial success still advances chronologically.
     */
    @Query("""
        SELECT * FROM sleep_sessions
        WHERE ownerUid = :ownerUid AND syncedAt = 0 AND date >= :sinceDate
        ORDER BY date ASC
    """)
    suspend fun getUnsyncedSessions(ownerUid: String, sinceDate: String): List<SleepSession>
}
