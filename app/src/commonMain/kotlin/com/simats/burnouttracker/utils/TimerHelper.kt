package com.simats.burnouttracker.utils

import androidx.compose.runtime.Composable

@Composable
expect fun rememberTimerHelper(): TimerHelper

interface TimerHelper {
    fun startTimer(sessionName: String)
    fun stopTimer()
}

expect fun getCurrentTimeMillis(): Long
