package com.simats.burnouttracker.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

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
     */
    @ColumnInfo(defaultValue = "0") val syncedAt: Long = 0
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
