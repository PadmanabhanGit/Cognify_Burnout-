package com.simats.burnouttracker.utils

expect fun formatTimestamp(timestamp: Long): String
expect fun formatMinutes(minutes: Int): String
expect fun formatHours(hours: Float): String
expect fun formatCurrentTime(): String
expect fun formatDashboardDate(): String
expect fun getLocalDateString(): String

/**
 * Current local hour (0-23) and minute (0-59), device clock. Used only to
 * decide whether tonight's automatic sleep analysis window (which closes at
 * 09:00, with SleepWorker running ~09:15 — see SleepWorker.kt, unmodified)
 * could plausibly have completed yet. Never used to compute or display a
 * sleep value itself.
 */
expect fun getCurrentHourMinute(): Pair<Int, Int>

/** "2m ago" / "3h ago" style label for a [DetailedAppUsage.lastUsedAt] timestamp. */
fun formatRelativeTime(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return "--"
    val diffMinutes = ((getCurrentTimeMillis() - timestampMillis) / 60000L).coerceAtLeast(0L)
    return when {
        diffMinutes < 1 -> "Just now"
        diffMinutes < 60 -> "${diffMinutes}m ago"
        diffMinutes < 24 * 60 -> "${diffMinutes / 60}h ago"
        else -> "${diffMinutes / (24 * 60)}d ago"
    }
}
