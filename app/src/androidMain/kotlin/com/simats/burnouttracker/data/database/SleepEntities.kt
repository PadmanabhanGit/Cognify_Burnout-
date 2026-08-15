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
    @ColumnInfo(defaultValue = "") val ownerUid: String = ""
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
