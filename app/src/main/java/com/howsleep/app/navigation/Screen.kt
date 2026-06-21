package com.howsleep.app.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object PreSleep : Screen("pre_sleep")
    object PostSleep : Screen("post_sleep")
    object Challenge : Screen("challenge")
    object ChallengeHistory : Screen("challenge_history")
    object Trends : Screen("trends")
    object Settings : Screen("settings")
}
