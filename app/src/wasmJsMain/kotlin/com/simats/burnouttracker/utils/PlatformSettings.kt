package com.simats.burnouttracker.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window

@Composable
actual fun rememberPlatformSettings(name: String): PlatformSettings {
    return remember(name) { PlatformSettings(name) }
}

actual class PlatformSettings(private val name: String) {
    actual fun getString(key: String, defaultValue: String?): String? {
        return window.localStorage.getItem("$name.$key") ?: defaultValue
    }

    actual fun putString(key: String, value: String) {
        window.localStorage.setItem("$name.$key", value)
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return window.localStorage.getItem("$name.$key")?.toBoolean() ?: defaultValue
    }

    actual fun putBoolean(key: String, value: Boolean) {
        window.localStorage.setItem("$name.$key", value.toString())
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        return window.localStorage.getItem("$name.$key")?.toIntOrNull() ?: defaultValue
    }

    actual fun putInt(key: String, value: Int) {
        window.localStorage.setItem("$name.$key", value.toString())
    }

    actual fun remove(key: String) {
        window.localStorage.removeItem("$name.$key")
    }

    actual fun clear() {
        // This is a bit aggressive, maybe just clear keys with our prefix
        // For simplicity:
        window.localStorage.clear()
    }
}
