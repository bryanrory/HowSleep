package com.howsleep.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.howsleep.app.ui.settings.SettingsScreen

@Composable
fun HowSleepNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Settings.route,
    ) {
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
        // Fase 2: Dashboard, PreSleep, PostSleep
        // Fase 3: Challenge
        // Fase 4: Trends
    }
}
