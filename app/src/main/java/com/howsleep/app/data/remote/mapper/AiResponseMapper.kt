package com.howsleep.app.data.remote.mapper

import com.howsleep.app.ai.AiChallengeResponse
import com.howsleep.app.ai.ChallengeSuggestion
import com.howsleep.app.data.db.entity.AiChallengeEntity
import java.time.LocalDate

object AiResponseMapper {

    fun toEntity(
        response: AiChallengeResponse,
        baselineValue: Float,
        validFromEpochDay: Long,
        promptContextJson: String,
        rawAiResponseJson: String,
    ): AiChallengeEntity = AiChallengeEntity(
        generatedAtUtcMs = System.currentTimeMillis(),
        validFromEpochDay = validFromEpochDay,
        validUntilEpochDay = validFromEpochDay + response.durationDays - 1,
        title = response.title,
        description = response.description,
        habitToChange = response.habitToChange,
        habitChangeInstruction = response.habitChangeInstruction,
        durationDays = response.durationDays,
        successMetricType = response.successMetricType,
        successMetricTarget = response.successMetricTarget,
        successMetricDirection = response.successMetricDirection,
        baselineValue = baselineValue,
        source = "AI_API",
        promptContextJson = promptContextJson,
        rawAiResponseJson = rawAiResponseJson,
        status = "ACTIVE",
    )

    fun suggestionToEntity(
        suggestion: ChallengeSuggestion,
        baselineValue: Float,
        validFromEpochDay: Long,
    ): AiChallengeEntity = AiChallengeEntity(
        generatedAtUtcMs = System.currentTimeMillis(),
        validFromEpochDay = validFromEpochDay,
        validUntilEpochDay = validFromEpochDay + suggestion.durationDays - 1,
        title = suggestion.title,
        description = suggestion.description,
        habitToChange = suggestion.habitToChange,
        habitChangeInstruction = suggestion.habitChangeInstruction,
        durationDays = suggestion.durationDays,
        successMetricType = suggestion.successMetricType,
        successMetricTarget = suggestion.successMetricTarget,
        successMetricDirection = suggestion.successMetricDirection,
        baselineValue = baselineValue,
        source = suggestion.source,
        promptContextJson = null,
        rawAiResponseJson = null,
        status = "ACTIVE",
    )
}
