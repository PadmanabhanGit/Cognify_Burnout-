package com.simats.burnouttracker.utils

expect fun formatTimestamp(timestamp: Long): String
expect fun formatMinutes(minutes: Int): String
expect fun formatHours(hours: Float): String
expect fun formatCurrentTime(): String
expect fun formatDashboardDate(): String
expect fun getLocalDateString(): String
