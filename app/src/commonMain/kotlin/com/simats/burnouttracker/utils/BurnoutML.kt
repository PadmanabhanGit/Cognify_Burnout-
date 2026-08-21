package com.simats.burnouttracker.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable

data class BurnoutFeatures(
    val socialHours: Float,
    val gamingHours: Float,
    val streamingHours: Float,
    val productivityHours: Float,
    val totalScreenTime: Float,
    val nightUsageHours: Float,
    val appSwitchCount: Int,
    val averageSessionMinutes: Float,
    val topApps: List<DetailedAppUsage> = emptyList(),
    /** Apps ordered by most recent foreground open today, not by total time — so a
     *  just-opened app with little accumulated usage still shows up. */
    val recentApps: List<DetailedAppUsage> = emptyList(),
    /** Apps that don't fit any of the four buckets above (Maps, Chrome, Calculator,
     *  ...) — previously folded into totalScreenTime with no visible row of their
     *  own, so the total looked unexplained next to the four category rows. */
    val othersHours: Float = 0f
)

data class DetailedAppUsage(
    val name: String,
    val packageName: String,
    val category: String,
    val hours: Float,
    val color: androidx.compose.ui.graphics.Color,
    /** Epoch millis of this app's most recent foreground open today, 0 if unknown. */
    val lastUsedAt: Long = 0L
)

data class BurnoutInsights(
    val studyLoad: Int,
    /** Null when no night window is in scope for this account yet (see [UsageStatsHelper.hasNightWindowData]) — not a measured zero. */
    val sleepQuality: Int?,
    val stressLevel: Int,
    val recoveryTime: Int
)

data class WellbeingMetrics(
    val focus: Int,
    val stress: Int,
    val mood: Int,
    val energy: Int,
    val sleep: Int,
    val studyLoad: Int
)

object InsightGenerator {
    fun generate(
        features: BurnoutFeatures,
        burnoutScore: Float,
        nightDataAvailable: Boolean = true
    ): BurnoutInsights {
        val studyLoad = (features.productivityHours * 15).toInt().coerceIn(0, 100)
        val sleepQuality = if (nightDataAvailable)
            (100 - features.nightUsageHours * 15).toInt().coerceIn(0, 100)
        else null
        val stressLevel = burnoutScore.toInt().coerceIn(0, 100)
        val recoveryTime = (100 - burnoutScore).toInt().coerceIn(0, 100)

        return BurnoutInsights(
            studyLoad,
            sleepQuality,
            stressLevel,
            recoveryTime
        )
    }
}

object WellbeingGenerator {
    fun generate(
        burnoutScore: Float,
        insights: BurnoutInsights
    ): WellbeingMetrics {
        val stress = burnoutScore.toInt()
        val focus = (100 - burnoutScore).toInt()
        val mood = (100 - burnoutScore * 0.8f).toInt()
        val energy = (100 - burnoutScore * 0.7f).toInt()
        // No radar-chart convention exists for "unknown axis" — fall back to 0
        // rather than repeat the same "no data reads as a real value" mistake
        // this table exists to avoid in the Contributing Factors bar.
        val sleep = insights.sleepQuality ?: 0
        val study = insights.studyLoad

        return WellbeingMetrics(
            focus,
            stress,
            mood,
            energy,
            sleep,
            study
        )
    }
}

object RecommendationEngine {
    fun generate(burnoutScore: Float): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        when {
            burnoutScore >= 75 -> {
                recommendations.add(Recommendation("Limit Screen Time", "Keep entertainment apps under 1h to reduce severe burnout risk.", Icons.Default.Block))
                recommendations.add(Recommendation("Take regular breaks", "Use the 20-20-20 rule to reduce eye strain and stress.", Icons.Default.SelfImprovement))
                recommendations.add(Recommendation("Digital Detox", "Consider 2 hours of no-phone time before bedtime.", Icons.Default.Bedtime))
            }
            burnoutScore >= 50 -> {
                recommendations.add(Recommendation("Balanced Usage", "Limit social media to within 2 hours daily to boost mood.", Icons.Default.FavoriteBorder))
                recommendations.add(Recommendation("Smart Scheduling", "Schedule gaming after study sessions as a reward, not before.", Icons.Default.Schedule))
                recommendations.add(Recommendation("Usage Alerts", "Enable notifications to stay aware of time passing.", Icons.Default.NotificationsNone))
            }
            else -> {
                recommendations.add(Recommendation("Healthy Habit", "Continue maintaining your balanced productivity routine.", Icons.Default.CheckCircle))
                recommendations.add(Recommendation("Stay Consistent", "Keep up with your current sleep and focus levels.", Icons.Default.Star))
            }
        }
        return recommendations
    }
}

data class Recommendation(val title: String, val subtitle: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

interface BurnoutPredictor {
    fun predict(features: BurnoutFeatures): Float
}

@Composable
expect fun rememberBurnoutPredictor(): BurnoutPredictor
