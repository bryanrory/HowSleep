package com.howsleep.app.data.repository.impl

import com.howsleep.app.BuildConfig
import com.howsleep.app.ai.AiChallengeResponse
import com.howsleep.app.ai.AiResponseSanitizer
import com.howsleep.app.ai.ChallengeValidator
import com.howsleep.app.ai.ChallengeSuggestion
import com.howsleep.app.ai.PromptBuilder
import com.howsleep.app.data.db.dao.AiChallengeDao
import com.howsleep.app.data.db.entity.AiChallengeEntity
import com.howsleep.app.data.remote.api.AiChallengeApi
import com.howsleep.app.data.remote.dto.AiMessageDto
import com.howsleep.app.data.remote.dto.AiRequestDto
import com.howsleep.app.data.remote.mapper.AiResponseMapper
import com.howsleep.app.data.repository.AiChallengeRepository
import com.howsleep.app.domain.model.NightDataAggregate
import com.howsleep.app.domain.util.TimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

class AiChallengeRepositoryImpl @Inject constructor(
    private val aiChallengeDao: AiChallengeDao,
    private val aiChallengeApi: AiChallengeApi,
    private val json: Json,
) : AiChallengeRepository {

    override fun getActiveChallenge(): Flow<AiChallengeEntity?> =
        aiChallengeDao.getActive()

    override suspend fun getActiveChallengeOnce(): AiChallengeEntity? =
        aiChallengeDao.getActiveOnce()

    override suspend fun saveChallenge(entity: AiChallengeEntity): Result<Unit> =
        runCatching { aiChallengeDao.insert(entity) }

    override suspend fun updateChallenge(entity: AiChallengeEntity): Result<Unit> =
        runCatching { aiChallengeDao.update(entity) }

    override suspend fun generateChallenge(nights: List<NightDataAggregate>): Result<AiChallengeEntity> =
        runCatching {
            val userContent = PromptBuilder.buildUserContent(nights)
            val requestDto = AiRequestDto(
                model = "claude-sonnet-4-6",
                maxTokens = 512,
                system = PromptBuilder.SYSTEM_PROMPT,
                messages = listOf(AiMessageDto(role = "user", content = userContent)),
            )
            val promptJson = json.encodeToString(AiRequestDto.serializer(), requestDto)

            val rawResponse = aiChallengeApi.generateChallenge(
                apiKey = BuildConfig.LLM_API_KEY,
                request = requestDto,
            )
            val rawText = rawResponse.content.firstOrNull { it.type == "text" }?.text
                ?: error("LLM returned no text content")

            val sanitized = AiResponseSanitizer.sanitize(rawText)
            val parsed = json.decodeFromString<AiChallengeResponse>(sanitized)
            val validated = ChallengeValidator.validate(parsed)

            val todayEpochDay = todayEpochDay()
            val baseline = computeBaseline(nights, validated.successMetricType)

            AiResponseMapper.toEntity(
                response = validated,
                baselineValue = baseline,
                validFromEpochDay = todayEpochDay,
                promptContextJson = promptJson,
                rawAiResponseJson = rawText,
            )
        }

    override suspend fun saveSuggestion(
        suggestion: ChallengeSuggestion,
        nights: List<NightDataAggregate>,
    ): Result<Unit> = runCatching {
        val baseline = computeBaseline(nights, suggestion.successMetricType)
        val entity = AiResponseMapper.suggestionToEntity(
            suggestion = suggestion,
            baselineValue = baseline,
            validFromEpochDay = todayEpochDay(),
        )
        aiChallengeDao.insert(entity)
    }

    override suspend fun hasChallengeGeneratedToday(): Boolean {
        val zoneId = ZoneId.of(TimeUtils.currentTimezoneId())
        val todayStart = LocalDateTime.of(LocalDate.now(zoneId), LocalTime.MIDNIGHT)
            .atZone(zoneId).toInstant().toEpochMilli()
        val todayEnd = todayStart + 86_400_000L
        return aiChallengeDao.countGeneratedBetween(todayStart, todayEnd) > 0
    }

    private fun computeBaseline(nights: List<NightDataAggregate>, metricType: String): Float {
        val last3 = nights.takeLast(3)
        val values = last3.mapNotNull { night ->
            when (metricType) {
                "SLEEP_DURATION" -> night.totalDurationMinutes?.let { it / 60f }
                "MOOD_SCORE" -> night.moodScore?.toFloat()
                "ENERGY_LEVEL" -> night.energyLevel?.toFloat()
                "PERCEIVED_QUALITY" -> night.perceivedQuality?.toFloat()
                else -> null
            }
        }
        return if (values.isEmpty()) 0f else values.average().toFloat()
    }

    override fun getChallengeHistory(): Flow<List<AiChallengeEntity>> =
        aiChallengeDao.getHistory()

    private fun todayEpochDay(): Long {
        val zone = ZoneId.of(TimeUtils.currentTimezoneId())
        return LocalDate.now(zone).toEpochDay()
    }
}
