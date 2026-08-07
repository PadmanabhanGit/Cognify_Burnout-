package com.simats.burnouttracker.utils

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val timeInForeground: Long,
    val category: String
)
