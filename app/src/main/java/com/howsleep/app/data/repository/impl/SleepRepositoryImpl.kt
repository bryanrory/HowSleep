package com.howsleep.app.data.repository.impl

import com.howsleep.app.data.db.dao.SleepSessionDao
import com.howsleep.app.data.db.entity.SleepSessionEntity
import com.howsleep.app.data.repository.SleepRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SleepRepositoryImpl @Inject constructor(
    private val sleepSessionDao: SleepSessionDao,
) : SleepRepository {

    override fun getLast7Nights(): Flow<List<SleepSessionEntity>> =
        sleepSessionDao.getLatest(7)

    override suspend fun saveSleepSession(entity: SleepSessionEntity): Result<Unit> =
        runCatching { sleepSessionDao.upsert(entity) }

    override suspend fun getByEpochDay(epochDay: Long): SleepSessionEntity? =
        sleepSessionDao.getByEpochDay(epochDay)
}
