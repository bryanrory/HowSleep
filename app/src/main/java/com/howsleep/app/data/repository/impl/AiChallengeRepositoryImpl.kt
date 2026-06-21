package com.howsleep.app.data.repository.impl

import com.howsleep.app.data.db.dao.AiChallengeDao
import com.howsleep.app.data.db.entity.AiChallengeEntity
import com.howsleep.app.data.repository.AiChallengeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AiChallengeRepositoryImpl @Inject constructor(
    private val aiChallengeDao: AiChallengeDao,
) : AiChallengeRepository {

    override fun getActiveChallenge(): Flow<AiChallengeEntity?> =
        aiChallengeDao.getActive()

    override suspend fun getActiveChallengeOnce(): AiChallengeEntity? =
        aiChallengeDao.getActiveOnce()

    override suspend fun saveChallenge(entity: AiChallengeEntity): Result<Unit> =
        runCatching { aiChallengeDao.insert(entity) }

    override suspend fun updateChallenge(entity: AiChallengeEntity): Result<Unit> =
        runCatching { aiChallengeDao.update(entity) }
}
