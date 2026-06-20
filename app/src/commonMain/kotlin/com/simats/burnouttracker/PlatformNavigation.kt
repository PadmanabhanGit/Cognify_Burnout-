package com.simats.burnouttracker

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder

expect fun NavGraphBuilder.addPlatformScreens(navController: NavController)
