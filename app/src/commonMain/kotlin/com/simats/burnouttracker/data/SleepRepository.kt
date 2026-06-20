package com.simats.burnouttracker.data

import androidx.compose.runtime.Composable
import com.simats.burnouttracker.data.models.*
import kotlinx.coroutines.flow.Flow

interface SleepRepository {
    fun getRecentSessions(): Flow<List<SleepSessionData>>
    suspend fun getSessionDetails(sessionId: Long): SleepSessionData?
    suspend fun getWakeEvents(sessionId: Long): List<WakeEventData>
    suspend fun getUsageLogs(sessionId: Long): List<AppUsageLogData>
    suspend fun refreshSleepData()
}

@Composable
expect fun rememberSleepRepository(): SleepRepository

expect fun getSleepRepository(): SleepRepository
