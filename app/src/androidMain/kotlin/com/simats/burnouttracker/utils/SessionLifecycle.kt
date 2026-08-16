package com.simats.burnouttracker.utils

import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.simats.burnouttracker.data.StudySessionStore
import com.simats.burnouttracker.data.api.ApiClient
import com.simats.burnouttracker.data.models.PendingStop
import com.simats.burnouttracker.services.StudyTimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The one place a session starts and the one place it ends.
 *
 * Every step that used to be scattered across MainActivity, LoginScreen,
 * RegisterScreen and the logout button lives here, in a fixed order, so there
 * is a single answer to "what exactly happens when the account changes?"
 */
internal object SessionManager {

    /** Unique WorkManager names this app enqueues. Kept together so teardown cannot miss one. */
    private val UNIQUE_WORK = listOf(
        "SleepAnalysisWork",
        "RecommendationNotificationWork",
        "OptimizationNotificationWork"
    )

    /**
     * Brings the session up for [uid].
     *
     * [alreadySignedIn] is true only for the app-start call, where Firebase had
     * persisted the session before this launch. It decides whether legacy
     * pre-scoping data is adopted by this account — an account that was already
     * signed in when this build first ran is the one that wrote that data; an
     * account signing in afterwards is not.
     *
     * Step order matters and is not arbitrary:
     *  1. device-key migration, before anything reads onboarding flags;
     *  2. session identity, so the epoch has moved before any store is resolved;
     *  3. account scope, which sets the persisted uid that store resolution and
     *     the sleep/usage boundaries all key on;
     *  4. drop the previous account's in-memory state;
     *  5. repopulate from THIS account's own cache;
     *  6. re-derive alarms and workers from this account's settings;
     *  7. mark ready, which is what releases the UI to render values;
     *  8. restore this account's nights from the backend, off the critical path.
     */
    fun begin(context: Context, uid: String, alreadySignedIn: Boolean) {
        val app = context.applicationContext

        PrefStores.migrateDeviceKeys(app)

        val changed = UserSession.beginSession(uid)

        AccountScope.syncActiveAccount(app, uid, alreadySignedIn)

        // Opens this account's usage-ownership interval for today. Must follow
        // syncActiveAccount, which is what makes `uid` the active account the
        // interval is recorded against. Any interval left open by a previous
        // session — a crash, a force-stop — is closed first, so no span can keep
        // accruing to a session that already ended.
        AccountScope.openUsageInterval(app, uid)

        if (changed) {
            // In-memory only. Nothing on disk is destroyed: the previous
            // account's values live in its own files and are still there when
            // it signs back in.
            AppData.reset()
            UserProfile.clear()
        }

        UserProfile.loadFromCache()

        if (changed) {
            // Both re-read the now-current account's settings. The alarms in
            // particular are already registered with AlarmManager under the
            // previous account's plan, so re-deriving is required — clearing or
            // switching the store alone would leave them running.
            ActionPlanScheduler.scheduleAlarms(app)
            NotificationHelper.updateWorkers(app)
        }

        UserSession.markReady()

        // LAST, and deliberately not awaited. This is a network round trip; the
        // UI must not wait on it to render, and a device that is offline at sign
        // -in has to reach a working session anyway. Room is observed through
        // Flows, so restored nights appear as they land without anything here
        // having to notify a screen.
        //
        // After markReady rather than before it for the same reason: ordering
        // this ahead of the release would make every sign-in block on the
        // network. Correctness does not depend on the position — the restore
        // re-reads the active account itself and stamps every row with `uid`.
        restoreSleepHistory(app, uid)
    }

    /**
     * Stops the timer service and takes durable custody of any running study
     * session, using the identity that is still current.
     *
     * Runs before anything else in [end] because both halves depend on the
     * outgoing account: the service reads AppData for the session it is
     * displaying, and the handoff has to stamp the pending stop with the uid
     * that OWNS the session — not with whoever signs in next.
     *
     * The stop request itself is dispatched but never awaited. Logout must not
     * block on the network, and it does not need to: the pending-stop record is
     * written to disk first, so a failed or interrupted request is retried by
     * flushPendingStops() the next time this account opens the study screen. The
     * durable record, not the request, is what guarantees the session is not
     * lost.
     */
    private fun endStudySession(app: Context) {
        // Stop the foreground service first, while the identity it is rendering
        // still exists. START_STICKY means a service left running would also be
        // restarted by the system after logout.
        try {
            app.stopService(Intent(app, StudyTimerService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val uid = FirebaseTokenProvider.currentUid()
        if (uid.isBlank()) return

        // Resolves through PrefStores to `study_tracker__u_<outgoing uid>`:
        // AccountScope still holds the outgoing account here, because it is only
        // overwritten by the next begin(). SettingsScreen also calls this before
        // signOut() for exactly that reason.
        val settings = platformSettings("study_tracker")
        val active = StudySessionStore.readActive(settings, uid) ?: return

        // Not this account's session: leave it entirely alone. Signing out of B
        // must not touch, stop or discard a session belonging to A.
        if (!active.belongsTo(uid)) return

        val sessionId = active.sessionId
        if (sessionId == null) {
            // Never received a server id, so there is nothing to stop remotely
            // and nothing that could be orphaned. Release the slot; no duration
            // is invented for it.
            StudySessionStore.clearActive(settings)
            return
        }

        // Durable custody BEFORE the slot is released, and before sign-out.
        StudySessionStore.handOffStop(
            settings,
            PendingStop(
                sessionId = sessionId,
                ownerUid = uid,
                subject = active.subject,
                startedAt = active.startedAt,
                queuedAt = System.currentTimeMillis()
            )
        )
        println("[STUDY] logout: session $sessionId handed to durable stop queue for $uid.")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.stopStudySession(sessionId)
                if (resp.success) {
                    StudySessionStore.removeStop(settings, sessionId)
                    println("[STUDY] logout: session $sessionId STOPPED and confirmed.")
                }
                // A failure is deliberately not marked here: the record already
                // sits in the queue in STOPPING, which is retryable, and this
                // coroutine may outlive the settings instance it was built from.
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Fire-and-forget restore of [uid]'s nights from the backend.
     *
     * Failures are swallowed on purpose: this is a best-effort backfill of data
     * the account already owns elsewhere, and nothing about the session depends
     * on it succeeding. It is never destructive — it only ever inserts dates
     * Room does not already hold for this account.
     */
    private fun restoreSleepHistory(app: Context, uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // BEFORE the restore, and before any sync can be in flight: a row
                // left SYNCING by a process death is relabelled FAILED so it is
                // both retryable and honestly described. Nothing is re-sent here.
                SleepMonitoringEngine(app).reclaimStrandedSyncs(uid)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                SleepHistoryRestore.restoreFor(app, uid)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Takes the session down, in an order that cannot orphan a study session.
     *
     * The persisted active uid in [AccountScope] is deliberately left in place:
     * it is what store resolution keys on, and clearing it would strand the
     * outgoing account's files under a namespace nothing can find again. The
     * next [begin] overwrites it, and signing back in as the same account finds
     * everything intact.
     *
     * ORDER, and why each step is where it is:
     *  1. capture the outgoing uid — every step below needs the OLD identity,
     *     and by the time Firebase has signed out it is gone;
     *  2. stop StudyTimerService, using that identity, BEFORE it disappears. The
     *     service is a foreground service driven by AppData; signing out first
     *     left it running with a live notification against an account that no
     *     longer existed;
     *  3. hand the running session off to the durable stop queue, so it is
     *     retryable rather than lost. Nothing is fabricated: if the session has
     *     no server id there is nothing to stop, and the record is simply
     *     released;
     *  4. clear the active-session slot — safe only because step 3 has already
     *     taken durable custody of it;
     *  5. clear account-scoped runtime state;
     *  6. end the session identity last, because everything above reads it.
     */
    fun end(context: Context) {
        val app = context.applicationContext

        // Close this account's usage-ownership interval FIRST, while the outgoing
        // identity is still current. Left open, every minute the next account
        // spends on the device would be attributed to this one — which is exactly
        // the mis-attribution the interval model exists to prevent.
        AccountScope.closeUsageInterval(app, FirebaseTokenProvider.currentUid())

        // 1 + 2 + 3 + 4. Study session teardown under the OUTGOING identity.
        endStudySession(app)

        val workManager = WorkManager.getInstance(app)
        UNIQUE_WORK.forEach { name ->
            try {
                workManager.cancelUniqueWork(name)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        ActionPlanScheduler.cancelAll(app)

        AppData.reset()
        UserProfile.clear()

        // Last: everything above may need to read the outgoing identity.
        UserSession.endSession()
    }
}

actual fun beginUserSession(uid: String) {
    SessionManager.begin(requireAppContext(), uid, alreadySignedIn = false)
}

actual fun endUserSession() {
    SessionManager.end(requireAppContext())
}
