package com.howsleep.app.ui.challenge

data class ChallengeUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val description: String = "",
    val habitInstruction: String = "",
    val source: String = "",
    val durationDays: Int = 0,
    val daysElapsed: Int = 0,
    val status: String = "ACTIVE",
    val successMetricType: String = "",
    val successMetricTarget: Float = 0f,
    val successMetricDirection: String = "",
    val baselineValue: Float = 0f,
    val hasActiveChallenge: Boolean = false,
    val errorMessage: String? = null,
)
