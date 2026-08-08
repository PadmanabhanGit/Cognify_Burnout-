package com.simats.burnouttracker.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Auth ──────────────────────────────────────────────────────────────────────

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null,
    val user: UserData? = null
)

@Serializable
data class UserData(
    val id: String,
    val fullName: String,
    val email: String,
    val avatarUrl: String? = null
)

@Serializable
data class ProfileResponse(
    val success: Boolean,
    val user: UserData? = null
)

@Serializable
data class ProfileData(
    val firstName: String? = null,
    val lastName: String? = null,
    val age: String? = null,
    val location: String? = null,
    val linkedAccounts: List<String>? = null,
    val syncHealth: Boolean? = null,
    val anonymousAnalytics: Boolean? = null,
    val personalizedInsights: Boolean? = null
)

// ─── Study Sessions ────────────────────────────────────────────────────────────

@Serializable
data class StartSessionRequest(
    val subject: String,
    val notes: String? = null
)

@Serializable
data class OfflineSessionRequest(
    val subject: String,
    val duration: Int,
    val startTime: String,
    val notes: String? = null
)

@Serializable
data class StudySession(
    @SerialName("_id") val id: String,
    val userId: String,
    val subject: String,
    val duration: Int,          // minutes
    val startTime: String,
    val endTime: String? = null,
    val isActive: Boolean,
    val notes: String? = null
)

@Serializable
data class StudySessionResponse(
    val success: Boolean,
    val message: String? = null,
    val session: StudySession? = null
)

@Serializable
data class StudyWeeklyStats(
    val totalMinutes: Int,
    val totalHours: Double,
    val sessionsCount: Int? = null,
    val sessionCount: Int? = null,
    val todayMinutes: Int? = null,
    val dailyTotals: Map<String, Int>? = null,
    val dailyBreakdown: Map<String, Int>? = null,
    val subjectBreakdown: Map<String, Int>? = null
)

@Serializable
data class StudyWeeklyResponse(
    val success: Boolean,
    val stats: StudyWeeklyStats? = null
)

@Serializable
data class StudyMonthlyStats(
    val dailyTotals: Map<String, Int>
)

@Serializable
data class StudyMonthlyResponse(
    val success: Boolean,
    val stats: StudyMonthlyStats? = null
)

// ─── Sleep & Mood ──────────────────────────────────────────────────────────────

@Serializable
data class SleepMoodLogRequest(
    val sleepDuration: Double,    // hours (e.g. 7.5)
    val sleepQuality: Int,        // 1-10
    val mood: String,             // "happy", "sad", "anxious", etc.
    val moodScore: Int,           // 1-10
    val notes: String? = null,
    val date: String? = null      // ISO date string, defaults to now
)

@Serializable
data class SleepMoodLog(
    @SerialName("_id") val id: String,
    val userId: String,
    val date: String,
    val sleepDuration: Double,
    val sleepQuality: Int,
    val mood: String,
    val moodScore: Int,
    val notes: String? = null
)

@Serializable
data class SleepMoodLogResponse(
    val success: Boolean,
    val message: String? = null,
    val log: SleepMoodLog? = null
)

@Serializable
data class SleepMoodLogsResponse(
    val success: Boolean,
    val logs: List<SleepMoodLog>
)

@Serializable
data class SleepTrendPoint(
    val date: String,
    val sleepDuration: Double,
    val sleepQuality: Int
)

@Serializable
data class SleepTrendsResponse(
    val success: Boolean,
    val trends: List<SleepTrendPoint>
)

@Serializable
data class MoodTrendPoint(
    val date: String,
    val mood: String,
    val moodScore: Int
)

@Serializable
data class MoodTrendsResponse(
    val success: Boolean,
    val trends: List<MoodTrendPoint>
)

// ─── Productivity ──────────────────────────────────────────────────────────────

@Serializable
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

@Serializable
data class CategoryItem(
    val name: String,
    val hours: Double
)

@Serializable
data class ProductivityLog(
    @SerialName("_id") val id: String,
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

@Serializable
data class ProductivityLogResponse(
    val success: Boolean,
    val message: String? = null,
    val log: ProductivityLog? = null
)

@Serializable
data class ProductivityTodayResponse(
    val success: Boolean,
    val log: ProductivityLog? = null
)

@Serializable
data class ProductivityWeeklyResponse(
    val success: Boolean,
    val trend: List<ProductivityLog>
)

// ─── Burnout ───────────────────────────────────────────────────────────────────

@Serializable
data class BurnoutFactor(
    val name: String,
    val score: Int     // 0-100
)

@Serializable
data class WellbeingDimensions(
    val physical: Int,      // 0-10
    val emotional: Int,
    val social: Int,
    val intellectual: Int,
    val occupational: Int
)

@Serializable
data class BurnoutAssessmentRequest(
    val riskScore: Int,           // 0-100
    val riskLevel: String,        // "low", "moderate", "high", "critical"
    val factors: List<BurnoutFactor>? = null,
    val wellbeingDimensions: WellbeingDimensions? = null,
    val warnings: List<String>? = null,
    val recommendations: List<String>? = null
)

@Serializable
data class BurnoutAssessment(
    @SerialName("_id") val id: String,
    val userId: String,
    val date: String,
    val riskScore: Int,
    val riskLevel: String,
    val factors: List<BurnoutFactor>,
    val wellbeingDimensions: WellbeingDimensions,
    val warnings: List<String>,
    val recommendations: List<String>
)

@Serializable
data class BurnoutAssessmentResponse(
    val success: Boolean,
    val message: String? = null,
    val assessment: BurnoutAssessment? = null
)

@Serializable
data class BurnoutComputeData(
    val riskScore: Int,
    val riskLevel: String,
    val factors: List<BurnoutFactor>,
    val wellbeingDimensions: WellbeingDimensions,
    val warnings: List<String>,
    val recommendations: List<String>
)

@Serializable
data class BurnoutComputeResponse(
    val success: Boolean,
    val computed: BurnoutComputeData? = null
)

// ─── Dashboard ─────────────────────────────────────────────────────────────────

@Serializable
data class QuickStats(
    val lastSleepHours: Double? = null,
    val lastSleepQuality: Int? = null,
    val lastMood: String? = null,
    val lastMoodScore: Int? = null,
    val lastProductivityScore: Int? = null,
    val weeklyStudyHours: Double? = null
)

@Serializable
data class BurnoutAlert(
    val riskScore: Int? = null,
    val riskLevel: String? = null,
    val topWarning: String? = null
)

@Serializable
data class FeatureCardStudy(
    val weeklyHours: Double? = null,
    val sessionCount: Int? = null
)

@Serializable
data class FeatureCardSleep(
    val lastDuration: Double? = null,
    val lastQuality: Int? = null
)

@Serializable
data class FeatureCardBurnout(
    val riskScore: Int? = null,
    val riskLevel: String? = null
)

@Serializable
data class FeatureCardProductivity(
    val score: Int? = null
)

@Serializable
data class FeatureCards(
    val study: FeatureCardStudy,
    val sleep: FeatureCardSleep,
    val burnout: FeatureCardBurnout,
    val productivity: FeatureCardProductivity
)

@Serializable
data class DashboardUser(
    val firstName: String? = null
)

@Serializable
data class DashboardData(
    val user: DashboardUser? = null,
    val quickStats: QuickStats,
    val burnoutAlert: BurnoutAlert,
    val featureCards: FeatureCards
)

@Serializable
data class DashboardResponse(
    val success: Boolean,
    val dashboard: DashboardData? = null
)

// ─── Weekly Report ─────────────────────────────────────────────────────────────

@Serializable
data class ReportSummary(
    val avgSleep: Double,
    val avgMood: Double,
    val totalStudyHours: Double,
    val avgProductivity: Double
)

@Serializable
data class ReportPeriod(
    val from: String,
    val to: String
)

@Serializable
data class WellnessRadar(
    val sleep: Int,
    val mood: Int,
    val study: Int,
    val productivity: Int,
    val balance: Int
)

@Serializable
data class MoodVsProductivityPoint(
    val date: String,
    val productivityScore: Int,
    val moodScore: Int? = null
)

@Serializable
data class NextWeekGoal(
    val goal: String,
    val completed: Boolean
)

@Serializable
data class WeeklyReportData(
    val period: ReportPeriod,
    val summary: ReportSummary,
    val dailyActivity: Map<String, Map<String, String>>, // Ktor JSON doesn't like Map<String, Any> easily without more config
    val moodVsProductivity: List<MoodVsProductivityPoint>,
    val wellnessRadar: WellnessRadar,
    val achievements: List<String>,
    val concerns: List<String>,
    val recommendations: List<String>,
    val nextWeekGoals: List<NextWeekGoal>
)

@Serializable
data class WeeklyReportResponse(
    val success: Boolean,
    val report: WeeklyReportData? = null
)

@Serializable
data class UsageSyncRequest(
    val usageData: List<UsageItemRequest>,
    val date: String? = null
)

@Serializable
data class UsageItemRequest(
    val packageName: String,
    val category: String,
    val duration: Long, // legacy whole minutes
    val durationSeconds: Long? = null
)

@Serializable
data class UsageTodayResponse(
    val success: Boolean,
    val usage: List<UsageItemBackend>
)

@Serializable
data class UsageItemBackend(
    val category: String,
    val time: String,
    val progress: Float,
    val color: String
)

@Serializable
data class SimpleResponse(
    val success: Boolean,
    val message: String
)
