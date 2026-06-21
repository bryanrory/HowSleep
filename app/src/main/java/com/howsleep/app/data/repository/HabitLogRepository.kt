package com.howsleep.app.data.repository

import com.howsleep.app.data.db.entity.PostSleepLogEntity
import com.howsleep.app.data.db.entity.PreSleepLogEntity
import com.howsleep.app.domain.model.NightDataAggregate
import kotlinx.coroutines.flow.Flow

interface HabitLogRepository {

    suspend fun savePreSleepLog(
        stressLevel: Int,
        exerciseDone: Boolean,
        caffeineMg: Int?,
        caffeineLastIntakeLocalHour: Int?,
        screenTimeMinutes: Int?,
        lastMealType: String?,
        lastMealLocalHour: Int?,
        exerciseIntensity: String?,
        exerciseMinutesBeforeBed: Int?,
        alcoholUnits: Float?,
        notes: String?,
    ): Result<Unit>

    suspend fun savePostSleepLog(
        moodScore: Int,
        energyLevel: Int,
        perceivedQuality: Int,
        morningGrogginessMinutes: Int?,
        dreamRecall: Boolean,
        headache: Boolean,
        notes: String?,
    ): Result<Unit>

    suspend fun getPreSleepLog(epochDay: Long): PreSleepLogEntity?
    suspend fun getPostSleepLog(epochDay: Long): PostSleepLogEntity?

    fun getLatestPreSleepLogs(limit: Int): Flow<List<PreSleepLogEntity>>
    fun getLatestPostSleepLogs(limit: Int): Flow<List<PostSleepLogEntity>>

    // Usado pelo PromptBuilder na Fase 3
    suspend fun getLastNNights(n: Int): Result<List<NightDataAggregate>>
}
