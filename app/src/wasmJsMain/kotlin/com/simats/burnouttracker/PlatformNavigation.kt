package com.simats.burnouttracker

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder

actual fun NavGraphBuilder.addPlatformScreens(navController: NavController) {
    // Web doesn't need the extra screens for now
}
