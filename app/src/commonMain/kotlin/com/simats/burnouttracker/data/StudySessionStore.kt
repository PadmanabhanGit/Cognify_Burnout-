package com.simats.burnouttracker.data

import com.simats.burnouttracker.data.models.ActiveStudySession
import com.simats.burnouttracker.data.models.PendingStop
import com.simats.burnouttracker.data.models.StudySessionState
import com.simats.burnouttracker.data.models.StudyStopLifecycle
import com.simats.burnouttracker.utils.PlatformSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Durable storage for the active study session and for stops that have been
 * promised but not yet confirmed.
 *
 * WHY THIS IS NOT JUST MORE KEYS IN THE SCREEN
 * The active session used to be three loose strings written and cleared from
 * inside a Compose click handler, with the clear happening BEFORE the stop POST
 * resolved. That ordering is the defect: once `activeSessionId` was gone, a
 * failed stop had nothing left to retry with, and the server kept the session
 * `isActive: true` forever — invisible to every total. Putting the reads and
 * writes behind one object is what makes the ordering reviewable in a single
 * place instead of being re-derived at each call site.
 *
 * ACCOUNT SCOPING IS ALREADY HALF DONE, AND THAT IS DELIBERATELY NOT REDONE
 * The `study_tracker` store is in PrefStores.USER_SCOPED, so this file already
 * resolves to `study_tracker__u_<uid>` and one account physically cannot read
 * another's keys. That is the primary isolation and it is left exactly as it is.
 * The [ActiveStudySession.ownerUid] and [PendingStop.ownerUid] stamps added here
 * are a SECOND, independent check for the window the namespace cannot cover:
 * store resolution reads the persisted active uid, so during sign-in or logout —
 * before AccountScope has synced, or after it has moved on — a resolved store and
 * the authenticated user can briefly disagree. Every operation below re-checks
 * ownership against the live authenticated uid rather than trusting that the
 * right file was opened.
 */
object StudySessionStore {

    private const val KEY_SESSION_ID = "activeSessionId"
    private const val KEY_SESSION_NAME = "activeSessionName"
    private const val KEY_SESSION_START = "sessionStartTime"

    /**
     * Added by this change. Everything else above predates it and is read with
     * the same names, so a session already running across the upgrade is still
     * found — it simply has no recorded owner (see [readActive]).
     */
    private const val KEY_SESSION_UID = "activeSessionUid"

    /** Stops promised but not yet confirmed. Distinct from `pending_sessions`. */
    private const val KEY_PENDING_STOPS = "pending_stops"

    private val json = Json { ignoreUnknownKeys = true }

    // ── active session ───────────────────────────────────────────────────────

    /**
     * The persisted running session, or null.
     *
     * A session written before [KEY_SESSION_UID] existed has no owner recorded.
     * It is returned with the CURRENT account as owner rather than being
     * discarded or being given a blank owner: discarding would silently lose a
     * legitimately running session across the upgrade, and a blank owner would
     * make it unstoppable by anyone. The store it was read from is already
     * namespaced to this account, so this account is the only one it could have
     * belonged to.
     */
    fun readActive(settings: PlatformSettings, currentUid: String): ActiveStudySession? {
        val subject = settings.getString(KEY_SESSION_NAME, null)?.takeIf { it.isNotBlank() } ?: return null
        val startedAt = settings.getString(KEY_SESSION_START, null)?.toLongOrNull() ?: return null
        return ActiveStudySession(
            sessionId = settings.getString(KEY_SESSION_ID, null)?.takeIf { it.isNotBlank() },
            ownerUid = settings.getString(KEY_SESSION_UID, null)?.takeIf { it.isNotBlank() } ?: currentUid,
            subject = subject,
            startedAt = startedAt
        )
    }

    /** Persists [session]. The owner is written FIRST so a crash cannot leave an unowned session. */
    fun writeActive(settings: PlatformSettings, session: ActiveStudySession) {
        settings.putString(KEY_SESSION_UID, session.ownerUid)
        settings.putString(KEY_SESSION_NAME, session.subject)
        settings.putString(KEY_SESSION_START, session.startedAt.toString())
        session.sessionId?.let { settings.putString(KEY_SESSION_ID, it) }
    }

    /** Records the server-assigned id on the already-running session. */
    fun attachSessionId(settings: PlatformSettings, sessionId: String) {
        settings.putString(KEY_SESSION_ID, sessionId)
    }

    /**
     * Releases the active slot.
     *
     * SAFE ONLY AFTER a confirmed stop or a completed durable handoff — see
     * [handOffStop], which is the only thing that should call this on the
     * failure path. Calling it earlier is precisely the defect this slice fixes.
     */
    fun clearActive(settings: PlatformSettings) {
        settings.remove(KEY_SESSION_ID)
        settings.remove(KEY_SESSION_NAME)
        settings.remove(KEY_SESSION_START)
        settings.remove(KEY_SESSION_UID)
    }

    // ── pending stops ────────────────────────────────────────────────────────

    /** Every promised-but-unconfirmed stop, across states. Corrupt storage reads as empty. */
    fun readPendingStops(settings: PlatformSettings): List<PendingStop> =
        try {
            json.decodeFromString<List<PendingStop>>(settings.getString(KEY_PENDING_STOPS, "[]") ?: "[]")
        } catch (e: Exception) {
            emptyList()
        }

    private fun writePendingStops(settings: PlatformSettings, queue: List<PendingStop>) {
        settings.putString(KEY_PENDING_STOPS, json.encodeToString(queue))
    }

    /**
     * THE HANDOFF. Writes the pending stop, then — and only then — releases the
     * active slot.
     *
     * This ordering is the invariant of this whole slice: the session id exists
     * in durable state continuously, first as the active session and then as a
     * pending stop, with no window in which neither holds it. A process killed
     * between the two writes leaves the active session intact and recoverable;
     * a process killed after them leaves a retryable pending stop. Nothing is
     * ever dropped because a POST was merely dispatched.
     *
     * Idempotent per session id: re-handing off a session already queued updates
     * that entry instead of adding a second one, so a double tap or a logout
     * racing the stop button cannot enqueue the same stop twice.
     */
    fun handOffStop(settings: PlatformSettings, stop: PendingStop) {
        val queue = readPendingStops(settings)
        val existing = queue.firstOrNull { it.sessionId == stop.sessionId }
        val merged = if (existing == null) queue + stop
        else queue.map { if (it.sessionId == stop.sessionId) it.copy(queuedAt = it.queuedAt) else it }
        writePendingStops(settings, merged)
        clearActive(settings)
    }

    /** Replaces one entry, matched by session id. A missing entry is not re-added. */
    fun updateStop(settings: PlatformSettings, updated: PendingStop) {
        writePendingStops(
            settings,
            readPendingStops(settings).map { if (it.sessionId == updated.sessionId) updated else it }
        )
    }

    /**
     * Drops a confirmed stop.
     *
     * Only ever called after the server has answered — [StudySessionState.STOPPED]
     * is the sole state that may leave this queue, which is the study-side
     * statement of the same rule sync uses for SYNCED.
     */
    fun removeStop(settings: PlatformSettings, sessionId: String) {
        writePendingStops(settings, readPendingStops(settings).filterNot { it.sessionId == sessionId })
    }

    /** The entries [authenticatedUid] is allowed to retry, oldest first. */
    fun retryableStops(settings: PlatformSettings, authenticatedUid: String): List<PendingStop> =
        StudyStopLifecycle.retryable(readPendingStops(settings), authenticatedUid)
}
