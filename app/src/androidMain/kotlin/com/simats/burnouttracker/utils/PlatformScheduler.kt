package com.simats.burnouttracker.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Internal, not private: the session lifecycle and the non-composable settings
// accessors need the application context too, and threading a Context parameter
// through commonMain `expect` signatures would leak Android types into shared code.
internal var appContext: Context? = null

fun initAppContext(context: Context) {
    appContext = context.applicationContext
}

/**
 * The application context, or throws.
 *
 * Every caller runs after MainActivity.onCreate has installed it, so a null here
 * is a wiring bug rather than a condition to degrade around silently.
 */
internal fun requireAppContext(): Context =
    appContext ?: throw IllegalStateException(
        "initAppContext() has not run yet — the application context is required for user-scoped storage."
    )

actual fun triggerActionPlanSync() {
    appContext?.let {
        ActionPlanScheduler.scheduleAlarms(it)
    }
}

actual fun scheduleStudyTimer(durationMins: Int) {
    appContext?.let {
        ActionPlanScheduler.scheduleStudyTimer(it, durationMins)
    }
}

actual fun cancelStudyTimer() {
    appContext?.let {
        ActionPlanScheduler.cancelStudyTimer(it)
    }
}

/**
 * Reads Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES and looks for this app's
 * AppBlockerService. Purely a status read — no permission is requested or
 * granted here, and nothing is started. Returns false on any error so the UI
 * degrades to "not enabled" rather than over-claiming.
 */
actual fun isAppBlockingEnabled(): Boolean {
    val ctx = appContext ?: return false
    return try {
        val enabled = android.provider.Settings.Secure.getString(
            ctx.contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val target = "${ctx.packageName}/${com.simats.burnouttracker.services.AppBlockerService::class.java.name}"
        enabled.split(':').any { it.equals(target, ignoreCase = true) }
    } catch (e: Exception) {
        false
    }
}

actual fun openAccessibilitySettings() {
    val ctx = appContext ?: return
    try {
        ctx.startActivity(
            android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
