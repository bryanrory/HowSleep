package com.howsleep.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AiRequestDto(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val system: String,
    val messages: List<AiMessageDto>,
)

@Serializable
data class AiMessageDto(
    val role: String,
    val content: String,
)
