package com.simats.burnouttracker.utils

import kotlin.math.roundToInt

object ProductivityPredictor {
    fun calculate(features: BurnoutFeatures, burnoutScore: Float, sleepHours: Float): Int {
        var score = 50f // Base neutral score

        // Positive Factors
        // Reward study/productivity hours (+8 per hour, max +40)
        score += (features.productivityHours * 8f).coerceAtMost(40f)

        // Sleep Time Bonus/Penalty
        if (sleepHours in 7.0..9.0) {
            score += 15f // Healthy sleep bonus
        } else if (sleepHours < 5.0 || sleepHours > 10.0) {
            score -= 10f // Poor sleep penalty
        }

        // Negative Factors
        // Penalize distractions (-5 per hour, max -20)
        val distractionHours = features.socialHours + features.gamingHours + features.streamingHours
        score -= (distractionHours * 5f).coerceAtMost(20f)
        
        // Burnout Penalty (-0.5 points for every burnout point above 50)
        if (burnoutScore > 50f) {
            val burnoutExcess = burnoutScore - 50f
            score -= burnoutExcess * 0.5f
        }

        // Penalize context switching (high app switch count) (-1 per 10 switches over 50, max -15)
        if (features.appSwitchCount > 50) {
            val switchesOver = features.appSwitchCount - 50
            score -= ((switchesOver / 10f) * 1f).coerceAtMost(15f)
        }

        return score.roundToInt().coerceIn(0, 100)
    }
}
