package com.simats.burnouttracker.data.database

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
    val disturbanceScore: Int
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
