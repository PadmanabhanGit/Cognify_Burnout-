package com.simats.burnouttracker.data.api

import com.simats.burnouttracker.data.models.*
import com.simats.burnouttracker.utils.FirebaseTokenProvider
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object ApiClient {
    private const val BASE_URL = "https://cognify-burnout.onrender.com/"

    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            level = LogLevel.BODY
        }
        defaultRequest {
            url(BASE_URL)
        }
    }

    private suspend fun authHeader(): String? {
        val token = FirebaseTokenProvider.getIdToken()
        return token?.let { "Bearer $it" }
    }

    suspend fun register(fullName: String): SimpleResponse {
        return try {
            client.post("api/auth/register") {
                contentType(ContentType.Application.Json)
                authHeader()?.let { header(HttpHeaders.Authorization, it) }
                setBody(mapOf("fullName" to fullName))
            }.body()
        } catch (e: Exception) {
            SimpleResponse(success = false, message = e.message ?: "Network error")
        }
    }

    suspend fun getDashboard(): DashboardResponse {
        return try {
            client.get("api/dashboard") {
                authHeader()?.let { header(HttpHeaders.Authorization, it) }
            }.body()
        } catch (e: Exception) {
            DashboardResponse(success = false)
        }
    }

    suspend fun saveBurnoutAssessment(request: BurnoutAssessmentRequest): BurnoutAssessmentResponse {
        return try {
            client.post("api/burnout/assessment") {
                contentType(ContentType.Application.Json)
                authHeader()?.let { header(HttpHeaders.Authorization, it) }
                setBody(request)
            }.body()
        } catch (e: Exception) {
            BurnoutAssessmentResponse(success = false, message = e.message)
        }
    }

    suspend fun logProductivity(request: ProductivityLogRequest): ProductivityLogResponse {
        return try {
            client.post("api/productivity/log") {
                contentType(ContentType.Application.Json)
                authHeader()?.let { header(HttpHeaders.Authorization, it) }
                setBody(request)
            }.body()
        } catch (e: Exception) {
            ProductivityLogResponse(success = false, message = e.message)
        }
    }

    suspend fun saveSleepMoodLog(request: SleepMoodLogRequest): SleepMoodLogResponse {
        return try {
            client.post("api/sleep-mood/log") {
                contentType(ContentType.Application.Json)
                authHeader()?.let { header(HttpHeaders.Authorization, it) }
                setBody(request)
            }.body()
        } catch (e: Exception) {
            SleepMoodLogResponse(success = false, message = e.message)
        }
    }

    suspend fun syncUsageData(request: UsageSyncRequest): SimpleResponse {
        return try {
            client.post("api/usage/sync") {
                contentType(ContentType.Application.Json)
                authHeader()?.let { header(HttpHeaders.Authorization, it) }
                setBody(request)
            }.body()
        } catch (e: Exception) {
            SimpleResponse(success = false, message = e.message ?: "Unknown error")
        }
    }

    suspend fun startStudySession(request: StartSessionRequest): StudySessionResponse {
        return try {
            client.post("api/study/start") {
                contentType(ContentType.Application.Json)
                authHeader()?.let { header(HttpHeaders.Authorization, it) }
                setBody(request)
            }.body()
        } catch (e: Exception) {
            StudySessionResponse(success = false, message = e.message)
        }
    }

    suspend fun getStudyWeeklyStats(): StudyWeeklyResponse {
        return try {
            client.get("api/study/stats/weekly") {
                authHeader()?.let { header(HttpHeaders.Authorization, it) }
            }.body()
        } catch (e: Exception) {
            StudyWeeklyResponse(success = false)
        }
    }

    suspend fun getWeeklyReport(): WeeklyReportResponse {
        return try {
            client.get("api/report/weekly") {
                authHeader()?.let { header(HttpHeaders.Authorization, it) }
            }.body()
        } catch (e: Exception) {
            WeeklyReportResponse(success = false, report = null)
        }
    }
}
