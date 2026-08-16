package com.simats.burnouttracker.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The sync state model's rules, exercised without Room, Android or a network.
 *
 * These matter because the previous model was a single `syncedAt` timestamp: a
 * night that had never been sent and a night whose upload had failed repeatedly
 * were the same value, and the reason for a failure was printed once and then
 * lost with the process. Every assertion below is about a distinction that model
 * could not make.
 */
class SyncStateMachineTest {

    private val t0 = 1_700_000_000_000L
    private val t1 = t0 + 5_000L
    private val fresh = SyncStatus()

    // ── the happy path ───────────────────────────────────────────────────────

    @Test
    fun `a new record starts PENDING with nothing recorded against it`() {
        assertEquals(SyncState.PENDING, fresh.state)
        assertEquals(0L, fresh.syncedAt)
        assertNull(fresh.lastSyncError)
    }

    @Test
    fun `successful sync walks PENDING to SYNCING to SYNCED`() {
        val syncing = SyncStateMachine.beginAttempt(fresh, t0)
        assertEquals(SyncState.SYNCING, syncing.state)
        // Still not confirmed — an attempt starting is not an outcome.
        assertEquals(0L, syncing.syncedAt)

        val synced = SyncStateMachine.succeeded(syncing, t1)
        assertEquals(SyncState.SYNCED, synced.state)
        assertEquals(t1, synced.syncedAt)
        assertNull(synced.lastSyncError)
    }

    @Test
    fun `only a confirmed response sets syncedAt`() {
        // Every transition except succeeded() must leave syncedAt at 0.
        val syncing = SyncStateMachine.beginAttempt(fresh, t0)
        val failed = SyncStateMachine.failed(syncing, "SocketTimeoutException: timeout", t1)
        assertEquals(0L, syncing.syncedAt)
        assertEquals(0L, failed.syncedAt)
    }

    // ── failures, by kind ────────────────────────────────────────────────────

    @Test
    fun `network failure lands in FAILED and is classified NETWORK`() {
        val failed = SyncStateMachine.failed(
            SyncStateMachine.beginAttempt(fresh, t0),
            "SocketTimeoutException: Socket timeout has expired", t1
        )

        assertEquals(SyncState.FAILED, failed.state)
        assertEquals(SyncFailureKind.NETWORK, SyncStateMachine.classify("SocketTimeoutException: x"))
        assertTrue(failed.lastSyncError!!.startsWith("NETWORK"))
    }

    @Test
    fun `HTTP failure is classified HTTP`() {
        assertEquals(
            SyncFailureKind.HTTP,
            SyncStateMachine.classify("ServerResponseException: Server error(500)")
        )
        val failed = SyncStateMachine.failed(fresh, "ClientRequestException: 401 Unauthorized", t1)
        assertEquals(SyncState.FAILED, failed.state)
        assertTrue(failed.lastSyncError!!.startsWith("HTTP"))
    }

    @Test
    fun `serialization failure is classified SERIALIZATION and outranks a timeout word`() {
        // The `_id` defect exactly: this is the one kind retrying cannot fix, so
        // it must not be mistaken for a network blip even when the message
        // happens to mention one.
        assertEquals(
            SyncFailureKind.SERIALIZATION,
            SyncStateMachine.classify(
                "SerializationException: Field 'id' is required but was missing (timeout)"
            )
        )
    }

    @Test
    fun `an unrecognised failure is UNKNOWN rather than silently a network problem`() {
        assertEquals(SyncFailureKind.UNKNOWN, SyncStateMachine.classify("something odd happened"))
        assertEquals(SyncFailureKind.UNKNOWN, SyncStateMachine.classify(null))
        assertEquals(SyncFailureKind.UNKNOWN, SyncStateMachine.classify(""))
    }

    // ── lastSyncError persistence ────────────────────────────────────────────

    @Test
    fun `lastSyncError is persisted with its kind and the original detail`() {
        val failed = SyncStateMachine.failed(fresh, "UnknownHostException: cognify-burnout.onrender.com", t1)

        assertNotNull(failed.lastSyncError)
        assertTrue(failed.lastSyncError!!.contains("NETWORK"))
        // The underlying detail survives — the kind is added, not substituted.
        assertTrue(failed.lastSyncError!!.contains("cognify-burnout.onrender.com"))
        assertEquals(t1, failed.lastSyncAttemptAt)
    }

    @Test
    fun `lastSyncError survives a retry starting and is cleared only by success`() {
        val failed = SyncStateMachine.failed(fresh, "SocketTimeoutException: x", t0)

        val retrying = SyncStateMachine.beginAttempt(failed, t1)
        // Still visible while the retry is in flight: "why did this fail last
        // time" is exactly what is useful during a retry.
        assertEquals(SyncState.SYNCING, retrying.state)
        assertNotNull(retrying.lastSyncError)

        val synced = SyncStateMachine.succeeded(retrying, t1)
        assertNull(synced.lastSyncError)
    }

    @Test
    fun `a huge error body is truncated rather than stored whole`() {
        val failed = SyncStateMachine.failed(fresh, "ServerResponseException: " + "x".repeat(5_000), t1)

        assertTrue(failed.lastSyncError!!.length <= 300)
        assertTrue(failed.lastSyncError!!.endsWith("…"))
    }

    // ── retry ────────────────────────────────────────────────────────────────

    @Test
    fun `retry after failure walks FAILED to SYNCING to SYNCED`() {
        val failed = SyncStateMachine.failed(SyncStateMachine.beginAttempt(fresh, t0), "SocketTimeoutException", t0)
        assertEquals(SyncState.FAILED, failed.state)

        val retrying = SyncStateMachine.beginAttempt(failed, t1)
        assertEquals(SyncState.SYNCING, retrying.state)

        val synced = SyncStateMachine.succeeded(retrying, t1)
        assertEquals(SyncState.SYNCED, synced.state)
        assertEquals(t1, synced.syncedAt)
    }

    @Test
    fun `a failed record is still owed to the backend`() {
        // The record must never be dropped merely because the POST failed.
        assertTrue(SyncState.FAILED.needsUpload)
        assertTrue(SyncState.PENDING.needsUpload)
        assertFalse(SyncState.SYNCED.needsUpload)
    }

    @Test
    fun `a record stranded in SYNCING is retried rather than stuck forever`() {
        // Process killed mid-upload. The old `syncedAt = 0` form would have kept
        // selecting it too, but only because it never had an in-flight state at
        // all; this asserts the new one does not regress into stranding it.
        assertTrue(SyncState.SYNCING.needsUpload)
        assertTrue(
            SyncStateMachine.isRetryable(SyncState.SYNCING, "uid-a", "uid-a", "2026-08-15", "2026-08-13")
        )
    }

    // ── terminal state ───────────────────────────────────────────────────────

    @Test
    fun `SYNCED is terminal and beginAttempt cannot move a confirmed record`() {
        val synced = SyncStateMachine.succeeded(SyncStateMachine.beginAttempt(fresh, t0), t0)

        val reattempted = SyncStateMachine.beginAttempt(synced, t1)

        assertEquals(SyncState.SYNCED, reattempted.state)
        assertEquals(t0, reattempted.syncedAt)
        // Not even the attempt timestamp moves — nothing about a confirmed record changes.
        assertEquals(synced, reattempted)
    }

    // ── account isolation ────────────────────────────────────────────────────

    @Test
    fun `retry never selects another account's record`() {
        val active = "knj65AANMfXR9bomcOiPBz1ozc53"
        val other = "2wZjgkBr4bhIyMwm7ifSPiAXIsN2"

        assertTrue(
            SyncStateMachine.isRetryable(SyncState.PENDING, active, active, "2026-08-15", "2026-08-13")
        )
        // Same state, same date, different owner: never picked up, or a retry
        // pass would re-POST another account's night under this account's token.
        assertFalse(
            SyncStateMachine.isRetryable(SyncState.PENDING, other, active, "2026-08-15", "2026-08-13")
        )
        assertFalse(
            SyncStateMachine.isRetryable(SyncState.FAILED, other, active, "2026-08-15", "2026-08-13")
        )
    }

    @Test
    fun `retry respects the date window and the synced state`() {
        val uid = "uid-a"
        assertFalse(
            SyncStateMachine.isRetryable(SyncState.PENDING, uid, uid, "2026-08-01", "2026-08-13")
        )
        assertFalse(
            SyncStateMachine.isRetryable(SyncState.SYNCED, uid, uid, "2026-08-15", "2026-08-13")
        )
    }

    // ── process death / stale SYNCING recovery ───────────────────────────────

    /**
     * Mirrors `SleepDao.reclaimStrandedSyncing`: SYNCING → FAILED with a reason,
     * everything else untouched. Stated here so the recovery rule is executable
     * rather than only expressible in SQL.
     */
    private fun reclaim(status: SyncStatus): SyncStatus =
        if (status.state == SyncState.SYNCING)
            status.copy(state = SyncState.FAILED, lastSyncError = "INTERRUPTED: upload did not complete")
        else status

    @Test
    fun `process death during SYNCING leaves a retryable record, never a lost one`() {
        val inFlight = SyncStateMachine.beginAttempt(fresh, t0)
        // Process dies here: no succeeded() and no failed() ever runs.
        assertEquals(SyncState.SYNCING, inFlight.state)
        assertEquals(0L, inFlight.syncedAt)
        // Still owed to the backend, so the retry query selects it.
        assertTrue(inFlight.state.needsUpload)
    }

    @Test
    fun `stale SYNCING is recovered into an honest retryable state on restart`() {
        val stranded = SyncStateMachine.beginAttempt(fresh, t0)

        val recovered = reclaim(stranded)

        assertEquals(SyncState.FAILED, recovered.state)
        assertTrue(recovered.lastSyncError!!.contains("INTERRUPTED"))
        assertTrue(recovered.state.needsUpload)
        // Recovery relabels only — it must not invent a confirmation.
        assertEquals(0L, recovered.syncedAt)
    }

    @Test
    fun `recovery never touches a confirmed record`() {
        val synced = SyncStateMachine.succeeded(SyncStateMachine.beginAttempt(fresh, t0), t1)

        assertEquals(synced, reclaim(synced))
    }

    @Test
    fun `a recovered record syncs normally on the next attempt`() {
        val recovered = reclaim(SyncStateMachine.beginAttempt(fresh, t0))

        val retried = SyncStateMachine.beginAttempt(recovered, t1)
        assertEquals(SyncState.SYNCING, retried.state)

        val synced = SyncStateMachine.succeeded(retried, t1)
        assertEquals(SyncState.SYNCED, synced.state)
        assertEquals(t1, synced.syncedAt)
    }

    // ── repeated sync of the same row ────────────────────────────────────────

    @Test
    fun `syncing the same row repeatedly converges rather than accumulating`() {
        var status = fresh
        repeat(5) {
            status = SyncStateMachine.succeeded(SyncStateMachine.beginAttempt(status, t0), t0)
        }

        // Idempotent: the fifth pass is indistinguishable from the first.
        assertEquals(SyncState.SYNCED, status.state)
        assertEquals(t0, status.syncedAt)
        assertNull(status.lastSyncError)
        // And a confirmed row is never re-sent, so no duplicate remote record
        // can be produced by repetition.
        assertFalse(status.state.needsUpload)
    }

    @Test
    fun `an unrecognised stored state is treated as PENDING not as synced`() {
        // Stranding a record would be the harmful failure mode; re-sending it is
        // idempotent server-side.
        assertEquals(SyncState.PENDING, SyncState.fromStored("WAT"))
        assertEquals(SyncState.PENDING, SyncState.fromStored(null))
        assertEquals(SyncState.SYNCED, SyncState.fromStored("SYNCED"))
    }
}
