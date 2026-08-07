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
    val disturbanceScore: Int
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
