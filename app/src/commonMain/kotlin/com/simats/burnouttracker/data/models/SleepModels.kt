package com.simats.burnouttracker.data.models

import androidx.compose.ui.graphics.Color

data class SleepSessionData(
    val id: Long = 0,
    val date: String,
    val sleepStart: Long,
    val sleepEnd: Long,
    val totalSleepMinutes: Int,
    val awakeningCount: Int,
    val sleepQuality: Int,
    val disturbanceScore: Int,
    /**
     * Where this night stands with the backend.
     *
     * Carried to the UI layer so a night that has not reached the cloud can be
     * shown as such instead of looking identical to one that has. Defaulted so
     * every existing construction site keeps compiling unchanged.
     */
    val syncState: SyncState = SyncState.PENDING,
    /** Why the last upload attempt failed, or null. See [SyncStatus.lastSyncError]. */
    val lastSyncError: String? = null
)

data class WakeEventData(
    val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val duration: Long,
    val appName: String,
    val packageName: String,
    val category: String
)

data class AppUsageLogData(
    val id: Long = 0,
    val sessionId: Long,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val appName: String,
    val packageName: String,
    val category: String
)
