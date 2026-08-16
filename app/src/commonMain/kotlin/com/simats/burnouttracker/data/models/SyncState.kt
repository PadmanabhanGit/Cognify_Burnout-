package com.simats.burnouttracker.data.models

/**
 * Where one locally-created record stands with respect to the backend.
 *
 * WHY THIS REPLACES A BARE TIMESTAMP
 * Sync state used to be a single `syncedAt: Long` — 0 or "when the server took
 * it". That collapses two genuinely different situations into one value: a
 * night detected thirty seconds ago that has not been sent yet, and a night
 * whose upload failed four times. Both read as 0, so nothing could tell the
 * user (or the log) which one it was looking at, and the reason for a failure
 * was printed once and then lost with the process.
 *
 * [SyncState] separates them, and [SyncStatus.lastSyncError] keeps the reason.
 * `syncedAt` is unchanged and still means "when the server confirmed this" —
 * the two are consistent by construction: `syncedAt > 0` exactly when the state
 * is [SYNCED].
 */
enum class SyncState {
    /** Created locally; no upload attempted yet. The state of every new record. */
    PENDING,

    /** An upload is in flight. */
    SYNCING,

    /** The server confirmed the write. The only state that stops retrying. */
    SYNCED,

    /** An attempt finished unsuccessfully. The record is intact and retryable. */
    FAILED;

    /**
     * Whether this record still owes the backend an upload.
     *
     * [SYNCING] counts as owing one. A row observed in SYNCING by a later pass
     * belongs to an attempt that never reached a terminal state — the process
     * was killed mid-upload — and would otherwise sit there permanently. Sending
     * it again is safe: automatic sleep writes are keyed `${uid}_${date}_automatic`
     * server-side and merge, so a duplicate POST reconciles the night instead of
     * adding one.
     */
    val needsUpload: Boolean get() = this != SYNCED

    companion object {
        /**
         * Reads a stored value, defaulting to [PENDING] for anything unrecognised.
         *
         * Unrecognised means a row written by a build that named states
         * differently. Treating that as PENDING re-sends the row once, which is
         * idempotent; treating it as SYNCED would silently strand it.
         */
        fun fromStored(raw: String?): SyncState =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

/** What kind of thing went wrong, kept because the four cases want different handling. */
enum class SyncFailureKind {
    /** No usable connection: timeout, DNS, refused, unreachable. Retrying later is likely to work. */
    NETWORK,

    /** The server answered with an error status. Retrying may work; a 4xx may not. */
    HTTP,

    /** The response did not match the DTO. Retrying will NOT help — this is a code/contract bug. */
    SERIALIZATION,

    /** Anything unclassified. */
    UNKNOWN
}

/** The full sync bookkeeping for one record. Plain data, no persistence concerns. */
data class SyncStatus(
    val state: SyncState = SyncState.PENDING,
    /** When the server confirmed this record, or 0. Non-zero exactly when [state] is SYNCED. */
    val syncedAt: Long = 0,
    /** Why the last attempt failed, or null. Survives retries; cleared only by success. */
    val lastSyncError: String? = null,
    /** When an attempt last began, or 0. Diagnostics only — never a retry gate. */
    val lastSyncAttemptAt: Long = 0
)

/**
 * The state transitions, as pure functions.
 *
 * Deliberately free of Room, Android and the network — the same reason
 * SleepRestorePlanner is split out of SleepHistoryRestore. The rules ARE the
 * feature, so they have to be executable in a JVM test rather than asserted in
 * a comment and verified by hand on a device.
 */
object SyncStateMachine {

    /** Longest error text persisted. An HTML error page must not become a database row. */
    private const val MAX_ERROR_LENGTH = 300

    /**
     * An upload is starting: PENDING or FAILED → SYNCING.
     *
     * [SyncState.SYNCED] is terminal and returns unchanged. Nothing in the app
     * re-uploads a confirmed record, and if a stray call ever tried, this is
     * where it stops rather than briefly showing a synced night as in-flight.
     *
     * [SyncStatus.lastSyncError] is deliberately NOT cleared here. While a retry
     * is in flight the most useful thing to show is still why the last one
     * failed; only success is allowed to erase that.
     */
    fun beginAttempt(current: SyncStatus, now: Long): SyncStatus =
        if (current.state == SyncState.SYNCED) current
        else current.copy(state = SyncState.SYNCING, lastSyncAttemptAt = now)

    /**
     * The server confirmed the write: SYNCING → SYNCED.
     *
     * The only transition that may set [SyncStatus.syncedAt], and the only one
     * that clears [SyncStatus.lastSyncError] — a record is synced because a
     * response said so, never because an attempt was merely made.
     */
    fun succeeded(current: SyncStatus, now: Long): SyncStatus =
        current.copy(state = SyncState.SYNCED, syncedAt = now, lastSyncError = null)

    /**
     * The attempt finished unsuccessfully: SYNCING → FAILED.
     *
     * [SyncStatus.syncedAt] is left exactly as it was. A failed retry of a
     * never-synced record keeps 0; nothing here can fabricate a confirmation.
     * The record itself is untouched — failure records a fact ABOUT the upload,
     * it never rolls back local data.
     */
    fun failed(current: SyncStatus, detail: String?, now: Long): SyncStatus =
        current.copy(
            state = SyncState.FAILED,
            lastSyncError = describeFailure(detail),
            lastSyncAttemptAt = now
        )

    /**
     * The stored error string: kind first, then the original detail.
     *
     * Kind-first so a row is triageable at a glance without parsing exception
     * names, and truncated so an HTML error body cannot bloat the table.
     */
    fun describeFailure(detail: String?): String {
        val trimmed = detail?.trim().orEmpty().ifEmpty { "no detail" }
        val text = "${classify(detail)}: $trimmed"
        return if (text.length <= MAX_ERROR_LENGTH) text
        else text.take(MAX_ERROR_LENGTH - 1) + "…"
    }

    /**
     * Classifies a failure from the text the client produced for it.
     *
     * Text rather than an exception, because ApiClient converts transport
     * failures into `success = false` with a message before this code ever sees
     * them — so the message is the only signal available. Matching is on
     * exception class names, which is what ApiClient puts at the front of that
     * message.
     *
     * SERIALIZATION is checked FIRST and matters most: it is the only kind that
     * will never succeed on retry, because it means the DTO and the backend
     * disagree. That is exactly the `_id` defect, which stayed invisible for a
     * whole debugging session by being indistinguishable from a network blip.
     */
    fun classify(detail: String?): SyncFailureKind {
        val text = detail?.lowercase().orEmpty()
        return when {
            text.isBlank() -> SyncFailureKind.UNKNOWN

            listOf("serializationexception", "jsonconvertexception", "jsondecodingexception",
                "missingfieldexception", "nosuchelementexception")
                .any { it in text } -> SyncFailureKind.SERIALIZATION

            listOf("sockettimeout", "connecttimeout", "unknownhost", "connectexception",
                "noroutetohost", "unable to resolve host", "network is unreachable",
                "timeout", "ssl", "connection reset")
                .any { it in text } -> SyncFailureKind.NETWORK

            listOf("serverresponseexception", "clientrequestexception", "redirectresponseexception",
                "responseexception", "http ", "status code")
                .any { it in text } -> SyncFailureKind.HTTP

            else -> SyncFailureKind.UNKNOWN
        }
    }

    /**
     * Whether a record should be picked up by a retry pass.
     *
     * This is the SPECIFICATION for `SleepDao.getUnsyncedSessions`, which mirrors
     * it in SQL. Both are stated in one place so the isolation rule can be tested
     * without a device: a record is retried only when it belongs to the ACTIVE
     * account, still owes an upload, and falls inside the pass's date window.
     *
     * The owner check is the account-isolation guarantee. Without it a retry pass
     * would re-POST another account's nights under the signed-in account's token,
     * which is precisely the cross-account leak this codebase already fixed once
     * at the detection layer.
     */
    fun isRetryable(
        state: SyncState,
        recordOwnerUid: String,
        activeUid: String,
        date: String,
        sinceDate: String
    ): Boolean =
        recordOwnerUid == activeUid && state.needsUpload && date >= sinceDate
}
