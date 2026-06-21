package com.howsleep.app.data.repository

import com.howsleep.app.ai.ChallengeSuggestion
import com.howsleep.app.data.db.entity.AiChallengeEntity
import com.howsleep.app.domain.model.NightDataAggregate
import kotlinx.coroutines.flow.Flow

interface AiChallengeRepository {
    fun getActiveChallenge(): Flow<AiChallengeEntity?>
    suspend fun getActiveChallengeOnce(): AiChallengeEntity?
    suspend fun saveChallenge(entity: AiChallengeEntity): Result<Unit>
    suspend fun updateChallenge(entity: AiChallengeEntity): Result<Unit>

    // Chama a API do LLM e retorna a entidade pronta (sem salvar)
    suspend fun generateChallenge(nights: List<NightDataAggregate>): Result<AiChallengeEntity>

    // Salva sugestão local ou estática com baseline calculado das noites
    suspend fun saveSuggestion(
        suggestion: ChallengeSuggestion,
        nights: List<NightDataAggregate>,
    ): Result<Unit>

    suspend fun hasChallengeGeneratedToday(): Boolean
}
