package com.simats.burnouttracker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.simats.burnouttracker.ui.theme.BurnOutTrackerTheme
import com.simats.burnouttracker.utils.*

/**
 * Gate for every screen that renders user-specific values.
 *
 * Two things it guarantees, both of which were previously unenforced:
 *
 *  1. Nothing renders until [UserSession.isReady]. Session start-up resets the
 *     previous account's in-memory state and reloads this account's own, and
 *     those steps are not instantaneous. Without the gate the first frame after
 *     an account switch paints whatever the outgoing user left in [AppData] —
 *     which is precisely the reported symptom of B seeing A's dashboard.
 *
 *  2. A signed-out session never renders user content at all; it redirects.
 *     Reaching a user-scoped route with no identity has no correct output, and
 *     falling back to the last values in memory is the wrong answer.
 *
 * Keyed reads of [UserSession.uid] and [UserSession.isReady] are Compose state,
 * so a session transition recomposes this automatically.
 */
@Composable
private fun UserScoped(
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val uid = UserSession.uid
    val isReady = UserSession.isReady

    LaunchedEffect(uid) {
        if (uid == null) {
            navController.navigate("login") { popUpTo(0) { inclusive = true } }
        }
    }

    if (uid != null && isReady) {
        content()
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun AppNavigation(initialRoute: String? = null) {
    println("DEBUG_FORCE: AppNavigation started. If you see this, code updates ARE working.")
    val navController = rememberNavController()
    val isWeb = getPlatform() == PlatformType.WEB
    val settings = rememberPlatformSettings()
    val authService = rememberAuthService()

    val isPolicyAccepted = remember(settings) { settings.getBoolean("policy_accepted", false) }
    val arePermissionsViewed = remember(settings) { settings.getBoolean("permissions_viewed", false) }
    val isLoggedIn = authService.isLoggedIn()

    val startDest = when {
        isLoggedIn && initialRoute != null -> initialRoute
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
                    },
                    onTermsClick = { navController.navigate("terms_of_service") },
                    onPrivacyClick = { navController.navigate("privacy_policy") }
                )
            }
            composable("dashboard") {
                UserScoped(navController) { DashboardScreen(navController) }
            }
            composable("study_tracker") {
                UserScoped(navController) { StudyTrackerScreen(navController) }
            }
            composable("study_tracker_details") {
                UserScoped(navController) { StudyTrackerDetailsScreen(navController) }
            }
            composable("burnout_risk") {
                UserScoped(navController) { BurnoutRiskScreen(navController) }
            }
            composable("weekly_report") {
                UserScoped(navController) { WeeklyReportScreen(navController) }
            }
            composable("productivity") {
                UserScoped(navController) { ProductivityScreen(navController) }
            }
            composable("sleep_mood") {
                UserScoped(navController) { SleepMoodScreen(navController) }
            }
            composable("sleep_mood_dashboard") {
                UserScoped(navController) { SleepMoodDashboardScreen(navController) }
            }
            // Routes "sleep_mood_logger" and "sleep_mood_details" retired.
            //
            // Neither had any navigate() caller, so neither was reachable:
            //  - sleep_mood_logger was superseded by the manual-entry fallback
            //    that now lives inside SleepMoodScreen (sleep_mood). Manual
            //    logging is unaffected by this removal.
            //  - sleep_mood_details rendered only the fabricated literals
            //    "Average Sleep: 7.5 hrs" and "Dominant Mood: Happy", and its
            //    sole entry point (RecentSleepLogsCard) was itself never called.
            //
            // Unregistering here is the smallest safe cleanup: it guarantees no
            // reachable route can render fabricated sleep data, without deleting
            // files or disturbing any live navigation.
            composable("sleep_mood_analytics") {
                UserScoped(navController) { SleepMoodAnalyticsScreen(navController) }
            }
            composable("generalized_action_plan") {
                UserScoped(navController) { GeneralizedActionPlanScreen(navController) }
            }
            composable("calendar") {
                UserScoped(navController) { CalendarScreen(navController) }
            }
            composable("entertainment_usage") {
                UserScoped(navController) { EntertainmentAppUsageScreen(navController) }
            }

            // ── Platform-Specific Screens ─────────────────────────────────────
            addPlatformScreens(navController)
        }
    }
}
