package com.simats.burnouttracker.utils

import androidx.compose.runtime.Composable

@Composable
expect fun rememberPlatformSettings(name: String = "burnout_tracker_prefs"): PlatformSettings

expect class PlatformSettings {
    fun getString(key: String, defaultValue: String? = null): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getInt(key: String, defaultValue: Int = 0): Int
    fun putInt(key: String, value: Int)
    fun remove(key: String)
    fun clear()
}
