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
    /** Whether last night's window is in scope for the active account yet. See [UsageStatsHelper.hasNightWindowData]. */
    var nightDataAvailable by mutableStateOf(false)
    var lastSleepLogged by mutableStateOf(0f)
    var lastMoodLogged by mutableStateOf("Neutral")
    
    // User Profile
    var userFullName by mutableStateOf<String?>(null)
    
    // Preferences
    var isDarkMode by mutableStateOf(false)
    var allowAllNotif by mutableStateOf(true)
    var burnoutAlerts by mutableStateOf(true)
    var dailyReminders by mutableStateOf(true)
    var weeklyReports by mutableStateOf(false)
    var studyPrompts by mutableStateOf(true)

    // Productivity Data
    var productivityScore by mutableStateOf(0)
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
    // Normalized 0-1, index 3 = current week. Only index 3 is ever written (from
    // real backend week totals); the backend exposes no multi-week study history,
    // so indices 0-2 have no source and stay 0. Size is fixed at 4 because
    // StudyTrackerScreen writes index 3 directly.
    // Previously seeded with 0.4/0.7/0.55/0.85, which drew a fabricated trend line.
    val monthlyStudyTrend = mutableStateListOf(0f, 0f, 0f, 0f)

    var activeSessionName by mutableStateOf<String?>(null)
    var activeSessionId by mutableStateOf<String?>(null)
    var sessionStartTime by mutableStateOf<Long?>(null)

    val sleepLogs = mutableStateListOf<SleepLog>()
    
    // Last 7 days points (0.0 to 1.0 normalized)
    val sleepTrendPoints = mutableStateListOf<Float>()
    val moodTrendPoints = mutableStateListOf<Float>()

    var hasData by mutableStateOf(false)
    var lastUpdatedTime by mutableStateOf("")
    var isSyncing by mutableStateOf(false)
    var lastSyncFailed by mutableStateOf(false)
    var lastSyncError by mutableStateOf("")

    /** Call this on logout to wipe every user-scoped field before the next
     *  user logs in. Device preferences (dark mode, notification toggles)
     *  are intentionally left untouched — they are not user data. */
    fun reset() {
        currentFeatures = BurnoutFeatures(0f, 0f, 0f, 0f, 0f, 0f, 0, 0f)
        predictedScore = 0f
        nightDataAvailable = false
        lastSleepLogged = 0f
        lastMoodLogged = "Neutral"
        userFullName = null
        productivityScore = 0
        peakFocusHours = 4.2f
        goalHitRate = 92
        averageStartTime = "09:00"
        userGlobalRanking = "Top 5%"
        studyTodayHours = 0f
        studyWeekHours = 0f
        studyMonthHours = 0f
        weeklyStudyData.replaceAll { 0f }
        studyBreakdown.clear()
        monthlyStudyTrend.replaceAll { 0f }
        activeSessionName = null
        activeSessionId = null
        sessionStartTime = null
        sleepLogs.clear()
        sleepTrendPoints.clear()
        moodTrendPoints.clear()
        hasData = false
        lastUpdatedTime = ""
        isSyncing = false
        lastSyncFailed = false
        lastSyncError = ""
    }
}

@Composable
expect fun rememberUsageStatsHelper(): UsageStatsHelper

expect class UsageStatsHelper {
    fun hasUsageStatsPermission(): Boolean
    fun openUsageStatsSettings()
    fun fetchDailyUsage(): BurnoutFeatures
    fun calculateSleepDuration(): Float
    /** See the actual implementation's doc comment: distinguishes "no night observed yet" from a measured zero. */
    fun hasNightWindowData(): Boolean
}
