package com.howsleep.app.data.repository.impl

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.howsleep.app.data.db.dao.PostSleepLogDao
import com.howsleep.app.data.db.dao.PreSleepLogDao
import com.howsleep.app.data.db.dao.SleepSessionDao
import com.howsleep.app.data.db.entity.PostSleepLogEntity
import com.howsleep.app.data.db.entity.PreSleepLogEntity
import com.howsleep.app.data.repository.HabitLogRepository
import com.howsleep.app.domain.model.NightDataAggregate
import com.howsleep.app.domain.util.TimeUtils
import com.howsleep.app.worker.AiCallWorker
import com.howsleep.app.worker.ChallengeEvaluationWorker
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

class HabitLogRepositoryImpl @Inject constructor(
    private val preSleepLogDao: PreSleepLogDao,
    private val postSleepLogDao: PostSleepLogDao,
    private val sleepSessionDao: SleepSessionDao,
    private val workManager: WorkManager,
) : HabitLogRepository {

    override suspend fun savePreSleepLog(
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
    ): Result<Unit> = runCatching {
        val nowMs = System.currentTimeMillis()
        val timezoneId = TimeUtils.currentTimezoneId()
        val epochDay = TimeUtils.resolveSleepEpochDay(nowMs, timezoneId)
        val zone = ZoneId.of(timezoneId)
        val epochDayDate = LocalDate.ofEpochDay(epochDay)

        val caffeineUtcMs = caffeineLastIntakeLocalHour?.let { hour ->
            LocalDateTime.of(epochDayDate, LocalTime.of(hour, 0))
                .atZone(zone).toInstant().toEpochMilli()
        }
        val lastMealUtcMs = lastMealLocalHour?.let { hour ->
            LocalDateTime.of(epochDayDate, LocalTime.of(hour, 0))
                .atZone(zone).toInstant().toEpochMilli()
        }
        val caffeineLocalHour = caffeineLastIntakeLocalHour?.takeIf { caffeineMg != null && caffeineMg > 0 }

        val entity = PreSleepLogEntity(
            sleepEpochDay = epochDay,
            filledAtUtcMs = nowMs,
            timezoneId = timezoneId,
            stressLevel = stressLevel,
            exerciseDone = exerciseDone,
            caffeineMg = caffeineMg,
            caffeineLastIntakeUtcMs = caffeineUtcMs,
            caffeineLastIntakeLocalHour = caffeineLocalHour,
            screenTimeMinutes2hBefore = screenTimeMinutes,
            lastMealType = lastMealType,
            lastMealUtcMs = lastMealUtcMs,
            exerciseIntensity = exerciseIntensity?.takeIf { exerciseDone },
            exerciseMinutesBeforeBed = exerciseMinutesBeforeBed?.takeIf { exerciseDone },
            alcoholUnits = alcoholUnits,
            notes = notes?.ifBlank { null },
        )
        preSleepLogDao.upsert(entity)
    }

    override suspend fun savePostSleepLog(
        moodScore: Int,
        energyLevel: Int,
        perceivedQuality: Int,
        morningGrogginessMinutes: Int?,
        dreamRecall: Boolean,
        headache: Boolean,
        notes: String?,
    ): Result<Unit> = runCatching {
        val nowMs = System.currentTimeMillis()
        val timezoneId = TimeUtils.currentTimezoneId()
        val epochDay = TimeUtils.resolvePostSleepEpochDay(nowMs, timezoneId)

        val entity = PostSleepLogEntity(
            sleepEpochDay = epochDay,
            filledAtUtcMs = nowMs,
            timezoneId = timezoneId,
            moodScore = moodScore,
            energyLevel = energyLevel,
            perceivedQuality = perceivedQuality,
            morningGrogginessMinutes = morningGrogginessMinutes,
            dreamRecall = dreamRecall,
            headache = headache,
            notes = notes?.ifBlank { null },
        )
        postSleepLogDao.upsert(entity)

        // Dispara os workers de IA e avaliação de desafio
        workManager.enqueue(OneTimeWorkRequestBuilder<AiCallWorker>().build())
        workManager.enqueue(OneTimeWorkRequestBuilder<ChallengeEvaluationWorker>().build())
    }

    override suspend fun getPreSleepLog(epochDay: Long): PreSleepLogEntity? =
        preSleepLogDao.getByEpochDay(epochDay)

    override suspend fun getPostSleepLog(epochDay: Long): PostSleepLogEntity? =
        postSleepLogDao.getByEpochDay(epochDay)

    override fun getLatestPreSleepLogs(limit: Int): Flow<List<PreSleepLogEntity>> =
        preSleepLogDao.getLatestList(limit)

    override fun getLatestPostSleepLogs(limit: Int): Flow<List<PostSleepLogEntity>> =
        postSleepLogDao.getLatestList(limit)

    override suspend fun getLastNNights(n: Int): Result<List<NightDataAggregate>> = runCatching {
        val timezoneId = TimeUtils.currentTimezoneId()
        val zone = ZoneId.of(timezoneId)
        val toDay = LocalDate.now(zone).toEpochDay()
        val fromDay = LocalDate.now(zone).minusDays(n.toLong()).toEpochDay()

        val sessions = sleepSessionDao.getRange(fromDay, toDay)
        val preLogs = preSleepLogDao.getRange(fromDay, toDay).associateBy { it.sleepEpochDay }
        val postLogs = postSleepLogDao.getRange(fromDay, toDay).associateBy { it.sleepEpochDay }

        sessions.map { session ->
            val pre = preLogs[session.sleepEpochDay]
            val post = postLogs[session.sleepEpochDay]
            val hoursSinceLastMeal = pre?.lastMealUtcMs?.let {
                (session.sleepStartUtcMs - it) / 3_600_000f
            }
            NightDataAggregate(
                sleepEpochDay = session.sleepEpochDay,
                date = LocalDate.ofEpochDay(session.sleepEpochDay),
                totalDurationMinutes = session.totalDurationMinutes,
                confidence = session.confidence,
                interruptionCount = session.interruptionCount,
                sleepEfficiencyPercent = null,
                caffeineMg = pre?.caffeineMg,
                caffeineLastIntakeLocalHour = pre?.caffeineLastIntakeLocalHour,
                screenTimeMinutes2hBefore = pre?.screenTimeMinutes2hBefore,
                stressLevel = pre?.stressLevel,
                hoursSinceLastMeal = hoursSinceLastMeal,
                lastMealType = pre?.lastMealType,
                exerciseDone = pre?.exerciseDone,
                exerciseIntensity = pre?.exerciseIntensity,
                exerciseMinutesBeforeBed = pre?.exerciseMinutesBeforeBed,
                alcoholUnits = pre?.alcoholUnits,
                moodScore = post?.moodScore,
                energyLevel = post?.energyLevel,
                perceivedQuality = post?.perceivedQuality,
                morningGrogginessMinutes = post?.morningGrogginessMinutes,
            )
        }.takeLast(n)
    }
}
