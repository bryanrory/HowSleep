package com.howsleep.app.ui.dashboard

import java.time.LocalDate

data class DashboardUiState(
    val nights: List<DashboardNightItem> = emptyList(),
    val isLoading: Boolean = false,
)

data class DashboardNightItem(
    val sleepEpochDay: Long,
    val localDate: LocalDate,
    val durationHours: Float? = null,
    val perceivedQuality: Int? = null,
    val moodScore: Int? = null,
    val energyLevel: Int? = null,
    val hasPreLog: Boolean = false,
    val hasPostLog: Boolean = false,
    val isLowConfidence: Boolean = false,
)
