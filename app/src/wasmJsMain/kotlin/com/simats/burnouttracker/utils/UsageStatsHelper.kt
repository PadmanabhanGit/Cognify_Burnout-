package com.simats.burnouttracker.utils

import androidx.compose.runtime.*

@Composable
actual fun rememberUsageStatsHelper(): UsageStatsHelper {
    return remember { UsageStatsHelper() }
}

actual class UsageStatsHelper {
    actual fun hasUsageStatsPermission(): Boolean = false
    actual fun openUsageStatsSettings() {}
    actual fun fetchDailyUsage(): BurnoutFeatures = BurnoutFeatures(0f, 0f, 0f, 0f, 0f, 0f, 0, 0f)
}
