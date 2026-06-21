package com.howsleep.app.data.remote.api

import com.howsleep.app.data.remote.dto.AiRequestDto
import com.howsleep.app.data.remote.dto.AiResponseDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AiChallengeApi {

    @POST("v1/messages")
    suspend fun generateChallenge(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Body request: AiRequestDto,
    ): AiResponseDto
}
