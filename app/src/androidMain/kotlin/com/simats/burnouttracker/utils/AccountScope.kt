package com.simats.burnouttracker.utils

import android.content.Context
import com.simats.burnouttracker.data.database.SleepDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Account data isolation.
 *
 * THE PROBLEM
 * Three stores in this app are device-level, not account-level:
 *   1. Android UsageStats  — an OS database that cannot and must not be cleared.
 *   2. Room `sleep_database` — one file per install, no userId column.
 *   3. SharedPreferences caches (study hours, burnout history, monitoring).
 * So a newly created account previously saw the previous account's study, sleep,
 * mood, productivity, burnout score and app-usage history.
 *
 * THE FIX
 * Each account gets a data-start timestamp, recorded the first time that account
 * becomes active on this device. Every UsageStats query clamps its lower bound to
 * that timestamp, so a new account can only ever see usage recorded AFTER it was
 * created. The OS database itself is never modified.
 *
 * SharedPreferences caches are no longer CLEARED on an account change. They are
 * resolved to a separate physical file per account instead (see [PrefStores]),
 * which isolates them the same way ownerUid isolates sleep rows: structurally,
 * and without destroying the outgoing account's data. Clearing had the same flaw
 * deleting sleep rows did — A → B → A wiped A's cache on the way out — plus a
 * sharper one, since the clear ran AFTER the active uid had already been moved
 * to the incoming account.
 *
 * Room sleep sessions are NOT cleared. They carry a SleepSession.ownerUid and
 * every query filters on the active account, which isolates accounts without
 * destroying anything: A → B → A used to wipe A's nights on the way out and
 * again on the way back, leaving A permanently empty. Ownership makes the
 * switch non-destructive in both directions.
 *
 * PRESERVING EXISTING ACCOUNTS
 * On the first run after this code ships, no active account is recorded. That
 * case is treated as MIGRATION, not an account switch: nothing is cleared, so an
 * existing user's history is fully preserved. Clearing only ever happens on a
 * genuine uid change after an account has already been recorded.
 *
 * Being first to be adopted is NOT by itself grounds for an unclamped data start.
 * Only an account Firebase had already persisted before this build ran can have
 * written the history already on the device, so only that account is given one.
 * An account that signs in during the session is clamped from the moment it
 * appears, whether or not it happens to be the first this device sees. Without
 * that distinction a newly created account became the single account on the
 * device able to read the whole UsageStats history, and re-derived the previous
 * user's nights as its own.
 */
object AccountScope {
    private const val STORE = "account_scope"
    private const val KEY_ACTIVE_UID = "active_uid"
    private const val KEY_DATA_START_PREFIX = "data_start_"

    @Volatile
    private var cachedDataStart: Long? = null

    private fun store(context: Context) =
        context.applicationContext.getSharedPreferences(STORE, Context.MODE_PRIVATE)

    /**
     * Lower bound for every UsageStats query, in epoch millis.
     *
     * 0 means "no clamp" — used for migrated accounts so their existing behaviour
     * is unchanged. Cached because UsageStatsHelper is called on short polling
     * loops; the cache is invalidated whenever the active account changes.
     */
    fun dataStartMillis(context: Context): Long {
        cachedDataStart?.let { return it }
        val prefs = store(context)
        val uid = prefs.getString(KEY_ACTIVE_UID, null) ?: return 0L
        val v = prefs.getLong(KEY_DATA_START_PREFIX + uid, 0L)
        cachedDataStart = v
        return v
    }

    /**
     * Firebase UID of the account currently active on this device, or "" if no
     * account has been adopted yet.
     *
     * Used as the ownership key for Room sleep sessions. "" is the same sentinel
     * SleepSession.ownerUid uses for not-yet-claimed rows, so a device that has
     * never adopted an account still sees exactly its own pre-existing nights.
     */
    fun activeUid(context: Context): String =
        store(context).getString(KEY_ACTIVE_UID, null) ?: ""

    /** Convenience for the UsageStats callers: never returns a bound earlier than [windowStart]. */
    fun clampWindowStart(context: Context, windowStart: Long): Long =
        maxOf(windowStart, dataStartMillis(context))

    /**
     * Call whenever the signed-in account may have changed (app start, auth state
     * change). A null [uid] (signed out) is ignored: signing out and back in as
     * the same account must not wipe anything.
     *
     * [alreadySignedIn] must be true only for the app-start call, where [uid]
     * comes from a session Firebase had already persisted before this launch, and
     * false for the auth-state listener, where the account signed in during this
     * session. It decides one thing: whether pre-ownership sleep rows are claimed
     * on first adoption. An account that was already signed in when this build
     * first ran is the account that recorded them; an account that signs in
     * afterwards is not, and must not inherit them.
     */
    fun syncActiveAccount(context: Context, uid: String?, alreadySignedIn: Boolean = false) {
        if (uid.isNullOrBlank()) return
        val prefs = store(context)
        val previous = prefs.getString(KEY_ACTIVE_UID, null)

        if (previous == uid) return // same account, nothing to do

        if (previous == null) {
            // FIRST ADOPTION on this device. Two very different situations reach
            // here, and they must not be treated alike:
            //
            //  - An account Firebase had ALREADY persisted before this build ran
            //    (alreadySignedIn). The history in UsageStats is its own, so it
            //    gets no clamp and keeps everything. This is the migration case.
            //
            //  - An account signing in DURING this session, on a device that had
            //    not yet adopted anyone. The history in UsageStats is not its own
            //    — it belongs to whoever used the phone before — so it is clamped
            //    from now, exactly like any other new account.
            //
            // Granting 0 unconditionally is what broke isolation in practice: a
            // freshly created account adopted first became the one account on the
            // device with NO lower bound, so every UsageStats query it made saw
            // the whole device history. SleepMonitoringEngine then re-derived the
            // previous user's nights under the new uid — the same night appearing
            // once per account with identical values — and the new account's
            // dashboard showed a stranger's sleep and app usage as its own.
            //
            // The `alreadySignedIn` distinction was already being drawn just
            // below for legacy rows and preference files; the clamp simply was
            // not covered by it. Same rule, same reason, now applied to all three.
            val dataStart = if (alreadySignedIn) 0L else System.currentTimeMillis()

            prefs.edit()
                .putString(KEY_ACTIVE_UID, uid)
                .putLong(KEY_DATA_START_PREFIX + uid, dataStart)
                .apply()
            cachedDataStart = dataStart
            // Sleep sessions recorded before ownership existed belong to the
            // account that was already signed in when this build first ran —
            // adopt them for it, or the ownerUid filter would hide an existing
            // user's own history from them. If instead this account signed in
            // during this session, those rows are someone else's: they are left
            // unowned, which shows them to nobody rather than to the wrong
            // account.
            if (alreadySignedIn) {
                claimUnownedSleepSessions(context, uid)
                // Same rule, same reason, for the pre-scoping SharedPreferences
                // files: they were written by whoever was signed in when this
                // build first ran. An account signing in later did not write
                // them and must not inherit them, so they are left where they
                // are — visible to nobody rather than to the wrong person.
                PrefStores.adoptLegacyStores(context, uid)
            }
            return
        }

        // GENUINE ACCOUNT CHANGE.
        // Record a data start for this account the first time we see it. An
        // account returning to this device keeps its original start rather than
        // having it pushed forward.
        val existingStart = prefs.getLong(KEY_DATA_START_PREFIX + uid, -1L)
        val dataStart = if (existingStart >= 0L) existingStart else System.currentTimeMillis()

        prefs.edit()
            .putString(KEY_ACTIVE_UID, uid)
            .putLong(KEY_DATA_START_PREFIX + uid, dataStart)
            .apply()
        cachedDataStart = dataStart

        // Nothing is cleared here. Isolation for local caches is structural now
        // (per-account preference files) and for sleep rows is ownership-based,
        // so an account change is purely a change of which data is addressed —
        // it destroys nothing belonging to the outgoing account.
        //
        // Dropping the previous account's IN-MEMORY state and re-deriving alarms
        // and workers from the incoming account's settings is SessionManager's
        // job, which owns the whole ordered transition. Doing it from here as
        // well would mean two places racing to define what an account change is.
    }

    /**
     * Attaches pre-ownership sleep rows to [uid]. Only ever called for the first
     * account this device adopts; rows already owned are untouched, so a night
     * can never move between accounts.
     */
    private fun claimUnownedSleepSessions(context: Context, uid: String) {
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SleepDatabase.getDatabase(app).sleepDao().claimUnownedSessions(uid)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
