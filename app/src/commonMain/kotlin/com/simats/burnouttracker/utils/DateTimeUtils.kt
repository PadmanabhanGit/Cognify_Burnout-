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
