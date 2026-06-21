package com.howsleep.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.howsleep.app.ui.challenge.ChallengeHistoryScreen
import com.howsleep.app.ui.challenge.ChallengeScreen
import com.howsleep.app.ui.dashboard.DashboardScreen
import com.howsleep.app.ui.postsleep.PostSleepScreen
import com.howsleep.app.ui.presleep.PreSleepScreen
import com.howsleep.app.ui.settings.SettingsScreen
import com.howsleep.app.ui.trends.TrendsScreen

@Composable
fun HowSleepNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToPreSleep = { navController.navigate(Screen.PreSleep.route) },
                onNavigateToPostSleep = { navController.navigate(Screen.PostSleep.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToChallenge = { navController.navigate(Screen.Challenge.route) },
                onNavigateToTrends = { navController.navigate(Screen.Trends.route) },
                onNavigateToChallengeHistory = { navController.navigate(Screen.ChallengeHistory.route) },
            )
        }
        composable(Screen.PreSleep.route) {
            PreSleepScreen(
                onNavigateToDashboard = { navController.popBackStack() },
            )
        }
        composable(Screen.PostSleep.route) {
            PostSleepScreen(
                onNavigateToDashboard = { navController.popBackStack() },
            )
        }
        composable(Screen.Challenge.route) {
            ChallengeScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.ChallengeHistory.route) {
            ChallengeHistoryScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Trends.route) {
            TrendsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
