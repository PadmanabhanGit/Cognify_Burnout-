package com.simats.burnouttracker.utils

import androidx.compose.runtime.Composable

/**
 * Key-value storage.
 *
 * Store names fall into exactly two categories, and the platform layer decides
 * which is which (see the Android actual):
 *
 *  - USER-SCOPED stores are resolved to a separate physical file per account, so
 *    two accounts on one device physically cannot read each other's values. This
 *    is structural: there is no key to forget to clear on logout, because there
 *    is nothing shared to clear.
 *
 *  - DEVICE stores hold things that belong to the handset rather than a person
 *    (theme, notification toggles, whether onboarding was completed). These are
 *    shared across accounts on purpose and survive sign-out.
 *
 * Callers do not need to know which is which — pass the logical store name and
 * the platform resolves it.
 */
@Composable
expect fun rememberPlatformSettings(name: String = "burnout_tracker_prefs"): PlatformSettings

/**
 * Non-composable accessor for the same store, for code that runs outside
 * composition (session lifecycle, background work, plain helpers).
 *
 * Resolves the active account at call time rather than caching it, so a store
 * obtained before an account change is never reused for the account after it.
 */
expect fun platformSettings(name: String = "burnout_tracker_prefs"): PlatformSettings

/**
 * The device-level store, shared across all accounts by design.
 *
 * Holds only settings that describe the handset, never anything about a person.
 */
expect fun deviceSettings(): PlatformSettings

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
