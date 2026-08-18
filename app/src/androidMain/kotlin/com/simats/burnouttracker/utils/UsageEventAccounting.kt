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

    /**
     * `UsageEvents.Event.SCREEN_NON_INTERACTIVE` (screen turned off / locked).
     *
     * Exists to bound the "still open at window end" rule below: without it, a
     * FOREGROUND event whose matching BACKGROUND never arrives (a real OS gap —
     * e.g. locking the phone while an app is open doesn't reliably pair) keeps
     * accruing time all the way to "now", however many hours that is. One
     * missed pairing early in the day turned into a single app showing ~4x its
     * actual usage. Nothing can genuinely be in use while the screen is off, so
     * this closes every open interval here instead of at windowEnd.
     */
    const val TYPE_SCREEN_OFF = 16

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
     *    stretch is exactly the usage the clamp exists to exclude. This applies
     *    AT MOST ONCE per package per call. Real devices emit RESUMED/PAUSED
     *    per Activity instance, not one clean pair per package — an app like
     *    Chrome (Custom Tabs, multiple tabs/tasks) pauses several instances of
     *    itself in sequence, and only the first has anything open to close.
     *    Without the once-only limit, every later orphaned pause independently
     *    "discovered" the package open since windowStart and re-added the
     *    (huge) elapsed-since-window-start duration — measured on-device as a
     *    single app showing 15-30x its real total. Once a package has closed
     *    for real (whether via a matched pair or this same fallback), any
     *    further orphaned background event is multi-instance noise, not a
     *    second window-spanning session, and contributes nothing.
     *
     *  - A screen-off event closes every currently open interval at that
     *    timestamp, standing in for whatever BACKGROUND event the OS failed to
     *    emit — a locked/idle phone must not accrue foreground time for
     *    whichever app happened to be open when it was last unlocked.
     *
     *  - An interval still open at the end is closed at [windowEnd], so time
     *    that has not elapsed yet is never counted. Now bounded in practice by
     *    the rule above for anything spanning a screen-off — this only carries
     *    an open interval all the way to [windowEnd] when the screen genuinely
     *    stayed on (or no screen event was supplied at all).
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
        // Packages that have closed at least once this call, whether via a
        // matched pair, the windowStart fallback, or a screen-off. Bounds the
        // windowStart fallback to a single use per package — see the doc
        // comment above.
        val everClosed = mutableSetOf<String>()

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
                    val from = openedAt.remove(event.packageName)
                    if (from != null) {
                        add(event.packageName, from, event.timestamp)
                        everClosed.add(event.packageName)
                    } else if (everClosed.add(event.packageName)) {
                        // Set.add() returns true only the first time: this is
                        // this package's first unmatched close this call.
                        add(event.packageName, windowStart, event.timestamp)
                    }
                    // A further unmatched close is multi-instance noise —
                    // ignored rather than fabricating another windowStart-
                    // anchored session.
                }

                TYPE_SCREEN_OFF -> {
                    openedAt.forEach { (packageName, from) ->
                        add(packageName, from, event.timestamp)
                        everClosed.add(packageName)
                    }
                    openedAt.clear()
                }
            }
        }

        // Anything still open when the window closed.
        openedAt.forEach { (packageName, from) -> add(packageName, from, windowEnd) }

        return totals
    }
}
