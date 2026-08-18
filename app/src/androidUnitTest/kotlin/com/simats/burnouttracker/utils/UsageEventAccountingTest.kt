package com.simats.burnouttracker.utils

import com.simats.burnouttracker.utils.UsageEventAccounting.ForegroundEvent
import com.simats.burnouttracker.utils.UsageEventAccounting.TYPE_BACKGROUND
import com.simats.burnouttracker.utils.UsageEventAccounting.TYPE_FOREGROUND
import com.simats.burnouttracker.utils.UsageEventAccounting.TYPE_SCREEN_OFF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the account-isolation clamp on app usage.
 *
 * The bug these exist to prevent: fetchDailyUsage() used
 * `queryAndAggregateUsageStats`, which returns whole daily buckets whose totals
 * ignore the query window. A newly adopted account with a data_start of "now"
 * was therefore shown the entire device's daily usage as its own. Nothing
 * failed loudly — the clamp was passed in and silently discarded — which is
 * exactly the kind of defect that needs a test rather than a code review.
 */
class UsageEventAccountingTest {

    private val app = "com.example.social"
    private val other = "com.example.game"

    private fun fg(pkg: String, at: Long) = ForegroundEvent(pkg, TYPE_FOREGROUND, at)
    private fun bg(pkg: String, at: Long) = ForegroundEvent(pkg, TYPE_BACKGROUND, at)
    private fun screenOff(at: Long) = ForegroundEvent("android", TYPE_SCREEN_OFF, at)

    private val minute = 60_000L
    private val hour = 60 * minute

    /**
     * THE regression test: a clamp at "now" must yield no usage.
     *
     * Six hours of real device activity sit before the clamp — the previous
     * account's day. A freshly adopted account whose data_start is the current
     * time must see none of it.
     */
    @Test
    fun `usage before the clamp does not count toward a newly adopted account`() {
        val dayStart = 0L
        val clamp = 6 * hour            // account adopted six hours into the day
        // Ten minutes of real elapsed time after adoption, so the window is
        // genuinely non-empty and the assertion below exercises the clamping
        // arithmetic rather than the empty-window short circuit.
        val now = clamp + 10 * minute

        val events = listOf(
            fg(app, dayStart), bg(app, dayStart + 2 * hour),
            fg(other, dayStart + 3 * hour), bg(other, dayStart + 5 * hour)
        )

        val totals = UsageEventAccounting.foregroundMillisByPackage(events, clamp, now)

        assertTrue("expected no usage attributed after the clamp, got $totals", totals.isEmpty())
    }

    /**
     * The same events WITHOUT a clamp still report the full six hours, so the
     * test above is proving isolation rather than a broken calculation.
     */
    @Test
    fun `the same events are fully counted when the account has no clamp`() {
        val events = listOf(
            fg(app, 0L), bg(app, 2 * hour),
            fg(other, 3 * hour), bg(other, 5 * hour)
        )

        val totals = UsageEventAccounting.foregroundMillisByPackage(events, 0L, 6 * hour)

        assertEquals(2 * hour, totals[app])
        assertEquals(2 * hour, totals[other])
    }

    /** Only the portion of a session after the clamp belongs to the new account. */
    @Test
    fun `a session straddling the clamp is counted only from the clamp onward`() {
        val clamp = 1 * hour
        // Opened before the account existed, closed 30 minutes after it did.
        val events = listOf(fg(app, 0L), bg(app, clamp + 30 * minute))

        val totals = UsageEventAccounting.foregroundMillisByPackage(events, clamp, 3 * hour)

        assertEquals(30 * minute, totals[app])
    }

    /**
     * An app already foreground when the window opened emits only a background
     * event. It is counted from the window start — not from whenever it was
     * actually opened, which is the usage the clamp exists to exclude.
     */
    @Test
    fun `an unmatched background event is counted from the window start`() {
        val events = listOf(bg(app, 90 * minute))

        val totals = UsageEventAccounting.foregroundMillisByPackage(events, 1 * hour, 3 * hour)

        assertEquals(30 * minute, totals[app])
    }

    /** Time that has not elapsed yet is never counted. */
    @Test
    fun `a session still open at the window end is closed at the window end`() {
        val events = listOf(fg(app, 1 * hour))

        val totals = UsageEventAccounting.foregroundMillisByPackage(events, 0L, 2 * hour)

        assertEquals(1 * hour, totals[app])
    }

    /** A duplicate resume must not restart the clock and shorten the interval. */
    @Test
    fun `a repeated foreground event keeps the earliest open`() {
        val events = listOf(fg(app, 0L), fg(app, 30 * minute), bg(app, 1 * hour))

        val totals = UsageEventAccounting.foregroundMillisByPackage(events, 0L, 2 * hour)

        assertEquals(1 * hour, totals[app])
    }

    /** Interleaved apps are attributed independently. */
    @Test
    fun `interleaved packages are tracked separately`() {
        val events = listOf(
            fg(app, 0L),
            fg(other, 10 * minute),
            bg(app, 20 * minute),
            bg(other, 40 * minute)
        )

        val totals = UsageEventAccounting.foregroundMillisByPackage(events, 0L, 1 * hour)

        assertEquals(20 * minute, totals[app])
        assertEquals(30 * minute, totals[other])
    }

    /** Events arriving out of order must not produce negative or lost time. */
    @Test
    fun `out of order events are sorted before pairing`() {
        val events = listOf(bg(app, 1 * hour), fg(app, 0L))

        val totals = UsageEventAccounting.foregroundMillisByPackage(events, 0L, 2 * hour)

        assertEquals(1 * hour, totals[app])
    }

    /**
     * THE regression test for the ~4x overcount bug: a FOREGROUND event whose
     * matching BACKGROUND never arrives (a real OS gap, e.g. locking the phone)
     * must stop accruing time at the screen-off event, not run all the way to
     * windowEnd — otherwise an app opened once in the morning reports being
     * "in use" through an entire screen-off afternoon.
     */
    @Test
    fun `a dangling session is closed at screen-off, not window end`() {
        val events = listOf(fg(app, 0L), screenOff(30 * minute))

        val totals = UsageEventAccounting.foregroundMillisByPackage(events, 0L, 8 * hour)

        assertEquals(30 * minute, totals[app])
    }

    /** A fresh foreground after screen-on starts its own interval, unaffected by the earlier screen-off. */
    @Test
    fun `usage resumes normally after a screen-off closes the previous session`() {
        val events = listOf(
            fg(app, 0L), screenOff(30 * minute),
            fg(app, 2 * hour), bg(app, 2 * hour + 15 * minute)
        )

        val totals = UsageEventAccounting.foregroundMillisByPackage(events, 0L, 8 * hour)

        assertEquals(45 * minute, totals[app])
    }

    /**
     * THE regression test for the 15-30x overcount measured on-device: a
     * package with multiple Activity instances (Chrome's Custom Tabs, tabs,
     * multi-window) pauses more than once while only ONE thing was ever
     * tracked as open. Each extra unmatched BACKGROUND must not independently
     * "discover" the package open since windowStart — that fallback is for
     * the app being open when the window began, and applies at most once.
     */
    @Test
    fun `a second unmatched background event for the same package is not counted`() {
        // Resume/pause of one activity instance, immediately followed by
        // resume/pause of a second instance of the SAME package, mirroring
        // com.android.chrome's real event log (ChromeLauncherActivity then
        // CustomTabActivity, both under com.android.chrome).
        val events = listOf(
            fg(app, 10 * minute), bg(app, 10 * minute + 5_000),
            fg(app, 10 * minute + 5_000), bg(app, 10 * minute + 8_000)
        )

        val totals = UsageEventAccounting.foregroundMillisByPackage(events, 0L, 8 * hour)

        // ~8 real seconds of usage, not ~10 minutes-plus fabricated from windowStart.
        assertEquals(8_000L, totals[app])
    }

    /**
     * Three or more orphaned closes in a row (observed on-device across many
     * Custom Tab opens in one session) must not each add their own
     * windowStart-anchored chunk.
     */
    @Test
    fun `many unmatched background events in a row only count the first`() {
        val events = listOf(
            bg(app, 1 * hour), bg(app, 2 * hour), bg(app, 3 * hour), bg(app, 4 * hour)
        )

        val totals = UsageEventAccounting.foregroundMillisByPackage(events, 0L, 8 * hour)

        // Only the first is credited (window start -> its timestamp).
        assertEquals(1 * hour, totals[app])
    }

    /** A clamp at or beyond "now" is an empty window, not a negative total. */
    @Test
    fun `an inverted or empty window yields nothing`() {
        val events = listOf(fg(app, 0L), bg(app, 1 * hour))

        assertTrue(UsageEventAccounting.foregroundMillisByPackage(events, 2 * hour, 2 * hour).isEmpty())
        assertTrue(UsageEventAccounting.foregroundMillisByPackage(events, 3 * hour, 2 * hour).isEmpty())
    }
}
