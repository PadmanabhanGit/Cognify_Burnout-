package com.simats.burnouttracker.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private var appContext: Context? = null

fun initAppContext(context: Context) {
    appContext = context
}

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
