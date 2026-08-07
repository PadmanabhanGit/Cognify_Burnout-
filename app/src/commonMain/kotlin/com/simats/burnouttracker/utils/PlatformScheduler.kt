package com.simats.burnouttracker.utils

import androidx.compose.runtime.Composable

expect fun triggerActionPlanSync()

expect fun scheduleStudyTimer(durationMins: Int)
expect fun cancelStudyTimer()
