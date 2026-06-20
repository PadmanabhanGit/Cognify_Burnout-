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
}
