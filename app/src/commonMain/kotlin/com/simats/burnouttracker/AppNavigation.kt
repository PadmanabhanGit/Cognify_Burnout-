package com.simats.burnouttracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.simats.burnouttracker.ui.theme.BurnOutTrackerTheme
import com.simats.burnouttracker.utils.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val isWeb = getPlatform() == PlatformType.WEB
    val settings = rememberPlatformSettings()
    val authService = rememberAuthService()

    val isPolicyAccepted = remember(settings) { settings.getBoolean("policy_accepted", false) }
    val arePermissionsViewed = remember(settings) { settings.getBoolean("permissions_viewed", false) }
    val isLoggedIn = authService.isLoggedIn()

    val startDest = when {
        isLoggedIn           -> "dashboard"
        isWeb                -> "login" 
        !arePermissionsViewed -> "splash" // Start at splash if permissions not done
        !isPolicyAccepted     -> "onboarding_privacy" // Then policy
        else                 -> "login" // Finally login
    }

    BurnOutTrackerTheme {
        NavHost(navController = navController, startDestination = startDest) {

            // ── Shared Data Screens ──────────────────────────────────────────
            composable("login") {
                LoginScreen(
                    onSignInClick = {
                        navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
                    },
                    onSignUpClick = { navController.navigate("register") }
                )
            }
            composable("register") {
                RegisterScreen(
                    onSignInClick = { navController.navigate("login") },
                    onSignUpClick = {
                        navController.navigate("dashboard") { popUpTo("register") { inclusive = true } }
                    }
                )
            }
            composable("dashboard") {
                DashboardScreen(navController)
            }
            composable("study_tracker") {
                StudyTrackerScreen(navController)
            }
            composable("study_tracker_details") {
                StudyTrackerDetailsScreen(navController)
            }
            composable("burnout_risk") {
                BurnoutRiskScreen(navController)
            }
            composable("weekly_report") {
                WeeklyReportScreen(navController)
            }
            composable("productivity") {
                ProductivityScreen(navController)
            }
            composable("sleep_mood") {
                SleepMoodScreen(navController)
            }
            composable("sleep_mood_dashboard") {
                SleepMoodDashboardScreen(navController)
            }
            composable("sleep_mood_logger") {
                SleepMoodLoggerScreen(navController)
            }
            composable("sleep_mood_details") {
                SleepMoodDetailsScreen(navController)
            }
            composable("sleep_mood_analytics") {
                SleepMoodAnalyticsScreen(navController)
            }
            composable("generalized_action_plan") {
                GeneralizedActionPlanScreen(navController)
            }
            composable("calendar") {
                CalendarScreen(navController)
            }
            composable("entertainment_usage") {
                EntertainmentAppUsageScreen(navController)
            }

            // ── Platform-Specific Screens ─────────────────────────────────────
            addPlatformScreens(navController)
        }
    }
}
