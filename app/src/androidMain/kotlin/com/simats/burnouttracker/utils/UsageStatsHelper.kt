package com.simats.burnouttracker.utils

import com.simats.burnouttracker.ui.theme.ThemeColors

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

import android.app.usage.UsageStatsManager
import java.util.Calendar

@Composable
actual fun rememberUsageStatsHelper(): UsageStatsHelper {
    val context = LocalContext.current
    return remember(context) { UsageStatsHelper(context) }
}

actual class UsageStatsHelper(private val context: Context) {
    actual fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    actual fun openUsageStatsSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    actual fun fetchDailyUsage(): BurnoutFeatures {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis

        val aggregatedStats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        
        var social = 0f
        var gaming = 0f
        var streaming = 0f
        var productivity = 0f
        var total = 0f
        
        val classifier = AppUsageClassifier(context)
        val packageManager = context.packageManager

        // Filter out system apps that might skew "total screen time"
        // and only count apps with reasonable foreground time
        val appList = mutableListOf<DetailedAppUsage>()

        aggregatedStats.forEach { (packageName, usage) ->
            val time = usage.totalTimeInForeground / (1000f * 60f * 60f) // to hours
            if (time > 0.005f) { // More than 18 seconds
                
                if (isSystemPackage(packageName)) return@forEach

                val appName = try {
                    packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
                } catch (e: Exception) {
                    packageName.split(".").last()
                }
                
                val category = classifier.classify(appName)
                val color = when(category) {
                    "Social Media" -> { social += time; Color(0xFFF43F5E) }
                    "Gaming" -> { gaming += time; Color(0xFFF59E0B) }
                    "Streaming" -> { streaming += time; Color(0xFF3B82F6) }
                    "Productivity" -> { productivity += time; Color(0xFF10B981) }
                    else -> ThemeColors.textTertiary
                }
                total += time
                
                appList.add(DetailedAppUsage(appName, packageName, category, time, color))
            }
        }

        // Calculate actual switches and avg session
        var switches = 0
        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == 1) { // MOVE_TO_FOREGROUND
                switches++
            }
        }

        val avgSession = if (switches > 0) (total * 60f) / switches else 0f

        return BurnoutFeatures(
            socialHours = social,
            gamingHours = gaming,
            streamingHours = streaming,
            productivityHours = productivity,
            totalScreenTime = total,
            nightUsageHours = calculateNightUsage(usageStatsManager),
            appSwitchCount = switches,
            averageSessionMinutes = avgSession,
            topApps = appList.sortedByDescending { it.hours }.take(10)
        )
    }

    private fun isSystemPackage(packageName: String): Boolean {
        // 1. Precise exclusions for common "false positive" screen time apps
        val low = packageName.lowercase()
        val ignoreList = listOf(
            "com.android.systemui",
            "com.android.settings",
            "com.android.keyguard",
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.google.android.gms",
            "com.google.android.googlequicksearchbox",
            "com.android.providers.telephony",
            "com.android.phone",
            "com.android.vending", // Play Store usually doesn't count as "Burnout" screen time
            "android"
        )
        
        if (ignoreList.any { low.contains(it) }) return true

        // 2. Pattern based exclusions
        if (low.startsWith("com.android.") || 
            low.startsWith("android.") || 
            low.contains("systemui") ||
            low.contains("overlay") ||
            low.contains("wallpaper") ||
            low.contains("inputmethod")) return true

        // 3. Flag-based check for core system apps
        return try {
            val pm = context.packageManager
            val ai = pm.getApplicationInfo(packageName, 0)
            (ai.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: Exception) {
            false
        }
    }

    private fun calculateNightUsage(usageStatsManager: UsageStatsManager): Float {
        val calendar = Calendar.getInstance()
        
        // Window: 10 PM Yesterday to 6 AM Today
        calendar.set(Calendar.HOUR_OF_DAY, 22)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val startTime = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 6)
        val endTime = calendar.timeInMillis
        
        // Use events for precise window calculation to avoid overcounting day buckets
        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = android.app.usage.UsageEvents.Event()
        
        var nightTimeMillis = 0L
        var lastForegroundEvent = -1L
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == 1) { // MOVE_TO_FOREGROUND
                lastForegroundEvent = event.timeStamp
            } else if (event.eventType == 2 && lastForegroundEvent != -1L) { // MOVE_TO_BACKGROUND
                nightTimeMillis += (event.timeStamp - lastForegroundEvent)
                lastForegroundEvent = -1L
            }
        }
        
        // If still in foreground at 6 AM
        if (lastForegroundEvent != -1L) {
            nightTimeMillis += (endTime - lastForegroundEvent)
        }
        
        return nightTimeMillis / (1000f * 60f * 60f)
    }

    actual fun calculateSleepDuration(): Float {
        // Now handled by the new SleepMonitoringEngine
        // Returning a dummy or finding latest from DB if needed
        return 0f
    }
}
