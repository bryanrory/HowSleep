package com.howsleep.app.data.repository

import com.howsleep.app.data.db.entity.AiChallengeEntity
import kotlinx.coroutines.flow.Flow

interface AiChallengeRepository {
    fun getActiveChallenge(): Flow<AiChallengeEntity?>
    suspend fun getActiveChallengeOnce(): AiChallengeEntity?
    suspend fun saveChallenge(entity: AiChallengeEntity): Result<Unit>
    suspend fun updateChallenge(entity: AiChallengeEntity): Result<Unit>
}
