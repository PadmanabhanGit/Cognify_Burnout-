package com.simats.burnouttracker.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Resolves logical store names to physical SharedPreferences files.
 *
 * THE MODEL
 * A user-scoped store gets one file per account: `study_tracker` becomes
 * `study_tracker__u_<uid>`. Two accounts on one device therefore cannot read
 * each other's cached values, because the bytes are in different files.
 *
 * This replaces "remember to clear the right keys on logout", which is the
 * approach that failed: the logout handler cleared nine keys out of the
 * `settings` file while every one of those keys actually lived in
 * `burnout_tracker_prefs` or `study_tracker`, so all nine removals were silent
 * no-ops and the previous account's name and study hours survived sign-out.
 * With per-account files there is no shared state to forget to clear.
 *
 * THE SCOPING KEY
 * Resolution uses [AccountScope.activeUid], which is PERSISTED, not
 * [UserSession.uid], which is in-memory. Broadcast receivers and services are
 * routinely started in a fresh process where no session has been established
 * yet; keying on in-memory state would resolve those to the empty namespace and
 * silently break the action-plan alarms and the app blocker.
 */
internal object PrefStores {

    /** Shared across accounts on purpose: theme, notification toggles, language, onboarding. */
    const val DEVICE = "settings"

    /** The store name callers get when they don't name one. */
    const val DEFAULT = "burnout_tracker_prefs"

    /**
     * Stores whose contents describe a person rather than the handset.
     *
     * `action_plan` is included in full: every key in it is one person's
     * wellness plan — bedtime, mindfulness and hydration reminders, and the
     * social/streaming/gaming limits. The accessibility grant that powers app
     * blocking lives in the OS, not here, so nothing device-level is lost.
     */
    val USER_SCOPED = setOf(
        DEFAULT,
        "study_tracker",
        "burnout_history",
        "action_plan"
    )

    /**
     * Namespace used when no account has been adopted — signed out, or a fresh
     * install before first login.
     *
     * Writes that happen with no signed-in user land here rather than in some
     * real account's file, and nothing reads it once an account is active.
     */
    private const val NO_ACCOUNT = "__u_none"

    /**
     * Device-level keys that were historically written into the (now
     * user-scoped) default store. They gate onboarding and are read before any
     * login, so they must live in the device store. See [migrateDeviceKeys].
     */
    private val DEVICE_KEYS_IN_LEGACY_DEFAULT = listOf("policy_accepted", "permissions_viewed")

    private const val MIGRATION_FLAG = "__device_keys_migrated"

    fun resolve(context: Context, name: String): String {
        if (name !in USER_SCOPED) return name
        val uid = AccountScope.activeUid(context)
        return if (uid.isBlank()) "$name$NO_ACCOUNT" else "${name}__u_$uid"
    }

    fun open(context: Context, name: String): SharedPreferences =
        context.applicationContext
            .getSharedPreferences(resolve(context, name), Context.MODE_PRIVATE)

    fun openDevice(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(DEVICE, Context.MODE_PRIVATE)

    /**
     * Moves onboarding flags out of the legacy unscoped default store and into
     * the device store, once per install.
     *
     * Without this, an existing user would be shown the splash and privacy
     * onboarding again after upgrading, because the flags would be looked for in
     * the device store while still sitting in the old file. Idempotent, and it
     * never overwrites a value already present in the device store.
     */
    fun migrateDeviceKeys(context: Context) {
        val app = context.applicationContext
        val device = openDevice(app)
        if (device.getBoolean(MIGRATION_FLAG, false)) return

        val legacy = app.getSharedPreferences(DEFAULT, Context.MODE_PRIVATE)
        val editor = device.edit()
        DEVICE_KEYS_IN_LEGACY_DEFAULT.forEach { key ->
            if (legacy.contains(key) && !device.contains(key)) {
                editor.putBoolean(key, legacy.getBoolean(key, false))
            }
        }
        editor.putBoolean(MIGRATION_FLAG, true).apply()
    }

    /**
     * Copies the legacy unscoped stores into [uid]'s namespace.
     *
     * Called exactly once, for the first account this device adopts, and only
     * when that account was ALREADY signed in when this build first ran — the
     * same rule [AccountScope] applies to unowned sleep rows, and for the same
     * reason. Those files were written by whoever was signed in at the time; an
     * account that signs in afterwards did not write them and must not inherit
     * them.
     *
     * The legacy files are left in place rather than deleted. Copying is
     * reversible and costs a few kilobytes; deleting is not, and this runs
     * exactly once against data we cannot regenerate.
     */
    @Suppress("UNCHECKED_CAST")
    fun adoptLegacyStores(context: Context, uid: String) {
        val app = context.applicationContext
        USER_SCOPED.forEach { name ->
            val legacy = app.getSharedPreferences(name, Context.MODE_PRIVATE)
            val entries = legacy.all
            if (entries.isEmpty()) return@forEach

            val target = app.getSharedPreferences("${name}__u_$uid", Context.MODE_PRIVATE)
            // Never overwrite: if the account already has a scoped file, it is
            // newer than the legacy one by definition.
            if (target.all.isNotEmpty()) return@forEach

            val editor = target.edit()
            entries.forEach { (key, value) ->
                when (value) {
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Set<*> -> editor.putStringSet(key, value as Set<String>)
                }
            }
            editor.apply()
        }
    }
}

/**
 * Keyed on [UserSession.epoch] as well as the store name, so an account change
 * produces a fresh instance pointing at the new account's file. Without the
 * epoch, a screen that survived the switch would keep writing into the previous
 * account's store.
 */
@Composable
actual fun rememberPlatformSettings(name: String): PlatformSettings {
    val context = LocalContext.current
    val epoch = UserSession.epoch
    return remember(context, name, epoch) {
        PlatformSettings(PrefStores.open(context, name))
    }
}

actual fun platformSettings(name: String): PlatformSettings =
    PlatformSettings(PrefStores.open(requireAppContext(), name))

actual fun deviceSettings(): PlatformSettings =
    PlatformSettings(PrefStores.openDevice(requireAppContext()))

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
