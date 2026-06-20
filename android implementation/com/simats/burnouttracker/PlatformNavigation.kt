package com.simats.burnouttracker

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

actual fun NavGraphBuilder.addPlatformScreens(navController: NavController) {
    composable("splash") {
        SplashScreen(onGetStartedClick = { navController.navigate("onboarding") })
    }
    composable("onboarding") {
        OnboardingScreen(onNextClick = { navController.navigate("splash3") })
    }
    composable("splash3") {
        Splash3Screen(onNextClick = { navController.navigate("splash4") })
    }
    composable("splash4") {
        Splash4Screen(onGetStartedClick = { navController.navigate("permission_screen") })
    }
    composable("onboarding_privacy") {
        PrivacyPolicyScreen(navController, isFirstTime = true)
    }
    composable("permission_screen") {
        AppUsagePermissionScreen(navController)
    }
    composable("settings") {
        SettingsScreen(navController)
    }
    composable("personal_information") {
        PersonalInformationScreen(navController)
    }
    composable("privacy_data") {
        PrivacyDataScreen(navController)
    }
    composable("terms_of_service") {
        TermsOfServiceScreen(navController)
    }
    composable("privacy_policy") {
        PrivacyPolicyScreen(navController, isFirstTime = false)
    }
    composable("change_password") {
        ChangePasswordScreen(navController)
    }
    composable("linked_accounts") {
        LinkedAccountsScreen(navController)
    }
}
