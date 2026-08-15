package com.simats.burnouttracker.utils

import androidx.compose.runtime.Composable

expect fun triggerActionPlanSync()

expect fun scheduleStudyTimer(durationMins: Int)
expect fun cancelStudyTimer()

/**
 * True when the existing AppBlockerService (an AccessibilityService) is actually
 * enabled by the user in system settings. Read-only status check — it grants
 * nothing and starts nothing. The Action Plan uses it so the UI never claims app
 * blocking that the device cannot currently perform.
 */
expect fun isAppBlockingEnabled(): Boolean

/** Opens the system Accessibility settings screen so the user can enable it themselves. */
expect fun openAccessibilitySettings()
