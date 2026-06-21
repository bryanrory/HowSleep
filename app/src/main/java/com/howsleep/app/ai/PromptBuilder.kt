package com.howsleep.app.ai

import com.howsleep.app.domain.model.NightDataAggregate
import java.time.format.DateTimeFormatter

object PromptBuilder {

    const val SYSTEM_PROMPT = "You are a sleep science advisor analyzing behavioral data from a " +
        "sleep tracking app. Your role is to identify the single most impactful habit the user " +
        "can change to improve their sleep quality. You must respond ONLY with a valid JSON object " +
        "matching the exact schema provided in the user message. Do not include explanations, " +
        "markdown formatting, code blocks, or any text outside the JSON object. Focus on patterns " +
        "that appear in at least 2 of the nights provided. Prioritize habits with clear correlation " +
        "to poor sleep outcomes. Be specific and actionable."

    private val DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE

    fun buildUserContent(nights: List<NightDataAggregate>): String {
        val avgDuration = nights.mapNotNull { it.totalDurationMinutes }
            .takeIf { it.isNotEmpty() }?.average()?.let { it / 60.0 }
        val avgQuality = nights.mapNotNull { it.perceivedQuality }
            .takeIf { it.isNotEmpty() }?.average()
        val avgMood = nights.mapNotNull { it.moodScore }
            .takeIf { it.isNotEmpty() }?.average()
        val avgEnergy = nights.mapNotNull { it.energyLevel }
            .takeIf { it.isNotEmpty() }?.average()
        val caffeineAfter14h = nights.count {
            (it.caffeineMg ?: 0) > 0 && (it.caffeineLastIntakeLocalHour ?: 0) >= 14
        }
        val screenOver60 = nights.count { (it.screenTimeMinutes2hBefore ?: 0) > 60 }
        val withExercise = nights.count { it.exerciseDone == true }
        val dateRange = if (nights.isNotEmpty()) {
            "${nights.first().date.format(DATE_FMT)} to ${nights.last().date.format(DATE_FMT)}"
        } else ""

        val nightsData = nights.joinToString(",") { nightJson(it) }

        return """{"request":"analyze_sleep_habits","nights_analyzed":${nights.size},"date_range":"$dateRange","user_context":{"avg_sleep_duration_hours":${avgDuration?.fmt1 ?: "null"},"avg_perceived_quality":${avgQuality?.fmt1 ?: "null"},"avg_mood_score":${avgMood?.fmt1 ?: "null"},"avg_energy_level":${avgEnergy?.fmt1 ?: "null"},"nights_with_caffeine_after_14h":$caffeineAfter14h,"nights_with_screen_time_over_60min":$screenOver60,"nights_with_exercise":$withExercise},"sleep_data":[$nightsData],"response_schema":{"title":"string (max 60 chars)","description":"string (max 200 chars)","habit_to_change":"one of: CAFFEINE | SCREEN_TIME | STRESS | MEAL_TIMING | EXERCISE | ALCOHOL | SLEEP_SCHEDULE","habit_change_instruction":"string (specific and actionable, max 150 chars)","duration_days":"integer between 5 and 14","success_metric_type":"one of: SLEEP_DURATION | MOOD_SCORE | ENERGY_LEVEL | PERCEIVED_QUALITY","success_metric_target":"float","success_metric_direction":"one of: ABOVE | BELOW","reasoning":"string (max 300 chars)"}}"""
    }

    private fun nightJson(n: NightDataAggregate): String {
        val durHours = n.totalDurationMinutes?.let { it / 60.0 }
        return """{"date":"${n.date.format(DATE_FMT)}","pre_sleep":{"caffeine_mg":${n.caffeineMg ?: "null"},"caffeine_last_intake_local_hour":${n.caffeineLastIntakeLocalHour ?: "null"},"screen_time_minutes_2h_before":${n.screenTimeMinutes2hBefore ?: "null"},"stress_level":${n.stressLevel ?: "null"},"hours_since_last_meal":${n.hoursSinceLastMeal?.fmt1 ?: "null"},"last_meal_type":${n.lastMealType?.quoted ?: "null"},"exercise_done":${n.exerciseDone ?: "null"},"exercise_intensity":${n.exerciseIntensity?.quoted ?: "null"},"exercise_minutes_before_bed":${n.exerciseMinutesBeforeBed ?: "null"},"alcohol_units":${n.alcoholUnits ?: "null"}},"sleep":{"total_duration_minutes":${n.totalDurationMinutes ?: "null"},"total_duration_hours":${durHours?.fmt2 ?: "null"},"confidence":${n.confidence ?: "null"},"interruption_count":${n.interruptionCount ?: "null"},"sleep_efficiency_percent":${n.sleepEfficiencyPercent?.fmt1 ?: "null"}},"post_sleep":{"mood_score":${n.moodScore ?: "null"},"energy_level":${n.energyLevel ?: "null"},"perceived_quality":${n.perceivedQuality ?: "null"},"morning_grogginess_minutes":${n.morningGrogginessMinutes ?: "null"}}}"""
    }

    private val Double.fmt1: String get() = "%.1f".format(this)
    private val Double.fmt2: String get() = "%.2f".format(this)
    private val Float.fmt1: String get() = "%.1f".format(this)
    private val String.quoted: String get() = "\"$this\""
}
