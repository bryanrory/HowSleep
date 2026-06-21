package com.howsleep.app.ai

data class ChallengeSuggestion(
    val title: String,
    val description: String,
    val habitToChange: String,
    val habitChangeInstruction: String,
    val durationDays: Int,
    val successMetricType: String,
    val successMetricTarget: Float,
    val successMetricDirection: String,
    val source: String,
)
