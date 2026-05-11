// ─── Usage Examples ───────────────────────────────────────────────────────────
// This file shows how to call the API from your Kotlin screens/ViewModels.
// Copy relevant snippets into your actual ViewModel classes.

package com.burnouttracker.examples

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burnouttracker.data.api.AuthInterceptor
import com.burnouttracker.data.api.RetrofitClient
import com.burnouttracker.data.models.*
import kotlinx.coroutines.launch

// ════════════════════════════════════════════════════════════════════════════════
// EXAMPLE 1: Login from LoginScreen
// ════════════════════════════════════════════════════════════════════════════════
class LoginViewModel(private val context: Context) : ViewModel() {

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                val api = RetrofitClient.getApiService(context)
                val response = api.login(LoginRequest(email, password))

                if (response.isSuccessful && response.body()?.success == true) {
                    val token = response.body()!!.token!!
                    val user = response.body()!!.user!!

                    // Save token for future requests
                    AuthInterceptor.saveToken(context, token)

                    // Navigate to DashboardScreen
                    // TODO: Update your UI state / navigate
                    println("Login success: ${user.fullName}")
                } else {
                    val errorMsg = response.body()?.message ?: "Login failed"
                    println("Login error: $errorMsg")
                }
            } catch (e: Exception) {
                println("Network error: ${e.message}")
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// EXAMPLE 2: Register from RegisterScreen
// ════════════════════════════════════════════════════════════════════════════════
class RegisterViewModel(private val context: Context) : ViewModel() {

    fun register(fullName: String, email: String, password: String) {
        viewModelScope.launch {
            try {
                val api = RetrofitClient.getApiService(context)
                val response = api.register(RegisterRequest(fullName, email, password))

                if (response.isSuccessful && response.body()?.success == true) {
                    val token = response.body()!!.token!!
                    AuthInterceptor.saveToken(context, token)
                    // Navigate to DashboardScreen
                }
            } catch (e: Exception) {
                println("Network error: ${e.message}")
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// EXAMPLE 3: Load DashboardScreen data
// ════════════════════════════════════════════════════════════════════════════════
class DashboardViewModel(private val context: Context) : ViewModel() {

    fun loadDashboard() {
        viewModelScope.launch {
            try {
                val api = RetrofitClient.getApiService(context)
                val response = api.getDashboard()

                if (response.isSuccessful) {
                    val dashboard = response.body()!!.dashboard!!

                    // Use these for your Quick Stats card
                    val sleepHours = dashboard.quickStats.lastSleepHours
                    val mood = dashboard.quickStats.lastMood
                    val productivity = dashboard.quickStats.lastProductivityScore
                    val studyHours = dashboard.quickStats.weeklyStudyHours

                    // Use this for the Burnout Alert card
                    val burnoutRisk = dashboard.burnoutAlert.riskScore
                    val burnoutLevel = dashboard.burnoutAlert.riskLevel

                    // Use these for feature cards
                    val featureCards = dashboard.featureCards
                }
            } catch (e: Exception) {
                println("Error loading dashboard: ${e.message}")
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// EXAMPLE 4: Study timer on StudyTrackingScreen
// ════════════════════════════════════════════════════════════════════════════════
class StudyTrackingViewModel(private val context: Context) : ViewModel() {

    private var activeSessionId: String? = null

    fun startTimer(subject: String) {
        viewModelScope.launch {
            val api = RetrofitClient.getApiService(context)
            val response = api.startStudySession(StartSessionRequest(subject))
            if (response.isSuccessful) {
                activeSessionId = response.body()?.session?.id
                // Start your local timer UI
            }
        }
    }

    fun stopTimer() {
        val sessionId = activeSessionId ?: return
        viewModelScope.launch {
            val api = RetrofitClient.getApiService(context)
            val response = api.stopStudySession(sessionId)
            if (response.isSuccessful) {
                val duration = response.body()?.session?.duration
                // Show duration to user, refresh charts
            }
        }
    }

    fun loadWeeklyChart() {
        viewModelScope.launch {
            val api = RetrofitClient.getApiService(context)
            val response = api.getStudyWeeklyStats()
            if (response.isSuccessful) {
                val stats = response.body()!!.stats!!
                // stats.dailyTotals → bar chart data
                // stats.subjectBreakdown → progress bars
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// EXAMPLE 5: Save sleep & mood log on SleepMoodScreen
// ════════════════════════════════════════════════════════════════════════════════
class SleepMoodViewModel(private val context: Context) : ViewModel() {

    fun saveEntry(sleepDuration: Double, sleepQuality: Int, mood: String, moodScore: Int) {
        viewModelScope.launch {
            val api = RetrofitClient.getApiService(context)
            val response = api.saveSleepMoodLog(
                SleepMoodLogRequest(
                    sleepDuration = sleepDuration,
                    sleepQuality = sleepQuality,
                    mood = mood,
                    moodScore = moodScore
                )
            )
            if (response.isSuccessful) {
                // Refresh the trends charts and recent logs
                loadSleepTrends()
                loadRecentLogs()
            }
        }
    }

    fun loadSleepTrends() {
        viewModelScope.launch {
            val api = RetrofitClient.getApiService(context)
            val response = api.getSleepTrends(30)
            if (response.isSuccessful) {
                val trends = response.body()!!.trends
                // trends → sleep area chart data
            }
        }
    }

    fun loadRecentLogs() {
        viewModelScope.launch {
            val api = RetrofitClient.getApiService(context)
            val response = api.getRecentSleepMoodLogs(7)
            if (response.isSuccessful) {
                val logs = response.body()!!.logs
                // logs → recent logs list
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// EXAMPLE 6: Burnout risk on BurnoutPredictionScreen
// ════════════════════════════════════════════════════════════════════════════════
class BurnoutViewModel(private val context: Context) : ViewModel() {

    fun computeRisk() {
        viewModelScope.launch {
            val api = RetrofitClient.getApiService(context)
            val response = api.computeBurnoutRisk()
            if (response.isSuccessful) {
                val data = response.body()!!.computed!!
                // data.riskScore → pie chart
                // data.factors → progress bars
                // data.wellbeingDimensions → radar chart
                // data.warnings → warning indicators
                // data.recommendations → AI recommendations list
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// EXAMPLE 7: Sign out from ProfileScreen
// ════════════════════════════════════════════════════════════════════════════════
class ProfileViewModel(private val context: Context) : ViewModel() {

    fun signOut() {
        // Clear the saved token — no API call needed
        AuthInterceptor.clearToken(context)
        // Navigate back to LoginScreen
    }
}
