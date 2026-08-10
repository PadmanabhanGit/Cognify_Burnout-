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

    @Query("SELECT * FROM sleep_sessions ORDER BY sleepStart DESC")
    fun getAllSessions(): Flow<List<SleepSession>>

    @Query("SELECT * FROM sleep_sessions WHERE date = :date LIMIT 1")
    suspend fun getSessionByDate(date: String): SleepSession?

    @Query("SELECT * FROM wake_events WHERE sessionId = :sessionId")
    suspend fun getWakeEventsForSession(sessionId: Long): List<WakeEvent>

    @Query("SELECT * FROM app_usage_logs WHERE sessionId = :sessionId")
    suspend fun getUsageLogsForSession(sessionId: Long): List<AppUsageLog>

    @Query("SELECT * FROM sleep_sessions ORDER BY sleepStart DESC LIMIT 7")
    fun getRecentSessions(): Flow<List<SleepSession>>

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
     */
    @Query("""
        DELETE FROM sleep_sessions
        WHERE id IN (
            SELECT d.id
            FROM sleep_sessions d
            JOIN (SELECT date AS grpDate, MIN(id) AS keepId FROM sleep_sessions GROUP BY date) g
                ON d.date = g.grpDate
            JOIN sleep_sessions k ON k.id = g.keepId
            WHERE d.id <> g.keepId
              AND d.sleepStart = k.sleepStart
              AND d.sleepEnd = k.sleepEnd
              AND d.totalSleepMinutes = k.totalSleepMinutes
              AND d.awakeningCount = k.awakeningCount
              AND d.sleepQuality = k.sleepQuality
              AND d.disturbanceScore = k.disturbanceScore
        )
    """)
    suspend fun deleteExactDuplicateSessions(): Int

    /**
     * How many rows remain beyond one-per-date. After
     * [deleteExactDuplicateSessions] runs, any non-zero result means duplicate
     * dates exist whose values DIFFER — reported, never auto-deleted.
     */
    @Query("SELECT COUNT(*) - COUNT(DISTINCT date) FROM sleep_sessions")
    suspend fun countRedundantSessionRows(): Int
}
