package com.simats.burnouttracker.data.models

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

/**
 * A `studySessions` document as the backend returns it.
 *
 * Same defect, same fix as [RemoteSleepLog]: this declared
 * `@SerialName("_id")` while `server/routes/study.js` responds with
 * `{ session: { id: doc.id, ...doc.data() } }`.
 *
 * This one broke more than sync bookkeeping. `startStudySession` could never
 * parse its response, so it returned `success = false` even though the session
 * document had been created; `AppData.activeSessionId` was therefore never set,
 * `stopStudySession` was never called with an id, and every session was routed
 * to the offline queue while the server kept an `isActive: true` row open
 * indefinitely.
 */
@Serializable
data class StudySession(
    val id: String? = null,
    val userId: String? = null,
    val subject: String? = null,
    val duration: Int? = null,          // minutes
    val startTime: String? = null,
    val endTime: String? = null,
    val isActive: Boolean? = null,
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
    // Nullable: a mood-only entry (no sleep session, automatic or manual, to
    // report) must be able to send neither rather than a fabricated value.
    // The server already treats these as optional (`sleepDuration ?? null`).
    val sleepDuration: Double? = null,    // hours (e.g. 7.5)
    val sleepQuality: Int? = null,        // automatic writers: 0-100. Manual writers: 1-10 (unchanged, see source below).
    val mood: String,             // "happy", "sad", "anxious", etc.
    val moodScore: Int,           // 1-10
    val notes: String? = null,
    val date: String? = null,     // ISO date string, defaults to now
    val sleepStart: Long? = null,
    val sleepEnd: Long? = null,
    val awakeningCount: Int? = null,
    val disturbanceScore: Int? = null,
    // Explicit record of who produced this entry: "automatic" (SleepMonitoringEngine)
    // or "manual" (a logging screen). Nullable/additive so older builds that don't
    // set it still compile and post successfully — the server falls back to its
    // existing sleepStart/sleepEnd-presence heuristic when this is absent.
    val source: String? = null
)

/**
 * One persisted `sleepMoodLogs` document, as the backend actually returns it.
 *
 * Replaces a previous `SleepMoodLog` model that declared `@SerialName("_id")`
 * with non-null `sleepDuration`/`sleepQuality`/`mood`/`moodScore`. Neither
 * assumption held: routes return `{ id: doc.id, ...doc.data() }` — `id`, never
 * `_id` — and POST /log persists `sleepDuration ?? null`, `sleepQuality ?? null`
 * and so on, so a mood-only entry legitimately stores nulls.
 *
 * The consequence was not a visible crash but a silent one. Deserializing the
 * POST *response* threw, ApiClient caught it and returned `success = false`, and
 * the caller concluded the write had failed — while the server had in fact
 * already committed the document. For sleep that meant `syncedAt` was never
 * stamped and every night was re-POSTed on every refresh, forever.
 *
 * Every field is therefore nullable with a null default: the backend stores only
 * what was sent, so an unsent field is ABSENT, not zero. Treating any of them as
 * required would reintroduce exactly this failure. Same reasoning, and the same
 * shape, as [ProductivityLog].
 */
@Serializable
data class RemoteSleepLog(
    val id: String? = null,
    val userId: String? = null,
    val date: String? = null,
    val sleepDuration: Double? = null,   // hours, as written by the phone: totalSleepMinutes / 60.0
    val sleepQuality: Int? = null,
    val mood: String? = null,
    val moodScore: Int? = null,
    val notes: String? = null,
    val sleepStart: Long? = null,
    val sleepEnd: Long? = null,
    val awakeningCount: Int? = null,
    val disturbanceScore: Int? = null,
    // Additive: absent on records written before this field existed.
    val source: String? = null
)

@Serializable
data class SleepMoodLogResponse(
    val success: Boolean,
    val message: String? = null,
    val log: RemoteSleepLog? = null
)

@Serializable
data class SleepMoodLogsResponse(
    val success: Boolean,
    val logs: List<RemoteSleepLog> = emptyList()
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

/**
 * The persisted productivityLogs/{userId}_{IST-date} document, as returned by
 * POST /log and GET /today. Every field beyond `date` is nullable and has no
 * default other than null: the backend only ever stores what was actually
 * sent (see server/routes/productivity.js), so an unsent optional field is
 * simply ABSENT from the document, not defaulted to 0/empty. Treating any of
 * these as non-null here would either crash deserialization on a real
 * document or silently misrepresent "not sent" as "sent as zero".
 *
 * Replaces a previous version of this model (SerialName("_id"), non-null
 * peakHourStart/peakHourEnd/categories/etc.) that was never wired to any
 * caller and did not match what the Firestore-backed endpoint actually
 * returns — it assumed a Mongo-style `_id` and fields the backend never
 * stores at all.
 */
@Serializable
data class ProductivityLog(
    val id: String? = null,
    val userId: String? = null,
    val date: String? = null,
    val productivityScore: Int? = null,
    val focusHours: Double? = null,
    val breakHours: Double? = null,
    val tasksCompleted: Int? = null,
    val tasksPlanned: Int? = null,
    val peakHourStart: Int? = null,
    val peakHourEnd: Int? = null,
    val distractions: Int? = null,
    val categories: List<CategoryItem>? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
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

/** One entry of GET /weekly's `days[]` array — see server/routes/productivity.js. */
@Serializable
data class ProductivityWeeklyDay(
    val date: String,
    val available: Boolean = false,
    val productivityScore: Int? = null,
    val focusHours: Double? = null
)

@Serializable
data class ProductivityWeeklyResponse(
    val success: Boolean,
    val days: List<ProductivityWeeklyDay> = emptyList()
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
    val recommendations: List<String>? = null,
    // Assessment sentence shown on the Android Burnout screen (burnoutAssessmentText).
    val assessment: String? = null,
    // The six axes of the Android "Wellbeing Analysis" radar (WellbeingGenerator output).
    val wellbeing: BurnoutWellbeing? = null
)

/** Android's WellbeingMetrics, flattened for transport. Values are 0-100. */
@Serializable
data class BurnoutWellbeing(
    val focus: Int,
    val stress: Int,
    val mood: Int,
    val energy: Int,
    val sleep: Int,
    val study: Int
)

/**
 * A `burnoutAssessments` document as the backend returns it.
 *
 * Third instance of the `_id` defect described on [RemoteSleepLog]. The nested
 * lists and object are nullable for the same reason as the scalars: the backend
 * stores only what was sent, so an assessment saved without warnings or
 * recommendations omits those keys entirely rather than storing empty arrays.
 */
@Serializable
data class BurnoutAssessment(
    val id: String? = null,
    val userId: String? = null,
    val date: String? = null,
    val riskScore: Int? = null,
    val riskLevel: String? = null,
    val factors: List<BurnoutFactor>? = null,
    val wellbeingDimensions: WellbeingDimensions? = null,
    val warnings: List<String>? = null,
    val recommendations: List<String>? = null
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
