package com.howsleep.app.domain.model

import java.time.LocalDate

data class NightDataAggregate(
    val sleepEpochDay: Long,
    val date: LocalDate,
    // sleep_session (null = sem sessão detectada)
    val totalDurationMinutes: Int?,
    val confidence: Int?,
    val interruptionCount: Int?,
    val sleepEfficiencyPercent: Float?,
    // pre_sleep_log (null = usuário não preencheu)
    val caffeineMg: Int?,
    val caffeineLastIntakeLocalHour: Int?,
    val screenTimeMinutes2hBefore: Int?,
    val stressLevel: Int?,
    val hoursSinceLastMeal: Float?,
    val lastMealType: String?,
    val exerciseDone: Boolean?,
    val exerciseIntensity: String?,
    val exerciseMinutesBeforeBed: Int?,
    val alcoholUnits: Float?,
    // post_sleep_log (null = usuário não preencheu)
    val moodScore: Int?,
    val energyLevel: Int?,
    val perceivedQuality: Int?,
    val morningGrogginessMinutes: Int?,
)
