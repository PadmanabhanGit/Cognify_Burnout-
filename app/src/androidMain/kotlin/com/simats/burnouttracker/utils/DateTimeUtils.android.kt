package com.simats.burnouttracker.utils

import java.text.SimpleDateFormat
import java.util.*

actual fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "--:--"
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

actual fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

actual fun formatHours(hours: Float): String {
    val h = hours.toInt()
    val m = ((hours - h) * 60).toInt()
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

actual fun formatCurrentTime(): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date())
}

actual fun formatDashboardDate(): String {
    val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy • h:mm a", Locale.getDefault())
    return sdf.format(Date())
}

actual fun getLocalDateString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}
