package com.howsleep.app.ui.trends

import java.time.LocalDate

enum class TrendsPeriod(val days: Int, val label: String) {
    WEEK(7, "7 dias"),
    MONTH(30, "30 dias"),
}

data class TrendNightItem(
    val epochDay: Long,
    val date: LocalDate,
    val durationHours: Float?,
    val perceivedQuality: Int?,
    val moodScore: Int?,
    val energyLevel: Int?,
)

data class TrendsUiState(
    val isLoading: Boolean = true,
    val period: TrendsPeriod = TrendsPeriod.WEEK,
    val nights: List<TrendNightItem> = emptyList(),
    val avgDurationHours: Float? = null,
    val avgQuality: Float? = null,
    val avgMood: Float? = null,
)
