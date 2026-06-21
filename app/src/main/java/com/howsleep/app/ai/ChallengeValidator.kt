package com.howsleep.app.ai

object ChallengeValidator {

    private val VALID_HABITS = setOf(
        "CAFFEINE", "SCREEN_TIME", "STRESS", "MEAL_TIMING",
        "EXERCISE", "ALCOHOL", "SLEEP_SCHEDULE"
    )
    private val VALID_METRICS = setOf(
        "SLEEP_DURATION", "MOOD_SCORE", "ENERGY_LEVEL", "PERCEIVED_QUALITY"
    )
    private val VALID_DIRECTIONS = setOf("ABOVE", "BELOW")

    fun validate(response: AiChallengeResponse): AiChallengeResponse {
        require(response.title.isNotBlank()) { "title is blank" }
        require(response.habitToChange in VALID_HABITS) {
            "invalid habit_to_change: ${response.habitToChange}"
        }
        require(response.durationDays in 5..14) {
            "duration_days must be 5–14, was ${response.durationDays}"
        }
        require(response.successMetricType in VALID_METRICS) {
            "invalid success_metric_type: ${response.successMetricType}"
        }
        require(response.successMetricTarget > 0f) {
            "success_metric_target must be > 0"
        }
        require(response.successMetricDirection in VALID_DIRECTIONS) {
            "invalid success_metric_direction: ${response.successMetricDirection}"
        }

        // Soft validations: truncate strings that are too long
        return response.copy(
            title = response.title.take(60),
            description = response.description.take(200),
            habitChangeInstruction = response.habitChangeInstruction.take(150),
            reasoning = response.reasoning?.take(300),
        )
    }
}
