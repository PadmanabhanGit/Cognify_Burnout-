package com.simats.burnouttracker.utils

/**
 * Per-package foreground time, computed from UsageStats EVENTS rather than
 * daily buckets.
 *
 * WHY THIS EXISTS
 * [UsageStatsHelper.fetchDailyUsage] previously used
 * `queryAndAggregateUsageStats(startTime, endTime)`. That API is bucket-based:
 * it returns the daily buckets that OVERLAP the requested range, each carrying
 * its whole-bucket `totalTimeInForeground`. Moving `startTime` forward does not
 * shrink those totals — sub-day granularity is not available from it at all. So
 * the account-isolation clamp was being passed in and silently ignored, and a
 * brand-new account still saw the whole device's daily usage as its own.
 *
 * Events do not have that problem: each one is a timestamped transition, so a
 * window bound actually bounds the result. The same technique was already in
 * use elsewhere in this codebase (`calculateNightUsage`, and
 * SleepMonitoringEngine's night detection) — this brings the daily totals onto
 * it too, leaving no bucket-based query anywhere in the app.
 *
 * Deliberately free of Android types so it can be unit-tested on the JVM
 * without an emulator or Robolectric. [UsageStatsHelper] adapts
 * `UsageEvents.Event` into [ForegroundEvent] at the call site.
 */
internal object UsageEventAccounting {

    /** `UsageEvents.Event.MOVE_TO_FOREGROUND` / `ACTIVITY_RESUMED`. */
    const val TYPE_FOREGROUND = 1

    /** `UsageEvents.Event.MOVE_TO_BACKGROUND` / `ACTIVITY_PAUSED`. */
    const val TYPE_BACKGROUND = 2

    data class ForegroundEvent(
        val packageName: String,
        val type: Int,
        val timestamp: Long
    )

    /**
     * Foreground milliseconds per package within `[windowStart, windowEnd]`.
     *
     * Pairing rules, and what each one is protecting against:
     *
     *  - A foreground event opens an interval for its package. A second one
     *    with no intervening background keeps the FIRST timestamp, so a
     *    duplicate resume cannot restart the clock and shorten the interval.
     *
     *  - A background event with no matching open interval means the app was
     *    already in the foreground when the window began. Its time is counted
     *    from [windowStart], never from when it actually opened — that earlier
     *    stretch is exactly the usage the clamp exists to exclude.
     *
     *  - An interval still open at the end is closed at [windowEnd], so time
     *    that has not elapsed yet is never counted.
     *
     * Known and accepted: an app foregrounded BEFORE [windowStart] and still
     * foreground at [windowEnd] emits no event inside the window at all, so it
     * contributes nothing. That under-reports rather than over-reports, which is
     * the right direction to err for isolation — recovering it would mean
     * querying before the clamp, which is the leak this replaced.
     *
     * Returns an empty map when the window is empty or inverted, so a clamp at
     * or after "now" yields no usage rather than a negative total.
     */
    fun foregroundMillisByPackage(
        events: List<ForegroundEvent>,
        windowStart: Long,
        windowEnd: Long
    ): Map<String, Long> {
        if (windowEnd <= windowStart) return emptyMap()

        val totals = mutableMapOf<String, Long>()
        val openedAt = mutableMapOf<String, Long>()

        fun add(packageName: String, from: Long, to: Long) {
            val bounded = minOf(to, windowEnd) - maxOf(from, windowStart)
            if (bounded > 0) totals[packageName] = (totals[packageName] ?: 0L) + bounded
        }

        for (event in events.sortedBy { it.timestamp }) {
            when (event.type) {
                TYPE_FOREGROUND ->
                    // putIfAbsent semantics: keep the earliest open.
                    if (!openedAt.containsKey(event.packageName)) {
                        openedAt[event.packageName] = event.timestamp
                    }

                TYPE_BACKGROUND -> {
                    val from = openedAt.remove(event.packageName) ?: windowStart
                    add(event.packageName, from, event.timestamp)
                }
            }
        }

        // Anything still open when the window closed.
        openedAt.forEach { (packageName, from) -> add(packageName, from, windowEnd) }

        return totals
    }
}
