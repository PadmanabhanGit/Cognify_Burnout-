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

    // success=false here means "request failed" (network/auth/server error), which
    // callers must treat differently from a successful response whose `log` is null
    // (meaning: no record exists yet for today). Collapsing those two cases would
    // show a fabricated empty state on a transient failure.
    suspend fun getProductivityToday(): ProductivityTodayResponse {
        return try {
            client.get("api/productivity/today") {
                authHeader()?.let { header(HttpHeaders.Authorization, it) }
            }.body()
        } catch (e: Exception) {
            ProductivityTodayResponse(success = false, log = null)
        }
    }

    suspend fun getProductivityWeekly(): ProductivityWeeklyResponse {
        return try {
            client.get("api/productivity/weekly") {
                authHeader()?.let { header(HttpHeaders.Authorization, it) }
            }.body()
        } catch (e: Exception) {
            ProductivityWeeklyResponse(success = false, days = emptyList())
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
            // The exception CLASS is prefixed, not just its message, because it is
            // what SyncStateMachine.classify() reads to tell a retryable network
            // blip apart from a SerializationException — which means the DTO and
            // the backend disagree and no amount of retrying will help. `e.message`
            // alone is frequently null, which classified everything as UNKNOWN.
            SleepMoodLogResponse(success = false, message = "${e::class.simpleName}: ${e.message}")
        }
    }

    /**
     * Reads back the signed-in account's own sleep/mood records.
     *
     * The counterpart to [saveSleepMoodLog], and the read half of sync: without
     * it a night that reached Firestore was unreachable again after a reinstall
     * or on a second device, because Room was the only thing that remembered it.
     *
     * Scoping is the server's, not ours — GET /api/sleep-mood/logs filters on
     * `userId == req.user.uid`, taken from the verified ID token this request
     * carries. There is no uid parameter to get wrong, so this call can only
     * ever return records belonging to the account whose token signed it.
     *
     * [limit] is applied server-side to a recency-sorted list that includes
     * manual mood entries, so it is a bound on RECORDS, not on nights — the
     * caller asks for more than it needs and filters (see SleepHistoryRestore).
     *
     * Returns success=false with no logs on any transport or parse failure,
     * matching the rest of this client: a restore that cannot reach the network
     * leaves local history exactly as it found it.
     */
    suspend fun getSleepMoodLogs(limit: Int): SleepMoodLogsResponse {
        return try {
            client.get("api/sleep-mood/logs") {
                parameter("limit", limit)
                authHeader()?.let { header(HttpHeaders.Authorization, it) }
            }.body()
        } catch (e: Exception) {
            // The exception TYPE is the only thing that distinguishes a transport
            // failure (SocketTimeout/UnknownHost — retry later) from a contract
            // failure (SerializationException — the DTO no longer matches the
            // backend and no amount of retrying will help). Collapsing both into
            // a bare success=false is what made the `_id` bug invisible for so
            // long, so the class name is logged here rather than discarded.
            println("[API] GET api/sleep-mood/logs failed: ${e::class.simpleName}: ${e.message}")
            SleepMoodLogsResponse(success = false)
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

    suspend fun logOfflineSession(request: OfflineSessionRequest): SimpleResponse {
        return try {
            client.post("api/study/log-offline") {
                contentType(ContentType.Application.Json)
                authHeader()?.let { header(HttpHeaders.Authorization, it) }
                setBody(request)
            }.body()
        } catch (e: Exception) {
            SimpleResponse(success = false, message = e.message ?: "Network error")
        }
    }

    suspend fun stopStudySession(sessionId: String): StudySessionResponse {
        return try {
            client.patch("api/study/stop/$sessionId") {
                authHeader()?.let { header(HttpHeaders.Authorization, it) }
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
