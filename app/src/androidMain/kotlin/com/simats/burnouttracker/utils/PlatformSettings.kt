package com.simats.burnouttracker.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberPlatformSettings(name: String): PlatformSettings {
    val context = LocalContext.current
    val sharedPreferences = remember(context, name) {
        context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }
    return remember(sharedPreferences) { PlatformSettings(sharedPreferences) }
}

actual class PlatformSettings(private val sharedPreferences: SharedPreferences) {
    actual fun getString(key: String, defaultValue: String?): String? {
        return sharedPreferences.getString(key, defaultValue)
    }

    actual fun putString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    actual fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    actual fun putInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    actual fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    actual fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}
