package com.simats.burnouttracker.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import com.simats.burnouttracker.data.models.SyncState
import com.simats.burnouttracker.data.models.SyncStateMachine
import com.simats.burnouttracker.data.models.SyncStatus

@Entity(tableName = "sleep_sessions")
data class SleepSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val sleepStart: Long,
    val sleepEnd: Long,
    val totalSleepMinutes: Int,
    val awakeningCount: Int,
    val sleepQuality: Int,
    val disturbanceScore: Int,
    /**
     * Firebase UID of the account this night belongs to.
     *
     * This table used to be device-level, so the only way to stop a new account
     * seeing the previous one's nights was to DELETE every row on an account
     * change — which also destroyed the outgoing account's history for good.
     * Ownership is recorded per row instead: every query is scoped to the
     * active account, so switching away no longer deletes anything and
     * switching back finds the account's own nights still there.
     *
     * "" means unowned: rows written before this column existed. They are
     * claimed by the first account this device adopts (see AccountScope), which
     * is the same account that already owned them.
     *
     * wake_events and app_usage_logs need no column of their own — they are
     * reachable only through their parent session's id, which is itself scoped.
     */
    @ColumnInfo(defaultValue = "") val ownerUid: String = "",
    /**
     * When this night was last accepted by the backend, or 0 if it never was.
     *
     * Sync used to be fire-once: SleepMonitoringEngine POSTed each night
     * immediately after inserting it, and if that POST failed — no network, an
     * expired token, the server down — nothing recorded the failure. The
     * duplicate-date guard then skipped the night on every later scan, so the
     * row stayed in Room forever and never reached Firestore. The web app,
     * reading only Firestore, showed a different night's numbers than the phone.
     *
     * Persisting the outcome is what makes a retry possible: a row with
     * syncedAt = 0 is a night the backend has not confirmed, and
     * AndroidSleepRepository.refreshSleepData() re-sends those.
     *
     * 0 for rows written before this column existed. They are re-sent once, which
     * is safe: automatic writes are keyed `${uid}_${date}_automatic` server-side
     * and merge onto the same document, so a re-send reconciles that night rather
     * than duplicating it.
     *
     * Now paired with [syncState], which it stays consistent with by
     * construction: `syncedAt > 0` exactly when the state is SYNCED. This column
     * answers "when", [syncState] answers "where in the process".
     */
    @ColumnInfo(defaultValue = "0") val syncedAt: Long = 0,
    /**
     * PENDING / SYNCING / SYNCED / FAILED — see [SyncState].
     *
     * Stored as text rather than an ordinal so the value stays readable in a
     * database dump and survives anyone reordering the enum. Read through
     * [SyncState.fromStored], which treats an unrecognised value as PENDING:
     * that re-sends the row once (idempotent server-side) instead of stranding
     * it forever, which is what assuming SYNCED would do.
     *
     * Existing rows are migrated from [syncedAt] rather than defaulted, so no
     * night already accepted by the backend is ever re-sent by this change.
     */
    @ColumnInfo(defaultValue = "PENDING") val syncState: String = SyncState.PENDING.name,
    /**
     * Why the last upload attempt failed, or null if none has.
     *
     * The point of persisting it: failures used to be printed and then lost with
     * the process, so a night stuck unsynced carried no record of WHY — a dead
     * network and a DTO that no longer matches the backend looked identical, and
     * only one of those is fixable by waiting. Kind-prefixed (see
     * [SyncStateMachine.describeFailure]) and length-capped.
     *
     * Cleared only by a successful sync. It deliberately survives a retry
     * starting, so an in-flight record still shows what went wrong last time.
     */
    val lastSyncError: String? = null,
    /**
     * When an upload attempt last began, or 0.
     *
     * Diagnostics only — deliberately NOT a retry gate. Making retries wait on a
     * backoff computed from this would mean a record could be silently
     * un-retryable, and there is no evidence yet that this app needs backoff.
     */
    @ColumnInfo(defaultValue = "0") val lastSyncAttemptAt: Long = 0
)

/**
 * The row's sync bookkeeping as one value, so the transition rules in
 * [SyncStateMachine] can be applied without the caller unpacking four columns.
 */
val SleepSession.syncStatus: SyncStatus
    get() = SyncStatus(
        state = SyncState.fromStored(syncState),
        syncedAt = syncedAt,
        lastSyncError = lastSyncError,
        lastSyncAttemptAt = lastSyncAttemptAt
    )

@Entity(
    tableName = "wake_events",
    foreignKeys = [ForeignKey(
        entity = SleepSession::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class WakeEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val duration: Long,
    val appName: String,
    val packageName: String,
    val category: String
)

@Entity(
    tableName = "app_usage_logs",
    foreignKeys = [ForeignKey(
        entity = SleepSession::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class AppUsageLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val appName: String,
    val packageName: String,
    val category: String
)
