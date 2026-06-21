package com.howsleep.app.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AiChallengeResponse(
    val title: String,
    val description: String,
    @SerialName("habit_to_change") val habitToChange: String,
    @SerialName("habit_change_instruction") val habitChangeInstruction: String,
    @SerialName("duration_days") val durationDays: Int,
    @SerialName("success_metric_type") val successMetricType: String,
    @SerialName("success_metric_target") val successMetricTarget: Float,
    @SerialName("success_metric_direction") val successMetricDirection: String,
    val reasoning: String? = null,
)
