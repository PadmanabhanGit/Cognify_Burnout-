package com.simats.burnouttracker.utils

import com.simats.burnouttracker.data.models.RemoteSleepLog
import com.simats.burnouttracker.utils.SleepRestorePlanner.Decision
import com.simats.burnouttracker.utils.SleepRestorePlanner.LocalRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for cloud restore ownership.
 *
 * The failure these exist to prevent actually happened on 2026-08-16: restore
 * imported five nights into yninja004 that were byte-identical to nights already
 * held by padmanabhancba on the same device. The nights themselves came from one
 * device-level UsageStats database, so identical values are expected and are NOT
 * evidence of shared ownership — which is exactly why deduplication must key on
 * `(ownerUid, date)` and never on the sleep values.
 */
class SleepRestorePlannerTest {

    private val padmanabhancba = "2wZjgkBr4bhIyMwm7ifSPiAXIsN2"
    private val yninja004 = "knj65AANMfXR9bomcOiPBz1ozc53"

    /** The real Aug 12 night, as it exists in both accounts' records. */
    private fun aug12(
        date: String = "2026-08-12",
        start: Long = 1786476878261,
        end: Long = 1786494637883
    ) = RemoteSleepLog(
        id = "doc",
        date = date,
        sleepDuration = 4.916666666666667, // 295 minutes
        sleepQuality = 75,
        sleepStart = start,
        sleepEnd = end,
        awakeningCount = 0,
        disturbanceScore = 25,
        source = "automatic"
    )

    private fun inserts(decisions: List<Decision>) =
        decisions.filterIsInstance<Decision.Insert>()

    // ── Test 1 ───────────────────────────────────────────────────────────────
    // Another account already holds the date. Its row must survive untouched,
    // and the restoring account still gets its own row.

    @Test
    fun `restore never reassigns or mutates another account's row`() {
        val local = listOf(LocalRow("2026-08-12", padmanabhancba))

        val decisions = SleepRestorePlanner.plan(yninja004, listOf(aug12()), local)
        val planned = inserts(decisions)

        // The plan may only ever describe INSERTS. There is no decision type that
        // can update or delete, so padmanabhancba's row cannot be touched.
        assertEquals(1, planned.size)
        assertEquals(yninja004, planned[0].row.ownerUid)
        assertEquals("2026-08-12", planned[0].row.date)

        // The other owner is seen and reported, not silently ignored.
        assertEquals(listOf(padmanabhancba), planned[0].otherOwners)

        // No decision anywhere names padmanabhancba as a row to write.
        assertTrue(planned.none { it.row.ownerUid == padmanabhancba })
    }

    @Test
    fun `restore inserts nothing when the restoring account already holds the date`() {
        val local = listOf(
            LocalRow("2026-08-12", padmanabhancba),
            LocalRow("2026-08-12", yninja004)
        )

        val decisions = SleepRestorePlanner.plan(yninja004, listOf(aug12()), local)

        assertTrue(inserts(decisions).isEmpty())
        assertTrue(decisions.any { it is Decision.AlreadyOwned })
    }

    // ── Test 2 ───────────────────────────────────────────────────────────────
    // Idempotency across runs.

    @Test
    fun `running restore twice produces exactly one row`() {
        val remote = listOf(aug12())

        val first = inserts(SleepRestorePlanner.plan(yninja004, remote, emptyList()))
        assertEquals(1, first.size)

        // Apply the plan, then re-run against the resulting local state.
        val afterApply = first.map { LocalRow(it.row.date, it.row.ownerUid) }
        val second = inserts(SleepRestorePlanner.plan(yninja004, remote, afterApply))

        assertTrue("second pass must insert nothing", second.isEmpty())
    }

    @Test
    fun `two remote records for one date produce a single insert in one pass`() {
        // Defensive: the server keys automatic nights by (uid, date), but a plan
        // that trusted that would be one upsert bug away from writing duplicates.
        val remote = listOf(aug12(), aug12())

        val planned = inserts(SleepRestorePlanner.plan(yninja004, remote, emptyList()))

        assertEquals(1, planned.size)
    }

    // ── Test 3 ───────────────────────────────────────────────────────────────
    // Identical values across accounts must not deduplicate.

    @Test
    fun `identical sleep values across accounts do not suppress the restore`() {
        // padmanabhancba's local row has the SAME start, end, duration and quality.
        val local = listOf(LocalRow("2026-08-12", padmanabhancba))

        val planned = inserts(SleepRestorePlanner.plan(yninja004, listOf(aug12()), local))

        // Field equality is not consulted: yninja004 still gets its own row.
        assertEquals(1, planned.size)
        assertEquals(yninja004, planned[0].row.ownerUid)
        assertEquals(1786476878261L, planned[0].row.sleepStart)
        assertEquals(1786494637883L, planned[0].row.sleepEnd)
    }

    @Test
    fun `a date held only by another account is still restorable for this account`() {
        val local = listOf(
            LocalRow("2026-08-09", padmanabhancba),
            LocalRow("2026-08-10", padmanabhancba)
        )
        val remote = listOf(
            aug12(date = "2026-08-09"),
            aug12(date = "2026-08-10"),
            aug12(date = "2026-08-11")
        )

        val planned = inserts(SleepRestorePlanner.plan(yninja004, remote, local))

        assertEquals(3, planned.size)
        assertTrue(planned.all { it.row.ownerUid == yninja004 })
        assertEquals(listOf("2026-08-09", "2026-08-10", "2026-08-11"), planned.map { it.row.date })
    }

    // ── Test 4 ───────────────────────────────────────────────────────────────
    // Existing row for the authenticated uid/date, restored again.

    @Test
    fun `restoring an already-held date creates no duplicate`() {
        val local = listOf(LocalRow("2026-08-12", yninja004))

        val decisions = SleepRestorePlanner.plan(yninja004, listOf(aug12()), local)

        assertTrue(inserts(decisions).isEmpty())
        assertEquals(listOf(Decision.AlreadyOwned("2026-08-12")), decisions)
    }

    // ── Ownership can never come from the payload ────────────────────────────

    @Test
    fun `ownerUid always comes from the authenticated account not the record`() {
        // A record whose userId claims a different account must still restore as
        // the account that authenticated the request.
        val hostile = aug12().copy(userId = padmanabhancba)

        val planned = inserts(SleepRestorePlanner.plan(yninja004, listOf(hostile), emptyList()))

        assertEquals(1, planned.size)
        assertEquals(yninja004, planned[0].row.ownerUid)
    }

    @Test
    fun `a blank uid restores nothing`() {
        assertTrue(SleepRestorePlanner.plan("", listOf(aug12()), emptyList()).isEmpty())
    }

    // ── Only detected nights are restorable ──────────────────────────────────

    @Test
    fun `manual entries are not restored`() {
        val manual = RemoteSleepLog(
            date = "2026-08-12", mood = "happy", moodScore = 7, source = "manual"
        )

        val decisions = SleepRestorePlanner.plan(yninja004, listOf(manual), emptyList())

        assertTrue(inserts(decisions).isEmpty())
        assertTrue(decisions.single() is Decision.Unusable)
    }

    @Test
    fun `records without detected bounds are not restored`() {
        val moodOnly = RemoteSleepLog(date = "2026-08-12", mood = "tired", moodScore = 3)

        val decisions = SleepRestorePlanner.plan(yninja004, listOf(moodOnly), emptyList())

        assertTrue(inserts(decisions).isEmpty())
    }

    @Test
    fun `pre-source records with valid bounds are still restorable`() {
        // Written before `source` existed; judged by bounds presence, as the
        // server's own fallback does.
        val legacy = aug12().copy(source = null)

        assertEquals(1, inserts(SleepRestorePlanner.plan(yninja004, listOf(legacy), emptyList())).size)
    }

    @Test
    fun `inverted or zero bounds are rejected`() {
        val inverted = aug12(start = 1786494637883, end = 1786476878261)
        val zero = aug12(start = 0L, end = 0L)

        assertTrue(inserts(SleepRestorePlanner.plan(yninja004, listOf(inverted), emptyList())).isEmpty())
        assertTrue(inserts(SleepRestorePlanner.plan(yninja004, listOf(zero), emptyList())).isEmpty())
    }

    // ── Same account, several dates ──────────────────────────────────────────

    @Test
    fun `one account restoring different dates gets a row for each`() {
        val remote = listOf(
            aug12(date = "2026-08-13"),
            aug12(date = "2026-08-14"),
            aug12(date = "2026-08-15")
        )

        val planned = inserts(SleepRestorePlanner.plan(yninja004, remote, emptyList()))

        assertEquals(3, planned.size)
        assertEquals(listOf("2026-08-13", "2026-08-14", "2026-08-15"), planned.map { it.row.date })
        assertTrue(planned.all { it.row.ownerUid == yninja004 })
    }

    @Test
    fun `an account already holding one date still restores its other dates`() {
        val local = listOf(LocalRow("2026-08-13", yninja004))
        val remote = listOf(aug12(date = "2026-08-13"), aug12(date = "2026-08-14"))

        val decisions = SleepRestorePlanner.plan(yninja004, remote, local)

        assertEquals(listOf("2026-08-14"), inserts(decisions).map { it.row.date })
        assertTrue(decisions.any { it is Decision.AlreadyOwned && it.date == "2026-08-13" })
    }

    // ── An existing row is never rewritten with another account's data ───────

    @Test
    fun `no decision can target a row owned by another account`() {
        val local = listOf(
            LocalRow("2026-08-09", padmanabhancba),
            LocalRow("2026-08-10", padmanabhancba),
            LocalRow("2026-08-11", padmanabhancba),
            LocalRow("2026-08-12", padmanabhancba)
        )
        val remote = (9..12).map { aug12(date = "2026-08-%02d".format(it)) }

        val decisions = SleepRestorePlanner.plan(yninja004, remote, local)

        // Every write the plan describes is an insert owned by the restoring
        // account. There is no update/delete decision, so padmanabhancba's four
        // rows are structurally unreachable from a restore.
        assertTrue(decisions.all { it is Decision.Insert || it is Decision.AlreadyOwned || it is Decision.Unusable })
        assertTrue(inserts(decisions).all { it.row.ownerUid == yninja004 })

        // ...and the plan still reports whose rows it is leaving alone.
        assertTrue(inserts(decisions).all { it.otherOwners == listOf(padmanabhancba) })
    }

    // ── Manual mood logs are never confused with detected nights ─────────────

    @Test
    fun `a manual mood log does not satisfy the duplicate check for an automatic night`() {
        // Both a random-ID manual entry and the automatic record for the SAME
        // date. The manual one must neither restore nor suppress the automatic.
        val manualSameDate = RemoteSleepLog(
            id = "azWrUnunRP3QVsKYYFdq",
            date = "2026-08-12",
            mood = "stressed",
            moodScore = 3,
            source = "manual"
        )
        val remote = listOf(manualSameDate, aug12())

        val decisions = SleepRestorePlanner.plan(yninja004, remote, emptyList())
        val planned = inserts(decisions)

        assertEquals(1, planned.size)
        assertEquals("2026-08-12", planned[0].row.date)
        assertEquals(295, planned[0].row.totalSleepMinutes)
        assertTrue(decisions.any { it is Decision.Unusable })
    }

    @Test
    fun `manual entries never create local rows that could block a later restore`() {
        val manualOnly = listOf(
            RemoteSleepLog(id = "FYeh87MnvFCH7UPtmZD2", date = "2026-08-12", mood = "ok", moodScore = 5),
            RemoteSleepLog(id = "IUkWfdLwi4KrQ75eBanY", date = "2026-08-13", mood = "tired", moodScore = 2)
        )

        val decisions = SleepRestorePlanner.plan(yninja004, manualOnly, emptyList())

        assertTrue(inserts(decisions).isEmpty())
        assertEquals(2, decisions.count { it is Decision.Unusable })
    }

    // ── Same owner + same date, differing values → conflict, never overwrite ──

    @Test
    fun `matching own row is a silent skip`() {
        val own = listOf(SleepRestorePlanner.OwnRow("2026-08-12", 1786476878261, 1786494637883, 295))
        val local = listOf(LocalRow("2026-08-12", yninja004))

        val decisions = SleepRestorePlanner.plan(yninja004, listOf(aug12()), local, own)

        assertTrue(inserts(decisions).isEmpty())
        assertEquals(listOf(Decision.AlreadyOwned("2026-08-12")), decisions)
    }

    @Test
    fun `differing own row is reported as a conflict and never overwritten`() {
        // Local says 295 minutes; the server record says the same night ran longer.
        val own = listOf(SleepRestorePlanner.OwnRow("2026-08-12", 1786476878261, 1786494637883, 250))
        val local = listOf(LocalRow("2026-08-12", yninja004))

        val decisions = SleepRestorePlanner.plan(yninja004, listOf(aug12()), local, own)

        // No write of any kind is planned.
        assertTrue(inserts(decisions).isEmpty())

        val conflict = decisions.filterIsInstance<Decision.Conflict>().single()
        assertEquals("2026-08-12", conflict.date)
        assertEquals(250, conflict.local.totalSleepMinutes)
        assertEquals(295, conflict.remote.totalSleepMinutes)
    }

    @Test
    fun `a conflict identifies both records well enough to act on without investigation`() {
        // The Aug-10 regression, as a test. That conflict named the remote record
        // only by (account, date), which does not locate a legacy `.add()`
        // document, and named the local row not at all — so resolving it took
        // several rounds of console searching. Every field needed to find both
        // sides must therefore be present on the decision itself.
        val own = listOf(
            SleepRestorePlanner.OwnRow(
                date = "2026-08-12",
                sleepStart = 1786476878261, sleepEnd = 1786494637883, totalSleepMinutes = 250,
                localId = 42, syncState = "SYNCED", lastSyncError = null
            )
        )
        val local = listOf(LocalRow("2026-08-12", yninja004))
        val remote = aug12().copy(id = "dmcBwAQB66isRSvns8BD")

        val conflict = SleepRestorePlanner.plan(yninja004, listOf(remote), local, own)
            .filterIsInstance<Decision.Conflict>().single()

        // Remote side: the exact cloud document id, whatever its naming scheme.
        assertEquals("dmcBwAQB66isRSvns8BD", conflict.remote.remoteId)
        // Owner uid, carried on the planned row and never taken from the payload.
        assertEquals(yninja004, conflict.remote.ownerUid)
        // Logical key.
        assertEquals("2026-08-12", conflict.date)
        // Local side: primary key, plus the sync state that says whether this row
        // ever reached the cloud at all.
        assertEquals(42L, conflict.local.localId)
        assertEquals("SYNCED", conflict.local.syncState)
        // Both durations and both timestamp pairs, for comparison.
        assertEquals(250, conflict.local.totalSleepMinutes)
        assertEquals(295, conflict.remote.totalSleepMinutes)
        assertEquals(1786476878261L, conflict.local.sleepStart)
        assertEquals(1786494637883L, conflict.remote.sleepEnd)
    }

    @Test
    fun `a conflict on an unsynced local row reports its failure reason`() {
        val own = listOf(
            SleepRestorePlanner.OwnRow(
                date = "2026-08-12",
                sleepStart = 1786476878261, sleepEnd = 1786494637883, totalSleepMinutes = 250,
                localId = 7, syncState = "FAILED",
                lastSyncError = "NETWORK: SocketTimeoutException"
            )
        )
        val local = listOf(LocalRow("2026-08-12", yninja004))

        val conflict = SleepRestorePlanner.plan(yninja004, listOf(aug12()), local, own)
            .filterIsInstance<Decision.Conflict>().single()

        // Explains WHY the two sides differ, rather than only that they do.
        assertEquals("FAILED", conflict.local.syncState)
        assertEquals("NETWORK: SocketTimeoutException", conflict.local.lastSyncError)
    }

    @Test
    fun `a conflict on one date does not block restoring another`() {
        val own = listOf(SleepRestorePlanner.OwnRow("2026-08-12", 1786476878261, 1786494637883, 250))
        val local = listOf(LocalRow("2026-08-12", yninja004))
        val remote = listOf(aug12(), aug12(date = "2026-08-13"))

        val decisions = SleepRestorePlanner.plan(yninja004, remote, local, own)

        assertEquals(1, decisions.filterIsInstance<Decision.Conflict>().size)
        assertEquals(listOf("2026-08-13"), inserts(decisions).map { it.row.date })
    }

    @Test
    fun `a differing row owned by ANOTHER account is not a conflict`() {
        // ownRows only ever contains the restoring account's rows, so another
        // account's differing values can never produce a conflict or a skip.
        val local = listOf(LocalRow("2026-08-12", padmanabhancba))

        val decisions = SleepRestorePlanner.plan(yninja004, listOf(aug12()), local, ownRows = emptyList())

        assertTrue(decisions.filterIsInstance<Decision.Conflict>().isEmpty())
        assertEquals(1, inserts(decisions).size)
    }

    @Test
    fun `duration is reproduced from sleepDuration not recomputed from the span`() {
        // The span here is 17,759,622 ms = 295.99 min, but the stored duration is
        // 295 min because the detector excludes time awake. Restoring must
        // reproduce the stored value, not the wider span.
        val planned = inserts(SleepRestorePlanner.plan(yninja004, listOf(aug12()), emptyList()))

        assertEquals(295, planned[0].row.totalSleepMinutes)
    }
}
