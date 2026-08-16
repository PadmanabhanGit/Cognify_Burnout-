package com.simats.burnouttracker.data.models

import kotlinx.serialization.Serializable

/**
 * Where one study session stands in its own lifecycle.
 *
 * Deliberately parallel to [SyncState], because the failure it prevents is the
 * same one: an attempt that BEGAN was being treated as an attempt that
 * SUCCEEDED, so the local record was dropped while the server still held the
 * session open. Only a confirmed server response may produce [STOPPED].
 */
enum class StudySessionState {
    /** Running. The timer is counting and the server document is `isActive: true`. */
    ACTIVE,

    /** A stop has been handed off durably and may be in flight. Not yet confirmed. */
    STOPPING,

    /** The server confirmed the stop. Terminal — the record may now be discarded. */
    STOPPED,

    /** A stop attempt completed unsuccessfully. Retryable; the record is intact. */
    FAILED;

    /** Whether this session still owes the server a confirmed stop. */
    val needsStop: Boolean get() = this != STOPPED
}

/**
 * The running session, as durable state rather than memory.
 *
 * [ownerUid] is the whole point. Before this, the active session was identified
 * only by an id, and "whose session is this?" was answered by whoever happened
 * to be signed in when the stop button was pressed. That is wrong across an
 * account switch: account B could stop account A's session, and the server would
 * accept it if the token allowed. Ownership is now recorded when the session
 * starts and re-checked before every stop.
 *
 * [sessionId] is nullable because a session legitimately exists locally before
 * the start POST returns — the timer runs immediately. A session with no id
 * cannot be stopped server-side; it is finished through the offline queue,
 * which is why [subject] and [startedAt] are carried here too.
 */
@Serializable
data class ActiveStudySession(
    val sessionId: String?,
    val ownerUid: String,
    val subject: String,
    val startedAt: Long
) {
    /** True only for a non-blank uid that matches. A blank uid owns nothing. */
    fun belongsTo(uid: String): Boolean = ownerUid.isNotBlank() && ownerUid == uid
}

/**
 * A stop that has been promised durably but not yet confirmed by the server.
 *
 * This record IS the handoff. The invariant it exists to satisfy: a session id
 * leaves the active slot only once either the server has confirmed the stop, or
 * this record has been written — never merely because a POST was dispatched.
 * Process death between those two points therefore cannot lose the session.
 */
@Serializable
data class PendingStop(
    val sessionId: String,
    val ownerUid: String,
    val subject: String,
    val startedAt: Long,
    val queuedAt: Long,
    val state: StudySessionState = StudySessionState.STOPPING,
    val lastAttemptAt: Long = 0,
    val lastError: String? = null
)

/**
 * The stop rules, as pure functions.
 *
 * Free of Compose, Android, storage and the network for the same reason
 * [SyncStateMachine] and SleepRestorePlanner are: these rules are the feature,
 * and a rule that can only be checked by logging out on a physical device at the
 * right moment is a rule that will regress.
 */
object StudyStopLifecycle {

    /**
     * Whether [authenticatedUid] is allowed to stop a session owned by [ownerUid].
     *
     * The single authorization gate, applied before every stop — the interactive
     * one, the logout one, and every queue retry. A mismatch is not an error to
     * report and continue past: it means this session belongs to a different
     * account, and the correct action is to send nothing at all and leave the
     * record for its owner.
     *
     * A blank uid on either side never authorizes anything. Blank is the
     * signed-out sentinel, so treating it as a match would let a signed-out app
     * stop the last account's sessions.
     */
    fun mayStop(ownerUid: String, authenticatedUid: String): Boolean =
        ownerUid.isNotBlank() && authenticatedUid.isNotBlank() && ownerUid == authenticatedUid

    /**
     * A stop attempt is starting: ACTIVE or FAILED → STOPPING.
     *
     * [StudySessionState.STOPPED] is terminal and returns unchanged, so a
     * duplicate stop — a double tap, a logout racing the button, a queue retry
     * for something already confirmed — cannot reopen a finished session.
     *
     * [PendingStop.lastError] is kept, exactly as in the sync model: while a
     * retry is in flight the useful thing to show is still why the last one
     * failed.
     */
    fun beginStop(record: PendingStop, now: Long): PendingStop =
        if (record.state == StudySessionState.STOPPED) record
        else record.copy(state = StudySessionState.STOPPING, lastAttemptAt = now)

    /**
     * The server confirmed the stop: STOPPING → STOPPED.
     *
     * The ONLY transition that produces STOPPED, and the only one that clears
     * [PendingStop.lastError].
     */
    fun stopConfirmed(record: PendingStop): PendingStop =
        record.copy(state = StudySessionState.STOPPED, lastError = null)

    /**
     * The attempt finished unsuccessfully: STOPPING → FAILED.
     *
     * The record survives in full — sessionId, ownerUid, subject and startedAt
     * are all retained, because they are exactly what a later retry needs. A
     * failed stop must never cost us the ability to try again.
     *
     * Error text is produced by [SyncStateMachine.describeFailure], reused rather
     * than reimplemented so a study failure and a sleep failure classify
     * identically and read identically.
     */
    fun stopFailed(record: PendingStop, detail: String?, now: Long): PendingStop =
        if (record.state == StudySessionState.STOPPED) record
        else record.copy(
            state = StudySessionState.FAILED,
            lastAttemptAt = now,
            lastError = SyncStateMachine.describeFailure(detail)
        )

    /**
     * Whether a queued stop should be retried by the account now signed in.
     *
     * Both halves matter. The state half stops a confirmed record being sent
     * again; the ownership half stops account B flushing account A's queue,
     * which is the cross-account leak this project has already had to fix once
     * in the sleep pipeline.
     */
    fun isRetryable(record: PendingStop, authenticatedUid: String): Boolean =
        record.state.needsStop && mayStop(record.ownerUid, authenticatedUid)

    /** The subset of [queue] that [authenticatedUid] may retry, oldest first. */
    fun retryable(queue: List<PendingStop>, authenticatedUid: String): List<PendingStop> =
        queue.filter { isRetryable(it, authenticatedUid) }.sortedBy { it.queuedAt }

    /**
     * What a recovered active session should do on startup.
     *
     * Only ever RESUME or HAND_OFF — never "invent an end time". If a session
     * cannot be stopped cleanly its pending state is preserved and surfaced,
     * because a fabricated duration is worse than an unresolved one: it is
     * indistinguishable from a real measurement and silently corrupts every
     * total it feeds.
     */
    enum class Recovery {
        /** Ours and still running: keep counting. */
        RESUME,

        /** Ours but finished (or must finish): hand off to the durable stop queue. */
        HAND_OFF,

        /** Not ours. Leave it entirely alone — a different account owns it. */
        IGNORE
    }

    /**
     * Decides what to do with a persisted [ActiveStudySession] found at startup.
     *
     * [stillRunning] is the caller's observation of whether the session should
     * still be counting — false when logout, a stop, or a crash left it
     * unresolved.
     */
    fun recoveryFor(
        session: ActiveStudySession,
        authenticatedUid: String,
        stillRunning: Boolean
    ): Recovery = when {
        !mayStop(session.ownerUid, authenticatedUid) -> Recovery.IGNORE
        stillRunning -> Recovery.RESUME
        else -> Recovery.HAND_OFF
    }
}
