package com.simats.burnouttracker.utils

import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import com.simats.burnouttracker.services.AppMonitoringService

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
     *  7. mark ready, which is what releases the UI to render values.
     */
    fun begin(context: Context, uid: String, alreadySignedIn: Boolean) {
        val app = context.applicationContext

        PrefStores.migrateDeviceKeys(app)

        val changed = UserSession.beginSession(uid)

        AccountScope.syncActiveAccount(app, uid, alreadySignedIn)

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
    }

    /**
     * Takes the session down.
     *
     * The persisted active uid in [AccountScope] is deliberately left in place:
     * it is what store resolution keys on, and clearing it would strand the
     * outgoing account's files under a namespace nothing can find again. The
     * next [begin] overwrites it, and signing back in as the same account finds
     * everything intact.
     */
    fun end(context: Context) {
        val app = context.applicationContext

        val workManager = WorkManager.getInstance(app)
        UNIQUE_WORK.forEach { name ->
            try {
                workManager.cancelUniqueWork(name)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            app.stopService(Intent(app, AppMonitoringService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
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
