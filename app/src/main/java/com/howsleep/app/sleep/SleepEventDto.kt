package com.howsleep.app.sleep

import kotlinx.serialization.Serializable

@Serializable
data class SleepEventDto(
    val startMs: Long,
    val endMs: Long,
    val status: String, // "ASLEEP" | "AWAKE"
)
