// ─── Data Models for the Mental Health & Productivity Tracker API ──────────
// Copy this file into your Android project under:
//   app/src/main/java/com/yourpackage/data/models/

package com.burnouttracker.data.models

import com.google.gson.annotations.SerializedName

// ─── Auth ──────────────────────────────────────────────────────────────────────

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String
)

data class AuthResponse(
    val success: Boolean,
    val message: String,
    val token: String?,
    val user: UserData?
)

data class UserData(
    val id: String,
    val fullName: String,
    val email: String,
    val avatarUrl: String?
)

data class ProfileResponse(
    val success: Boolean,
    val user: UserData?
)

// ─── Study Sessions ────────────────────────────────────────────────────────────

data class StartSessionRequest(
    val subject: String,
    val notes: String? = null
)

data class StudySession(
    @SerializedName("_id") val id: String,
    val userId: String,
    val subject: String,
    val duration: Int,          // minutes
    val startTime: String,
    val endTime: String?,
    val isActive: Boolean,
    val notes: String?
)

data class StudySessionResponse(
    val success: Boolean,
    val message: String?,
    val session: StudySession?
)

data class StudyWeeklyStats(
    val totalMinutes: Int,
    val totalHours: Double,
    val sessionsCount: Int,
    val dailyTotals: Map<String, Int>,
    val subjectBreakdown: Map<String, Int>
)

data class StudyWeeklyResponse(
    val success: Boolean,
    val stats: StudyWeeklyStats?
)

data class StudyMonthlyStats(
    val dailyTotals: Map<String, Int>
)

data class StudyMonthlyResponse(
    val success: Boolean,
    val stats: StudyMonthlyStats?
)

// ─── Sleep & Mood ──────────────────────────────────────────────────────────────

data class SleepMoodLogRequest(
    val sleepDuration: Double,    // hours (e.g. 7.5)
    val sleepQuality: Int,        // 1-10
    val mood: String,             // "happy", "sad", "anxious", etc.
    val moodScore: Int,           // 1-10
    val notes: String? = null,
    val date: String? = null      // ISO date string, defaults to now
)

data class SleepMoodLog(
    @SerializedName("_id") val id: String,
    val userId: String,
    val date: String,
    val sleepDuration: Double,
    val sleepQuality: Int,
    val mood: String,
    val moodScore: Int,
    val notes: String?
)

data class SleepMoodLogResponse(
    val success: Boolean,
    val message: String?,
    val log: SleepMoodLog?
)

data class SleepMoodLogsResponse(
    val success: Boolean,
    val logs: List<SleepMoodLog>
)

data class SleepTrendPoint(
    val date: String,
    val sleepDuration: Double,
    val sleepQuality: Int
)

data class SleepTrendsResponse(
    val success: Boolean,
    val trends: List<SleepTrendPoint>
)

data class MoodTrendPoint(
    val date: String,
    val mood: String,
    val moodScore: Int
)

data class MoodTrendsResponse(
    val success: Boolean,
    val trends: List<MoodTrendPoint>
)

// ─── Productivity ──────────────────────────────────────────────────────────────

data class ProductivityLogRequest(
    val productivityScore: Int,     // 0-100
    val focusHours: Double? = null,
    val breakHours: Double? = null,
    val tasksCompleted: Int? = null,
    val tasksPlanned: Int? = null,
    val peakHourStart: Int? = null,  // 0-23
    val peakHourEnd: Int? = null,
    val distractions: Int? = null,
    val categories: List<CategoryItem>? = null,
    val notes: String? = null,
    val date: String? = null
)

data class CategoryItem(
    val name: String,
    val hours: Double
)

data class ProductivityLog(
    @SerializedName("_id") val id: String,
    val userId: String,
    val date: String,
    val productivityScore: Int,
    val focusHours: Double,
    val breakHours: Double,
    val tasksCompleted: Int,
    val tasksPlanned: Int,
    val peakHourStart: Int,
    val peakHourEnd: Int,
    val distractions: Int,
    val categories: List<CategoryItem>
)

data class ProductivityLogResponse(
    val success: Boolean,
    val message: String?,
    val log: ProductivityLog?
)

data class ProductivityTodayResponse(
    val success: Boolean,
    val log: ProductivityLog?
)

data class ProductivityWeeklyResponse(
    val success: Boolean,
    val trend: List<ProductivityLog>
)

// ─── Burnout ───────────────────────────────────────────────────────────────────

data class BurnoutFactor(
    val name: String,
    val score: Int     // 0-100
)

data class WellbeingDimensions(
    val physical: Int,      // 0-10
    val emotional: Int,
    val social: Int,
    val intellectual: Int,
    val occupational: Int
)

data class BurnoutAssessmentRequest(
    val riskScore: Int,           // 0-100
    val riskLevel: String,        // "low", "moderate", "high", "critical"
    val factors: List<BurnoutFactor>? = null,
    val wellbeingDimensions: WellbeingDimensions? = null,
    val warnings: List<String>? = null,
    val recommendations: List<String>? = null
)

data class BurnoutAssessment(
    @SerializedName("_id") val id: String,
    val userId: String,
    val date: String,
    val riskScore: Int,
    val riskLevel: String,
    val factors: List<BurnoutFactor>,
    val wellbeingDimensions: WellbeingDimensions,
    val warnings: List<String>,
    val recommendations: List<String>
)

data class BurnoutAssessmentResponse(
    val success: Boolean,
    val message: String?,
    val assessment: BurnoutAssessment?
)

data class BurnoutComputeData(
    val riskScore: Int,
    val riskLevel: String,
    val factors: List<BurnoutFactor>,
    val wellbeingDimensions: WellbeingDimensions,
    val warnings: List<String>,
    val recommendations: List<String>
)

data class BurnoutComputeResponse(
    val success: Boolean,
    val computed: BurnoutComputeData?
)

// ─── Dashboard ─────────────────────────────────────────────────────────────────

data class QuickStats(
    val lastSleepHours: Double?,
    val lastSleepQuality: Int?,
    val lastMood: String?,
    val lastMoodScore: Int?,
    val lastProductivityScore: Int?,
    val weeklyStudyHours: Double?
)

data class BurnoutAlert(
    val riskScore: Int?,
    val riskLevel: String?,
    val topWarning: String?
)

data class FeatureCardStudy(
    val weeklyHours: Double?,
    val sessionCount: Int?
)

data class FeatureCardSleep(
    val lastDuration: Double?,
    val lastQuality: Int?
)

data class FeatureCardBurnout(
    val riskScore: Int?,
    val riskLevel: String?
)

data class FeatureCardProductivity(
    val score: Int?
)

data class FeatureCards(
    val study: FeatureCardStudy,
    val sleep: FeatureCardSleep,
    val burnout: FeatureCardBurnout,
    val productivity: FeatureCardProductivity
)

data class DashboardData(
    val quickStats: QuickStats,
    val burnoutAlert: BurnoutAlert,
    val featureCards: FeatureCards
)

data class DashboardResponse(
    val success: Boolean,
    val dashboard: DashboardData?
)

// ─── Weekly Report ─────────────────────────────────────────────────────────────

data class ReportSummary(
    val avgSleep: Double,
    val avgMood: Double,
    val totalStudyHours: Double,
    val avgProductivity: Double
)

data class ReportPeriod(
    val from: String,
    val to: String
)

data class WellnessRadar(
    val sleep: Int,
    val mood: Int,
    val study: Int,
    val productivity: Int,
    val balance: Int
)

data class MoodVsProductivityPoint(
    val date: String,
    val productivityScore: Int,
    val moodScore: Int?
)

data class NextWeekGoal(
    val goal: String,
    val completed: Boolean
)

data class WeeklyReportData(
    val period: ReportPeriod,
    val summary: ReportSummary,
    val dailyActivity: Map<String, Map<String, Any>>,
    val moodVsProductivity: List<MoodVsProductivityPoint>,
    val wellnessRadar: WellnessRadar,
    val achievements: List<String>,
    val concerns: List<String>,
    val recommendations: List<String>,
    val nextWeekGoals: List<NextWeekGoal>
)

data class WeeklyReportResponse(
    val success: Boolean,
    val report: WeeklyReportData?
)
