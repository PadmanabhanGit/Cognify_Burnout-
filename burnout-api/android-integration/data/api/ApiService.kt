// ─── Retrofit API Service Interface ──────────────────────────────────────────
// Copy this file into your Android project under:
//   app/src/main/java/com/yourpackage/data/api/

package com.burnouttracker.data.api

import com.burnouttracker.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ─── Authentication ────────────────────────────────────────────────────────

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("api/auth/profile")
    suspend fun getProfile(): Response<ProfileResponse>

    // ─── Dashboard ─────────────────────────────────────────────────────────────

    @GET("api/dashboard")
    suspend fun getDashboard(): Response<DashboardResponse>

    // ─── Study Tracking ────────────────────────────────────────────────────────

    @POST("api/study/start")
    suspend fun startStudySession(@Body request: StartSessionRequest): Response<StudySessionResponse>

    @PATCH("api/study/stop/{sessionId}")
    suspend fun stopStudySession(@Path("sessionId") sessionId: String): Response<StudySessionResponse>

    @GET("api/study/stats/weekly")
    suspend fun getStudyWeeklyStats(): Response<StudyWeeklyResponse>

    @GET("api/study/stats/monthly")
    suspend fun getStudyMonthlyStats(): Response<StudyMonthlyResponse>

    // ─── Sleep & Mood ──────────────────────────────────────────────────────────

    @POST("api/sleep-mood/log")
    suspend fun saveSleepMoodLog(@Body request: SleepMoodLogRequest): Response<SleepMoodLogResponse>

    @GET("api/sleep-mood/logs")
    suspend fun getRecentSleepMoodLogs(
        @Query("limit") limit: Int = 7
    ): Response<SleepMoodLogsResponse>

    @GET("api/sleep-mood/trends/sleep")
    suspend fun getSleepTrends(
        @Query("days") days: Int = 30
    ): Response<SleepTrendsResponse>

    @GET("api/sleep-mood/trends/mood")
    suspend fun getMoodTrends(
        @Query("days") days: Int = 30
    ): Response<MoodTrendsResponse>

    // ─── Productivity ──────────────────────────────────────────────────────────

    @POST("api/productivity/log")
    suspend fun logProductivity(@Body request: ProductivityLogRequest): Response<ProductivityLogResponse>

    @GET("api/productivity/today")
    suspend fun getTodayProductivity(): Response<ProductivityTodayResponse>

    @GET("api/productivity/weekly")
    suspend fun getWeeklyProductivity(): Response<ProductivityWeeklyResponse>

    // ─── Burnout Prediction ────────────────────────────────────────────────────

    @GET("api/burnout/compute")
    suspend fun computeBurnoutRisk(): Response<BurnoutComputeResponse>

    @GET("api/burnout/latest")
    suspend fun getLatestBurnoutAssessment(): Response<BurnoutAssessmentResponse>

    @POST("api/burnout/assessment")
    suspend fun saveBurnoutAssessment(@Body request: BurnoutAssessmentRequest): Response<BurnoutAssessmentResponse>

    // ─── Weekly Report ─────────────────────────────────────────────────────────

    @GET("api/report/weekly")
    suspend fun getWeeklyReport(): Response<WeeklyReportResponse>
}
