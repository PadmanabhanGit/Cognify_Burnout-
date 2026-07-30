package com.simats.burnouttracker.utils

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

data class SleepLog(
    val date: String,
    val hours: Float,
    val moodEmoji: String,
    val status: String,
    val statusColor: Color
)

object AppData {
    var currentFeatures by mutableStateOf(
        BurnoutFeatures(0f, 0f, 0f, 0f, 0f, 0f, 0, 0f)
    )

    var predictedScore by mutableStateOf(0f)
    var lastSleepLogged by mutableStateOf(0f)
    var lastMoodLogged by mutableStateOf("Neutral")
    
    // Productivity Data
    var productivityScore by mutableStateOf(78)
    var peakFocusHours by mutableStateOf(4.2f)
    var goalHitRate by mutableStateOf(92)
    var averageStartTime by mutableStateOf("09:00")
    var userGlobalRanking by mutableStateOf("Top 5%")

    // Study Data
    var studyTodayHours by mutableStateOf(0f)
    var studyWeekHours by mutableStateOf(0f)
    var studyMonthHours by mutableStateOf(0f)
    val weeklyStudyData = mutableStateListOf(0f, 0f, 0f, 0f, 0f, 0f, 0f)
    val studyBreakdown = mutableStateMapOf<String, Float>()
    val monthlyStudyTrend = mutableStateListOf(0.4f, 0.7f, 0.55f, 0.85f) // Normalized 0-1

    var activeSessionName by mutableStateOf<String?>(null)
    var sessionStartTime by mutableStateOf<Long?>(null)

    val sleepLogs = mutableStateListOf<SleepLog>()
    
    // Last 7 days points (0.0 to 1.0 normalized)
    val sleepTrendPoints = mutableStateListOf<Float>()
    val moodTrendPoints = mutableStateListOf<Float>()

    var hasData by mutableStateOf(false)
    var lastUpdatedTime by mutableStateOf("")
    var isSyncing by mutableStateOf(false)
    var lastSyncFailed by mutableStateOf(false)
}

@Composable
expect fun rememberUsageStatsHelper(): UsageStatsHelper

expect class UsageStatsHelper {
    fun hasUsageStatsPermission(): Boolean
    fun openUsageStatsSettings()
    fun fetchDailyUsage(): BurnoutFeatures
    fun calculateSleepDuration(): Float
}
