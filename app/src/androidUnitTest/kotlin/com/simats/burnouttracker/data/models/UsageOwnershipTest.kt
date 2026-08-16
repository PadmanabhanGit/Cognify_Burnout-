package com.simats.burnouttracker.data.models

import com.simats.burnouttracker.data.models.UsageOwnership.Interval
import com.simats.burnouttracker.data.models.UsageOwnership.OPEN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Usage attribution across account switches within a single day.
 *
 * The case that broke the previous single-start model, measured on device:
 * account A signed in at 12:14, B signed in at 12:19 and used the phone until
 * 12:23, and A's daily query counted all of B's foreground time as A's. A lower
 * bound can move the start of a window but cannot exclude a span in the middle
 * of one, and Android's UsageStats is device-level, so it records only that the
 * app was foregrounded — never who was signed in.
 */
class UsageOwnershipTest {

    private val accountA = "knj65AANMfXR9bomcOiPBz1ozc53"
    private val accountB = "2wZjgkBr4bhIyMwm7ifSPiAXIsN2"

    // The real timeline from the device.
    private val t1214 = 1_786_862_640_000L
    private val t1219 = 1_786_862_968_000L
    private val t1223 = 1_786_863_180_000L
    private val t1230 = 1_786_863_600_000L
    private val midnight = 1_786_818_600_000L

    // ── the regression ───────────────────────────────────────────────────────

    @Test
    fun `A does not own the span during which B was signed in`() {
        // A signs in, logs out when B takes over, and signs back in later.
        var a = UsageOwnership.openSession(emptyList(), t1214)
        a = UsageOwnership.closeSession(a, t1219)
        a = UsageOwnership.openSession(a, t1223)

        val owned = UsageOwnership.clip(UsageOwnership.resolve(a, t1230), midnight, t1230)

        assertEquals(listOf(Interval(t1214, t1219), Interval(t1223, t1230)), owned)
        // The 12:19–12:23 span is absent, which is the whole point.
        assertFalse(owned.any { it.start <= t1219 && it.end >= t1223 })
    }

    @Test
    fun `B owns only its own span`() {
        var b = UsageOwnership.openSession(emptyList(), t1219)
        b = UsageOwnership.closeSession(b, t1223)

        val owned = UsageOwnership.clip(UsageOwnership.resolve(b, t1230), midnight, t1230)

        assertEquals(listOf(Interval(t1219, t1223)), owned)
        assertEquals(t1223 - t1219, UsageOwnership.totalMillis(owned))
    }

    @Test
    fun `the two accounts' owned spans never overlap`() {
        val a = UsageOwnership.resolve(
            UsageOwnership.openSession(
                UsageOwnership.closeSession(UsageOwnership.openSession(emptyList(), t1214), t1219),
                t1223
            ),
            t1230
        )
        val b = UsageOwnership.resolve(
            UsageOwnership.closeSession(UsageOwnership.openSession(emptyList(), t1219), t1223), t1230
        )

        for (x in a) for (y in b) {
            assertTrue("overlap between $x and $y", x.end <= y.start || y.end <= x.start)
        }
    }

    // ── open intervals ───────────────────────────────────────────────────────

    @Test
    fun `an open interval accrues only up to the moment of reading`() {
        val a = UsageOwnership.openSession(emptyList(), t1214)

        assertEquals(listOf(Interval(t1214, t1219)), UsageOwnership.resolve(a, t1219))
        assertEquals(listOf(Interval(t1214, t1230)), UsageOwnership.resolve(a, t1230))
        // Resolving does not mutate the stored record.
        assertEquals(OPEN, a.single().end)
    }

    @Test
    fun `a crash leaves an interval open and the next session closes it`() {
        // No clean logout: the process died while the interval was open.
        val stranded = UsageOwnership.openSession(emptyList(), t1214)

        val next = UsageOwnership.openSession(stranded, t1223)

        // Capped at the last moment we can vouch for, not left running forever.
        assertEquals(Interval(t1214, t1223), next.first())
        assertEquals(OPEN, next.last().end)
        assertEquals(2, next.size)
    }

    @Test
    fun `a genuine gap still produces a separate span`() {
        var a = UsageOwnership.openSession(emptyList(), t1214)
        a = UsageOwnership.closeSession(a, t1219)
        a = UsageOwnership.openSession(a, t1223)   // gap: 12:19 → 12:23

        assertEquals(listOf(Interval(t1214, t1219), Interval(t1223, OPEN)), a)
    }

    @Test
    fun `closing when nothing is open changes nothing`() {
        val closed = UsageOwnership.closeSession(UsageOwnership.openSession(emptyList(), t1214), t1219)

        assertEquals(closed, UsageOwnership.closeSession(closed, t1230))
    }

    // ── same-day re-login ────────────────────────────────────────────────────

    @Test
    fun `logging out and back in the same day adds a span, never rewrites the first`() {
        var a = UsageOwnership.openSession(emptyList(), t1214)
        a = UsageOwnership.closeSession(a, t1219)
        a = UsageOwnership.openSession(a, t1223)
        a = UsageOwnership.closeSession(a, t1230)

        assertEquals(listOf(Interval(t1214, t1219), Interval(t1223, t1230)), a)
        // Earlier usage cannot be erased by cycling the session.
        assertEquals((t1219 - t1214) + (t1230 - t1223), UsageOwnership.totalMillis(a))
    }

    // ── reconciliation without a clean logout ────────────────────────────────
    //
    // AccountScope.reconcileOpenIntervals wires these: a new session closes every
    // OTHER open span before claiming time. Composed here because the composition
    // is the rule — closing at the right boundary is what stops one account
    // swallowing another's time.

    @Test
    fun `A switching to B without logout does not let A swallow B's time`() {
        // A signs in and is never cleanly logged out — killed process, crash.
        val aOpen = UsageOwnership.openSession(emptyList(), t1214)

        // B's session begins: reconciliation closes A at that instant.
        val aClosed = UsageOwnership.closeSession(aOpen, t1219)
        val b = UsageOwnership.closeSession(UsageOwnership.openSession(emptyList(), t1219), t1223)

        // Read much later. Without reconciliation A would resolve to [12:14→now]
        // and contain the whole of B's stretch.
        val aOwned = UsageOwnership.resolve(aClosed, t1230)

        assertEquals(listOf(Interval(t1214, t1219)), aOwned)
        for (x in aOwned) for (y in b) {
            assertTrue("overlap $x / $y", x.end <= y.start || y.end <= x.start)
        }
    }

    @Test
    fun `an unreconciled open span would have swallowed the other account`() {
        // Documents the defect this reconciliation prevents, so a regression here
        // fails loudly rather than quietly re-inflating one account's totals.
        val aNeverClosed = UsageOwnership.openSession(emptyList(), t1214)

        val wrong = UsageOwnership.resolve(aNeverClosed, t1230)

        assertEquals(listOf(Interval(t1214, t1230)), wrong)
        assertTrue("would contain B's span", wrong.single().start < t1219 && wrong.single().end > t1223)
    }

    // ── midnight ─────────────────────────────────────────────────────────────

    @Test
    fun `a span open across midnight closes at the end of its own day`() {
        val yesterday2350 = t1214 - 12 * 3_600_000L - 24 * 60_000L
        val endOfYesterday = 1_786_818_599_999L   // 23:59:59.999 local

        val open = UsageOwnership.openSession(emptyList(), yesterday2350)
        val closed = UsageOwnership.closeSession(open, endOfYesterday)

        assertEquals(endOfYesterday, closed.single().end)
        // Critically it does NOT run on into today, which is what closing it at
        // `now` (after midnight) would have produced.
        assertTrue(closed.single().end < t1214)
    }

    @Test
    fun `each day's total stays inside that day`() {
        val endOfYesterday = 1_786_818_599_999L
        val yesterday2350 = endOfYesterday - 9 * 60_000L - 59_999L

        val yesterday = UsageOwnership.closeSession(
            UsageOwnership.openSession(emptyList(), yesterday2350), endOfYesterday
        )
        val today = UsageOwnership.closeSession(
            UsageOwnership.openSession(emptyList(), t1214), t1219
        )

        // One ms short of ten minutes: the day ends at 23:59:59.999, so a span
        // closed at that boundary can never quite reach the following midnight.
        assertEquals(10 * 60_000L - 1, UsageOwnership.totalMillis(yesterday))
        assertEquals(t1219 - t1214, UsageOwnership.totalMillis(today))
        assertTrue(yesterday.last().end < today.first().start)
    }

    // ── clipping against the lifetime clamp ──────────────────────────────────

    @Test
    fun `spans are clipped to the account-lifetime bound`() {
        val a = UsageOwnership.closeSession(UsageOwnership.openSession(emptyList(), t1214), t1230)

        // data_start later than the span start truncates it rather than widening.
        assertEquals(listOf(Interval(t1219, t1230)), UsageOwnership.clip(a, t1219, t1230))
        // A window ending early truncates the other end.
        assertEquals(listOf(Interval(t1214, t1219)), UsageOwnership.clip(a, midnight, t1219))
        // No overlap at all yields nothing, not the raw window.
        assertTrue(UsageOwnership.clip(a, t1230, t1230 + 1000).isEmpty())
    }

    @Test
    fun `an account with no intervals owns nothing`() {
        assertTrue(UsageOwnership.clip(emptyList(), midnight, t1230).isEmpty())
        assertTrue(UsageOwnership.resolve(emptyList(), t1230).isEmpty())
    }

    // ── persistence ──────────────────────────────────────────────────────────

    @Test
    fun `intervals survive a round trip through storage`() {
        var a = UsageOwnership.openSession(emptyList(), t1214)
        a = UsageOwnership.closeSession(a, t1219)
        a = UsageOwnership.openSession(a, t1223)

        assertEquals(a, UsageOwnership.parse(UsageOwnership.encode(a)))
    }

    @Test
    fun `corrupt storage degrades to owning nothing rather than inheriting time`() {
        assertTrue(UsageOwnership.parse("garbage").isEmpty())
        assertTrue(UsageOwnership.parse("").isEmpty())
        assertTrue(UsageOwnership.parse(null).isEmpty())
        // A partially valid record keeps only what parses.
        assertEquals(listOf(Interval(t1214, t1219)), UsageOwnership.parse("$t1214:$t1219,junk"))
    }

    @Test
    fun `keys are scoped per account and per day`() {
        assertTrue(UsageOwnership.key(accountA, "2026-08-16") != UsageOwnership.key(accountB, "2026-08-16"))
        assertTrue(UsageOwnership.key(accountA, "2026-08-15") < UsageOwnership.key(accountA, "2026-08-16"))
        assertTrue(UsageOwnership.key(accountA, "2026-08-16").endsWith("2026-08-16"))
    }

    @Test
    fun `pruning keeps recent days and drops stale ones`() {
        assertTrue(UsageOwnership.shouldRetain("2026-08-16", "2026-06-17"))
        assertTrue(UsageOwnership.shouldRetain("2026-06-17", "2026-06-17"))
        assertFalse(UsageOwnership.shouldRetain("2026-06-16", "2026-06-17"))
    }
}
