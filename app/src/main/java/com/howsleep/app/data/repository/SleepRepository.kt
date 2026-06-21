package com.howsleep.app.data.repository

import com.howsleep.app.data.db.entity.SleepSessionEntity
import kotlinx.coroutines.flow.Flow

interface SleepRepository {
    fun getLast7Nights(): Flow<List<SleepSessionEntity>>
    suspend fun saveSleepSession(entity: SleepSessionEntity): Result<Unit>
    suspend fun getByEpochDay(epochDay: Long): SleepSessionEntity?
}
