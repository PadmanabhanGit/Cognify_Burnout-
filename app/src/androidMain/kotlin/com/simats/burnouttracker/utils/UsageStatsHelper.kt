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
        // Account isolation, lower bound: never read usage recorded before the
        // active account existed on this device. The OS UsageStats database
        // itself is untouched — only this query's bounds move. For
        // migrated/existing accounts the clamp is 0, so the window starts at
        // midnight as before.
        val startTime = AccountScope.clampWindowStart(context, calendar.timeInMillis)

        // Account isolation, INTERIOR: the spans of today this account was
        // actually signed in for.
        //
        // A lower bound alone is not sufficient and was measured to be wrong on
        // device: UsageStats is device-level, so with a single bound an account
        // that signed in at 12:14 counted everything a second account did after
        // signing in at 12:19. Time is attributed only to the account that was
        // signed in while it elapsed; time with nobody signed in belongs to
        // nobody. See [UsageOwnership].
        val ownedSpans = AccountScope.usageIntervals(context, startTime, endTime)

        // Event-based, NOT queryAndAggregateUsageStats(). That API returns whole
        // daily buckets whose totals ignore the window bounds entirely, so the
        // clamp above had no effect on the numbers below and a new account saw
        // the device's full daily usage as its own. See [UsageEventAccounting].
        //
        // One query spanning all owned spans, then accounted per span: the
        // pairing logic needs the events, and slicing it per span is what keeps
        // another account's foreground time out of this account's totals.
        val rawEvents = if (ownedSpans.isEmpty()) emptyList()
        else readForegroundEvents(usageStatsManager, ownedSpans.first().start, endTime)

        val foregroundMillis = mutableMapOf<String, Long>()
        ownedSpans.forEach { span ->
            UsageEventAccounting.foregroundMillisByPackage(rawEvents, span.start, span.end)
                .forEach { (pkg, millis) ->
                    foregroundMillis[pkg] = (foregroundMillis[pkg] ?: 0L) + millis
                }
        }

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

        foregroundMillis.forEach { (packageName, millis) ->
            val time = millis / (1000f * 60f * 60f) // to hours
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

        // Switches come from the same event list that produced the totals, so the
        // two can no longer disagree about what fell inside the window — and are
        // restricted to the owned spans for the same reason the totals are, or
        // this account would be credited with another account's app switches.
        val switches = rawEvents.count { event ->
            event.type == UsageEventAccounting.TYPE_FOREGROUND &&
                ownedSpans.any { event.timestamp >= it.start && event.timestamp < it.end }
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

    /**
     * Drains [UsageStatsManager.queryEvents] into the plain, Android-free shape
     * [UsageEventAccounting] works on.
     *
     * Only foreground/background transitions are kept — every other event type
     * (screen on/off, configuration changes, standby buckets) carries no
     * foreground duration and would only add noise to the pairing.
     */
    private fun readForegroundEvents(
        usageStatsManager: UsageStatsManager,
        startTime: Long,
        endTime: Long
    ): List<UsageEventAccounting.ForegroundEvent> {
        if (endTime <= startTime) return emptyList()

        val collected = mutableListOf<UsageEventAccounting.ForegroundEvent>()
        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEventAccounting.TYPE_FOREGROUND ||
                event.eventType == UsageEventAccounting.TYPE_BACKGROUND
            ) {
                collected.add(
                    UsageEventAccounting.ForegroundEvent(
                        packageName = event.packageName,
                        type = event.eventType,
                        timestamp = event.timeStamp
                    )
                )
            }
        }
        return collected
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
        // Same account-isolation clamp as fetchDailyUsage above. Window shape and
        // the 22:00–06:00 hours are unchanged.
        val startTime = AccountScope.clampWindowStart(context, calendar.timeInMillis)

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
