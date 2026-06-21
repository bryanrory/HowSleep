package com.howsleep.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AiResponseDto(
    val id: String = "",
    val type: String = "",
    val role: String = "",
    val content: List<AiContentBlockDto> = emptyList(),
    val model: String = "",
    @SerialName("stop_reason") val stopReason: String? = null,
    val usage: AiUsageDto? = null,
)

@Serializable
data class AiContentBlockDto(
    val type: String,
    val text: String = "",
)

@Serializable
data class AiUsageDto(
    @SerialName("input_tokens") val inputTokens: Int = 0,
    @SerialName("output_tokens") val outputTokens: Int = 0,
)
