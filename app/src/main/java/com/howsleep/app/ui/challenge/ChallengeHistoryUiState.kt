package com.howsleep.app.ui.challenge

import java.time.LocalDate

data class ChallengeHistoryItem(
    val id: Long,
    val title: String,
    val status: String,
    val source: String,
    val durationDays: Int,
    val validFromDate: LocalDate,
    val validUntilDate: LocalDate,
    val habitToChange: String,
    val baselineValue: Float,
)

data class ChallengeHistoryUiState(
    val isLoading: Boolean = true,
    val challenges: List<ChallengeHistoryItem> = emptyList(),
)
