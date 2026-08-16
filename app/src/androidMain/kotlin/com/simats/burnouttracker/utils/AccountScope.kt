package com.simats.burnouttracker.utils

import android.content.Context
import com.simats.burnouttracker.data.database.SleepDatabase
import com.simats.burnouttracker.data.models.UsageOwnership
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    private const val KEY_USAGE_INTERVALS_PREFIX = "usage_intervals_"

    /** How long per-day ownership records are kept. Far beyond anything the app reads back. */
    private const val DAY_WINDOW_RETENTION_DAYS = 60

    @Volatile
    private var cachedDataStart: Long? = null

    /**
     * The (uid, day) whose usage interval THIS PROCESS has already opened.
     *
     * Deliberately process-scoped and never persisted: its whole purpose is to
     * distinguish a duplicate begin() within one launch from a fresh start after
     * the process died. Persisting it would erase exactly that distinction.
     */
    @Volatile
    private var openedThisProcessFor: String? = null

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

    // ── Per-day usage OWNERSHIP INTERVALS ────────────────────────────────────

    /**
     * The spans of today the ACTIVE account was signed in for, clipped to
     * `[windowStart, windowEnd]`.
     *
     * Replaces the earlier single-start model, which mis-attributed usage as soon
     * as two accounts shared a day: a lower bound cannot exclude a span in the
     * MIDDLE of a window, so an account that signed in first counted everything
     * a later account did. See [UsageOwnership].
     *
     * [windowStart] should already carry the account-lifetime clamp, so both
     * bounds apply — never before the account existed, and only while it was
     * signed in. An empty result means this account owns none of the window, and
     * the caller must report no usage rather than falling back to the raw window.
     */
    fun usageIntervals(context: Context, windowStart: Long, windowEnd: Long): List<UsageOwnership.Interval> {
        val prefs = store(context)
        val uid = prefs.getString(KEY_ACTIVE_UID, null) ?: return emptyList()
        val stored = UsageOwnership.parse(prefs.getString(UsageOwnership.key(uid, todayKey()), null))
        val resolved = UsageOwnership.resolve(stored, System.currentTimeMillis())
        return UsageOwnership.clip(resolved, windowStart, windowEnd)
    }

    /** Local calendar day as `yyyy-MM-dd`, matching the rest of the app's date keys. */
    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    /**
     * [uid]'s session begins now: opens an ownership interval for today.
     *
     * Any interval left open by a previous session is closed first — a crash or
     * force-stop never produces a clean logout, and an interval left open would
     * otherwise keep accruing every later minute to that session forever.
     */
    fun openUsageInterval(context: Context, uid: String) {
        if (uid.isBlank()) return
        val prefs = store(context)
        val key = UsageOwnership.key(uid, todayKey())

        // PROCESS-SCOPED, not timestamp-scoped. SessionManager.begin runs twice
        // per launch (MainActivity and the auth-state listener), and a second
        // call in the SAME process is a duplicate, not a new session — opening
        // again would fragment the record on every start.
        //
        // Deciding this by timestamp instead does not work and was tried: "the
        // previous span ended exactly now" is true both for that duplicate call
        // AND for a stranded span being capped after a crash, so merging on it
        // extended the crash gap instead of closing it. Process identity is what
        // actually separates the two cases — a crash means a NEW process, where
        // this flag is null and the stale span is correctly capped.
        if (openedThisProcessFor == key) return

        // Reconcile everything left open by anyone, before claiming time here.
        reconcileOpenIntervals(prefs, exceptKey = key, now = System.currentTimeMillis())

        val updated = UsageOwnership.openSession(
            UsageOwnership.parse(prefs.getString(key, null)),
            System.currentTimeMillis()
        )
        prefs.edit().putString(key, UsageOwnership.encode(updated)).apply()
        openedThisProcessFor = key
        pruneOldDayWindows(prefs)
    }

    /**
     * Closes every open interval that is not [exceptKey], at the correct boundary.
     *
     * Two situations this exists for, both of which the per-account close alone
     * cannot handle because it only ever looks at ONE key:
     *
     *  - ANOTHER ACCOUNT LEFT A SPAN OPEN. Switching accounts without a clean
     *    logout — a killed process, a crash, any path that misses
     *    [closeUsageInterval] — leaves A's span open. Left alone, A's next
     *    sign-in would cap that span at THAT moment and swallow the whole
     *    stretch B was signed in for, which is precisely the mis-attribution the
     *    interval model exists to prevent. Only one account can own a moment, so
     *    a new session closes everyone else's open span at `now`.
     *
     *  - A SPAN IS OPEN ON AN EARLIER DAY. A session running across midnight
     *    belongs partly to each date, and [closeUsageInterval] resolves
     *    `todayKey()` — so after midnight it looks for a key that does not exist
     *    and silently leaves yesterday's span open forever. An earlier day's span
     *    is therefore closed at the END of ITS OWN day, never at `now`, which
     *    both terminates it and keeps each date's total inside that date.
     */
    private fun reconcileOpenIntervals(
        prefs: android.content.SharedPreferences,
        exceptKey: String,
        now: Long
    ) {
        val today = todayKey()
        val editor = prefs.edit()
        var changed = false

        prefs.all.keys
            .filter { it.startsWith(KEY_USAGE_INTERVALS_PREFIX) && it != exceptKey }
            .forEach { key ->
                val existing = UsageOwnership.parse(prefs.getString(key, null))
                if (existing.none { it.end == UsageOwnership.OPEN }) return@forEach

                val dateKey = key.takeLast(10)
                // An earlier day closes at its own midnight; today closes now.
                val boundary = if (dateKey == today) now else endOfDayMillis(dateKey)
                if (boundary <= 0L) return@forEach

                editor.putString(key, UsageOwnership.encode(UsageOwnership.closeSession(existing, boundary)))
                changed = true
            }

        if (changed) editor.apply()
    }

    /** Last instant of [dateKey] (`yyyy-MM-dd`) in local time, or 0 if unparseable. */
    private fun endOfDayMillis(dateKey: String): Long = try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateKey)
        if (parsed == null) 0L else Calendar.getInstance().apply {
            time = parsed
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    } catch (e: Exception) {
        0L
    }

    /**
     * [uid]'s session ends now: closes its open ownership interval for today.
     *
     * Must run BEFORE the identity changes, or the span would be left open and
     * the next account's activity would be attributed to this one.
     */
    fun closeUsageInterval(context: Context, uid: String) {
        if (uid.isBlank()) return
        val prefs = store(context)
        val key = UsageOwnership.key(uid, todayKey())
        // Cleared even when there is nothing to close, so signing back in as this
        // account later in the same process opens a genuinely new span rather
        // than being suppressed as a duplicate.
        if (openedThisProcessFor == key) openedThisProcessFor = null

        // Closes this account's open span wherever it is, not only under today's
        // key. A session that began before midnight is stored under YESTERDAY's
        // key, so resolving `todayKey()` alone would find nothing and leave it
        // open forever — the span would then never terminate and yesterday's
        // total would keep growing. Each date's span closes at its own end.
        val today = todayKey()
        val now = System.currentTimeMillis()
        val editor = prefs.edit()
        var changed = false

        prefs.all.keys
            .filter { it.startsWith(UsageOwnership.key(uid, "")) }
            .forEach { k ->
                val existing = UsageOwnership.parse(prefs.getString(k, null))
                if (existing.none { it.end == UsageOwnership.OPEN }) return@forEach
                val dateKey = k.takeLast(10)
                val boundary = if (dateKey == today) now else endOfDayMillis(dateKey)
                if (boundary <= 0L) return@forEach
                editor.putString(k, UsageOwnership.encode(UsageOwnership.closeSession(existing, boundary)))
                changed = true
            }

        if (changed) editor.apply()
    }

    /**
     * Drops day windows older than [DAY_WINDOW_RETENTION_DAYS].
     *
     * One key per account per day would otherwise accumulate forever. Retention
     * is far longer than any window the app reads back, so this cannot remove a
     * key still in use.
     */
    private fun pruneOldDayWindows(prefs: android.content.SharedPreferences) {
        val cutoff = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -DAY_WINDOW_RETENTION_DAYS)
        }
        val oldest = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cutoff.time)

        val stale = prefs.all.keys.filter { k ->
            k.startsWith(KEY_USAGE_INTERVALS_PREFIX) &&
                k.takeLast(10).let { it.length == 10 && !UsageOwnership.shouldRetain(it, oldest) }
        }
        if (stale.isEmpty()) return
        prefs.edit().apply { stale.forEach { remove(it) } }.apply()
    }

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
