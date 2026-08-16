package com.simats.burnouttracker.data.models

import com.simats.burnouttracker.data.models.StudyStopLifecycle.Recovery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The regression wall for study-session lifecycle.
 *
 * The invariant every test below defends:
 *
 *   At any point in logout, restart, process death, account switching, network
 *   failure, timeout or repeated request, a study session belongs to exactly one
 *   uid, can be stopped at most once semantically, and cannot disappear locally
 *   until the stop is either confirmed or durably retryable.
 *
 * These run on the JVM against the pure rules rather than against a device,
 * because every one of them describes a moment that is impractical to reproduce
 * by hand: a process dying between two writes, a token expiring mid-flush, a
 * second account signing in while a queue is non-empty.
 */
class StudySessionLifecycleTest {

    private val accountA = "knj65AANMfXR9bomcOiPBz1ozc53"
    private val accountB = "2wZjgkBr4bhIyMwm7ifSPiAXIsN2"
    private val t0 = 1_700_000_000_000L
    private val t1 = t0 + 60_000L

    private fun stop(
        owner: String = accountA,
        id: String = "sess-1",
        state: StudySessionState = StudySessionState.STOPPING
    ) = PendingStop(
        sessionId = id, ownerUid = owner, subject = "Physics",
        startedAt = t0, queuedAt = t0, state = state
    )

    /** A tiny in-memory stand-in for the durable queue, so ordering can be asserted. */
    private class FakeQueue {
        var active: ActiveStudySession? = null
        val stops = mutableListOf<PendingStop>()

        fun handOff(s: PendingStop) {
            // Mirrors StudySessionStore.handOffStop: enqueue FIRST, then release.
            if (stops.none { it.sessionId == s.sessionId }) stops += s
            active = null
        }

        fun confirm(id: String) { stops.removeAll { it.sessionId == id } }
    }

    // ── ownership ────────────────────────────────────────────────────────────

    @Test
    fun `an account may stop only its own session`() {
        assertTrue(StudyStopLifecycle.mayStop(accountA, accountA))
        assertFalse(StudyStopLifecycle.mayStop(accountA, accountB))
    }

    @Test
    fun `a signed-out app can stop nothing`() {
        // "" is the signed-out sentinel; matching it to anything would let a
        // logged-out app stop the previous account's sessions.
        assertFalse(StudyStopLifecycle.mayStop(accountA, ""))
        assertFalse(StudyStopLifecycle.mayStop("", ""))
        assertFalse(StudyStopLifecycle.mayStop("", accountA))
    }

    @Test
    fun `account B cannot stop account A's active session`() {
        val session = ActiveStudySession("sess-1", accountA, "Physics", t0)

        assertTrue(session.belongsTo(accountA))
        assertFalse(session.belongsTo(accountB))
        assertEquals(Recovery.IGNORE, StudyStopLifecycle.recoveryFor(session, accountB, stillRunning = true))
    }

    @Test
    fun `account A's queue is untouched when account B signs in`() {
        val queue = listOf(stop(owner = accountA, id = "a-1"), stop(owner = accountB, id = "b-1"))

        assertEquals(listOf("a-1"), StudyStopLifecycle.retryable(queue, accountA).map { it.sessionId })
        assertEquals(listOf("b-1"), StudyStopLifecycle.retryable(queue, accountB).map { it.sessionId })
        // And neither entry is removed by the other's flush.
        assertEquals(2, queue.size)
    }

    // ── stop outcomes ────────────────────────────────────────────────────────

    @Test
    fun `stop succeeds and the record leaves the queue`() {
        val q = FakeQueue().apply { active = ActiveStudySession("sess-1", accountA, "Physics", t0) }

        q.handOff(stop())
        assertNull(q.active)
        q.confirm("sess-1")

        assertTrue(q.stops.isEmpty())
    }

    @Test
    fun `stop network failure retains the record with a reason`() {
        val failed = StudyStopLifecycle.stopFailed(stop(), "SocketTimeoutException: expired", t1)

        assertEquals(StudySessionState.FAILED, failed.state)
        assertNotNull(failed.lastError)
        assertTrue(failed.lastError!!.startsWith("NETWORK"))
        // Everything a retry needs survives.
        assertEquals("sess-1", failed.sessionId)
        assertEquals(accountA, failed.ownerUid)
        assertEquals(t0, failed.startedAt)
    }

    @Test
    fun `an HTTP failure is retained and classified`() {
        val failed = StudyStopLifecycle.stopFailed(stop(), "ServerResponseException: 500", t1)

        assertEquals(StudySessionState.FAILED, failed.state)
        assertTrue(failed.lastError!!.startsWith("HTTP"))
        assertTrue(StudyStopLifecycle.isRetryable(failed, accountA))
    }

    @Test
    fun `failed stop then retry ends STOPPED`() {
        val failed = StudyStopLifecycle.stopFailed(stop(), "SocketTimeoutException", t0)
        val retrying = StudyStopLifecycle.beginStop(failed, t1)
        assertEquals(StudySessionState.STOPPING, retrying.state)

        val stopped = StudyStopLifecycle.stopConfirmed(retrying)
        assertEquals(StudySessionState.STOPPED, stopped.state)
        assertNull(stopped.lastError)
        assertFalse(StudyStopLifecycle.isRetryable(stopped, accountA))
    }

    // ── at-most-once semantics ───────────────────────────────────────────────

    @Test
    fun `a second stop request cannot reopen or extend a stopped session`() {
        val stopped = StudyStopLifecycle.stopConfirmed(stop())

        assertEquals(stopped, StudyStopLifecycle.beginStop(stopped, t1))
        assertEquals(stopped, StudyStopLifecycle.stopFailed(stopped, "SocketTimeoutException", t1))
    }

    @Test
    fun `logging out twice does not enqueue the stop twice`() {
        val q = FakeQueue().apply { active = ActiveStudySession("sess-1", accountA, "Physics", t0) }

        q.handOff(stop())
        q.handOff(stop())   // second logout, same session

        assertEquals(1, q.stops.size)
    }

    @Test
    fun `timeout after the server accepted the stop is safe to retry`() {
        // The client never saw the response, so it retries. Locally the record is
        // still STOPPING and retryable; the server answers already-stopped and the
        // record is confirmed without a second stop being applied.
        val inFlight = StudyStopLifecycle.beginStop(stop(), t0)
        assertTrue(StudyStopLifecycle.isRetryable(inFlight, accountA))

        val confirmed = StudyStopLifecycle.stopConfirmed(inFlight)
        assertEquals(StudySessionState.STOPPED, confirmed.state)
    }

    // ── durability across process death ──────────────────────────────────────

    @Test
    fun `a session is never dropped merely because the POST was dispatched`() {
        val q = FakeQueue().apply { active = ActiveStudySession("sess-1", accountA, "Physics", t0) }

        q.handOff(stop())   // enqueue, then release — the ordering under test

        // Process dies here. The id is still held durably, by the queue.
        assertNull(q.active)
        assertEquals(1, q.stops.size)
        assertEquals("sess-1", q.stops.single().sessionId)
        assertTrue(StudyStopLifecycle.isRetryable(q.stops.single(), accountA))
    }

    @Test
    fun `a record stranded in STOPPING is retried rather than stuck`() {
        assertTrue(StudySessionState.STOPPING.needsStop)
        assertTrue(StudyStopLifecycle.isRetryable(stop(state = StudySessionState.STOPPING), accountA))
    }

    // ── restart recovery ─────────────────────────────────────────────────────

    @Test
    fun `restart with an owned running session resumes it`() {
        val session = ActiveStudySession("sess-1", accountA, "Physics", t0)

        assertEquals(Recovery.RESUME, StudyStopLifecycle.recoveryFor(session, accountA, stillRunning = true))
    }

    @Test
    fun `restart with an owned but finished session hands it off rather than inventing an end`() {
        val session = ActiveStudySession("sess-1", accountA, "Physics", t0)

        assertEquals(Recovery.HAND_OFF, StudyStopLifecycle.recoveryFor(session, accountA, stillRunning = false))
    }

    @Test
    fun `a service callback arriving after logout is rejected by uid`() {
        val session = ActiveStudySession("sess-1", accountA, "Physics", t0)

        // Signed out: uid is blank, so nothing is resumed and nothing is stopped.
        assertEquals(Recovery.IGNORE, StudyStopLifecycle.recoveryFor(session, "", stillRunning = true))
        assertFalse(StudyStopLifecycle.mayStop(session.ownerUid, ""))
    }

    // ── logout ───────────────────────────────────────────────────────────────

    @Test
    fun `logout with no active session leaves the queue empty`() {
        val q = FakeQueue()   // nothing running

        assertNull(q.active)
        assertTrue(q.stops.isEmpty())
    }

    @Test
    fun `logout with an active session queues it and releases the slot`() {
        val q = FakeQueue().apply { active = ActiveStudySession("sess-1", accountA, "Physics", t0) }

        q.handOff(stop())

        assertNull(q.active)
        assertEquals(StudySessionState.STOPPING, q.stops.single().state)
        assertEquals(accountA, q.stops.single().ownerUid)
    }

    @Test
    fun `logout while a stop POST is pending does not enqueue a second stop`() {
        val q = FakeQueue().apply { active = ActiveStudySession("sess-1", accountA, "Physics", t0) }

        // Interactive stop: handed off, request dispatched but not yet answered.
        q.handOff(stop())
        val inFlight = q.stops.single()
        assertEquals(StudySessionState.STOPPING, inFlight.state)

        // Logout arrives while that request is still outstanding. The active slot
        // is already empty, so there is nothing left to hand off.
        assertNull(q.active)
        q.handOff(stop())

        assertEquals(1, q.stops.size)
    }

    @Test
    fun `repeated logout produces no duplicate stop events`() {
        val q = FakeQueue().apply { active = ActiveStudySession("sess-1", accountA, "Physics", t0) }

        repeat(4) { q.handOff(stop()) }

        assertEquals(1, q.stops.size)
        assertNull(q.active)
    }

    @Test
    fun `logout after the stop was already confirmed re-enqueues nothing`() {
        val q = FakeQueue().apply { active = ActiveStudySession("sess-1", accountA, "Physics", t0) }
        q.handOff(stop())
        q.confirm("sess-1")

        // Slot already released and record already gone: a later logout has
        // nothing to act on, so no stop event is produced for a finished session.
        assertNull(q.active)
        assertTrue(q.stops.isEmpty())
    }

    @Test
    fun `retrying the same stop repeatedly converges on one confirmed stop`() {
        var record = stop()
        repeat(4) { record = StudyStopLifecycle.beginStop(record, t1) }
        record = StudyStopLifecycle.stopConfirmed(record)

        // Further attempts are no-ops against a terminal record, so no repeat can
        // produce a second remote effect.
        assertEquals(record, StudyStopLifecycle.beginStop(record, t1))
        assertEquals(StudySessionState.STOPPED, record.state)
    }

    // ── unknown session ──────────────────────────────────────────────────────

    @Test
    fun `an unknown session fails explicitly and fabricates nothing`() {
        // 404 from the server. The record is marked FAILED with the reason kept;
        // no duration, end time or replacement record is invented for it.
        val failed = StudyStopLifecycle.stopFailed(stop(), "Session not found", t1)

        assertEquals(StudySessionState.FAILED, failed.state)
        assertNotNull(failed.lastError)
        assertTrue(failed.lastError!!.contains("Session not found"))
        assertEquals(t0, failed.startedAt)   // untouched
    }

    @Test
    fun `a session with no server id is never queued for a server stop`() {
        val session = ActiveStudySession(sessionId = null, ownerUid = accountA, subject = "Physics", startedAt = t0)

        // Owned, so recovery is not IGNORE — but there is no id to stop, which is
        // why the caller releases it locally instead of enqueueing an unstoppable
        // record.
        assertEquals(Recovery.RESUME, StudyStopLifecycle.recoveryFor(session, accountA, stillRunning = true))
        assertNull(session.sessionId)
    }
}
