package com.simats.burnouttracker.utils

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.simats.burnouttracker.services.StudyTimerService

@Composable
actual fun rememberTimerHelper(): TimerHelper {
    val context = LocalContext.current
    return remember(context) { AndroidTimerHelper(context) }
}

class AndroidTimerHelper(private val context: Context) : TimerHelper {
    override fun startTimer(sessionName: String) {
        val intent = Intent(context, StudyTimerService::class.java).apply {
            action = "START"
            putExtra("SESSION_NAME", sessionName)
        }
        context.startService(intent)
    }

    override fun stopTimer() {
        val intent = Intent(context, StudyTimerService::class.java).apply {
            action = "STOP"
        }
        context.startService(intent)
    }
}

actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()
