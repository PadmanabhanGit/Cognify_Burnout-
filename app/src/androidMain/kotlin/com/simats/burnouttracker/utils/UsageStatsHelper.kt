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

        // Most recent foreground open per package, for the "Recently Opened"
        // list — independent of accumulated time, and restricted to the owned
        // spans for the same account-isolation reason as everything else here.
        val lastOpenedAt = mutableMapOf<String, Long>()
        rawEvents.forEach { event ->
            if (event.type == UsageEventAccounting.TYPE_FOREGROUND &&
                ownedSpans.any { event.timestamp >= it.start && event.timestamp < it.end }
            ) {
                val current = lastOpenedAt[event.packageName]
                if (current == null || event.timestamp > current) {
                    lastOpenedAt[event.packageName] = event.timestamp
                }
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

                val category = classifier.classify(packageName, appName)
                val color = when(category) {
                    "Social Media" -> { social += time; Color(0xFFF43F5E) }
                    "Gaming" -> { gaming += time; Color(0xFFF59E0B) }
                    "Streaming" -> { streaming += time; Color(0xFF3B82F6) }
                    "Productivity" -> { productivity += time; Color(0xFF10B981) }
                    else -> ThemeColors.textTertiary
                }
                total += time

                appList.add(DetailedAppUsage(appName, packageName, category, time, color, lastOpenedAt[packageName] ?: 0L))
            }
        }

        // Switches come from the same event list that produced the totals, so the
        // two can no longer disagree about what fell inside the window — and are
        // restricted to the owned spans for the same reason the totals are, or
        // this account would be credited with another account's app switches.
        //
        // Counts PACKAGE transitions, not raw FOREGROUND events. A single real
        // visit can emit several FOREGROUND events for the same package — proven
        // on-device with Chrome, which fires one per Activity instance (a
        // Custom Tab launch is RESUMED-PAUSED-RESUMED, two FOREGROUND events for
        // one visit). Counting those raw would inflate appSwitchCount and, since
        // averageSessionMinutes divides total time by it, silently understate
        // session length too — both are direct BurnoutPredictor inputs.
        var lastForegroundPackage: String? = null
        val switches = rawEvents
            .filter { event ->
                event.type == UsageEventAccounting.TYPE_FOREGROUND &&
                    ownedSpans.any { event.timestamp >= it.start && event.timestamp < it.end }
            }
            .sortedBy { it.timestamp }
            .count { event ->
                val isSwitch = event.packageName != lastForegroundPackage
                lastForegroundPackage = event.packageName
                isSwitch
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
            topApps = appList.sortedByDescending { it.hours }.take(10),
            recentApps = appList.filter { it.lastUsedAt > 0L }
                .sortedByDescending { it.lastUsedAt }
                .take(8)
        )
    }

    /**
     * Drains [UsageStatsManager.queryEvents] into the plain, Android-free shape
     * [UsageEventAccounting] works on.
     *
     * Foreground/background transitions and screen-off are kept; every other
     * event type (configuration changes, standby buckets, ...) carries no
     * foreground duration and would only add noise to the pairing. Screen-off
     * is not itself a duration, but [UsageEventAccounting] uses it to close any
     * session left dangling by a BACKGROUND event the OS failed to emit — see
     * its doc comment. Omitting it was letting one missed pairing accrue
     * foreground time for whatever app was open all the way to "now".
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
                event.eventType == UsageEventAccounting.TYPE_BACKGROUND ||
                event.eventType == UsageEventAccounting.TYPE_SCREEN_OFF
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
        // 1. Exact exclusions for background/shell components and the couple of
        // apps that register foreground time without a deliberate open (e.g.
        // Play Store install-progress screens). Kept as exact matches, not
        // substring, so this can't catch an unrelated app that merely
        // contains one of these names.
        val exactIgnore = setOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.keyguard",
            "com.android.vending",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller"
        )
        if (packageName in exactIgnore) return true

        // 2. Narrow pattern exclusions for shell/launcher/input components.
        // These are deliberately specific (dotted segments, prefixes) so they
        // don't accidentally swallow real apps the way a bare "android" or
        // "com.android." substring/prefix check previously did — that older
        // check was filtering out most of com.google.android.* (YouTube,
        // Maps, Gmail, Photos, Chrome, ...), which is why usage looked sparse.
        val low = packageName.lowercase()
        if (low.contains("systemui") ||
            low.contains("launcher") ||
            low.contains(".inputmethod") ||
            low.contains("wallpaper") ||
            low.startsWith("com.android.providers.") ||
            low.startsWith("com.android.server.")
        ) return true

        // 3. Everything else: only treat as "system" if it's both
        // FLAG_SYSTEM-flagged AND has no launcher entry. A preinstalled app
        // the user can actually open from the launcher (Chrome, Camera,
        // Gallery, Phone, File Manager, Calculator, an OEM-bundled
        // Facebook, ...) is real usage even though it ships as a system app;
        // a flagged package with no launcher icon (telephony framework,
        // GMS-adjacent services) is not. This generalizes across OEMs without
        // needing a hardcoded per-app allowlist.
        return try {
            val pm = context.packageManager
            val ai = pm.getApplicationInfo(packageName, 0)
            val isSystemFlagged = (ai.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            val hasLauncherEntry = pm.getLaunchIntentForPackage(packageName) != null
            isSystemFlagged && !hasLauncherEntry
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Whether last night's 10 PM-6 AM window is actually queryable for the
     * active account, i.e. [calculateNightUsage]'s clamp did not swallow the
     * whole window.
     *
     * For an account whose data_start falls after 6 AM today (freshly signed
     * in / adopted today), the clamp pushes the window's start past its end,
     * so [calculateNightUsage] always returns 0f — not because no phone use
     * happened overnight, but because there is no night in scope for this
     * account yet. Without this distinction that 0f reads as "measured, zero
     * night usage" and [InsightGenerator] turns it into a maxed-out 100%
     * Sleep Quality bar on day one, which is backwards: it is showing maximum
     * confidence for the case with the least data.
     */
    actual fun hasNightWindowData(): Boolean {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 22)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val startTime = AccountScope.clampWindowStart(context, calendar.timeInMillis)

        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 6)
        val endTime = calendar.timeInMillis

        return startTime <= endTime
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

        // Shares UsageEventAccounting's pairing with fetchDailyUsage instead of
        // hand-rolling it again here. This used to have its own copy of the
        // pairing loop with no screen-off handling, so a session left dangling
        // by a missed BACKGROUND event ran all the way to 6 AM regardless of
        // how much of the night the screen was actually off — the same
        // over-counting bug fetchDailyUsage had, independently duplicated.
        val events = readForegroundEvents(usageStatsManager, startTime, endTime)
        val nightTimeMillis = UsageEventAccounting.foregroundMillisByPackage(events, startTime, endTime)
            .values.sum()

        return nightTimeMillis / (1000f * 60f * 60f)
    }

    actual fun calculateSleepDuration(): Float {
        // Now handled by the new SleepMonitoringEngine
        // Returning a dummy or finding latest from DB if needed
        return 0f
    }
}
