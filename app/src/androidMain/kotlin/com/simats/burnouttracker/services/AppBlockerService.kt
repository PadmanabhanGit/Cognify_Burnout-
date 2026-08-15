package com.simats.burnouttracker.services

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.simats.burnouttracker.BlockerActivity
import com.simats.burnouttracker.utils.AppUsageClassifier
import com.simats.burnouttracker.utils.UsageStatsHelper

class AppBlockerService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            // Fast check if limits are enabled
            // Scoped to the active account, so one user's app limits are never
            // enforced against another's session.
            val prefs = com.simats.burnouttracker.utils.PrefStores.open(this, "action_plan")
            val limitSocial = prefs.getBoolean("limit_social", false)
            val limitStreaming = prefs.getBoolean("limit_streaming", false)
            val limitGaming = prefs.getBoolean("limit_gaming", false)

            if (!limitSocial && !limitStreaming && !limitGaming) return

            val pm = packageManager
            val appName = try {
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            } catch (e: Exception) {
                return
            }

            val classifier = AppUsageClassifier(this)
            val category = classifier.classify(appName)

            var isBlocked = false

            if (category == "Social Media" && limitSocial) {
                val socialLimitMins = prefs.getInt("social_limit", 60)
                val helper = UsageStatsHelper(this)
                if (helper.hasUsageStatsPermission()) {
                    val stats = helper.fetchDailyUsage()
                    if (stats.socialHours * 60 > socialLimitMins) {
                        isBlocked = true
                    }
                }
            } else if (category == "Streaming" && limitStreaming) {
                val streamingLimitMins = prefs.getInt("streaming_limit", 120)
                val helper = UsageStatsHelper(this)
                if (helper.hasUsageStatsPermission()) {
                    val stats = helper.fetchDailyUsage()
                    if (stats.streamingHours * 60 > streamingLimitMins) {
                        isBlocked = true
                    }
                }
            } else if (category == "Gaming" && limitGaming) {
                val gamingLimitMins = prefs.getInt("gaming_limit", 60)
                val helper = UsageStatsHelper(this)
                if (helper.hasUsageStatsPermission()) {
                    val stats = helper.fetchDailyUsage()
                    if (stats.gamingHours * 60 > gamingLimitMins) {
                        isBlocked = true
                    }
                }
            }

            if (isBlocked) {
                val intent = Intent(this, BlockerActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra("BLOCKED_APP_NAME", appName)
                }
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() {
        // No implementation needed
    }
}
