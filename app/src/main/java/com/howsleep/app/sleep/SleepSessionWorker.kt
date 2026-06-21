package com.howsleep.app.sleep

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.howsleep.app.data.db.entity.SleepSessionEntity
import com.howsleep.app.data.repository.SleepRepository
import com.howsleep.app.domain.util.TimeUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.json.Json

@HiltWorker
class SleepSessionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val sleepRepository: SleepRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val eventsJson = inputData.getString("events_json") ?: return Result.success()

        val dtos = runCatching {
            Json.decodeFromString<List<SleepEventDto>>(eventsJson)
        }.getOrNull() ?: return Result.success()

        val segments = dtos.filter { it.status == "ASLEEP" }.sortedBy { it.startMs }
        if (segments.isEmpty()) return Result.success()

        val sleepStartMs = segments.first().startMs
        val sleepEndMs = segments.last().endMs
        val totalMinutes = segments.sumOf { (it.endMs - it.startMs) / 60_000L }.toInt()
        val interruptionCount = maxOf(0, segments.size - 1)

        val timezoneId = TimeUtils.currentTimezoneId()
        val epochDay = TimeUtils.resolveSleepEpochDay(sleepStartMs, timezoneId)

        val entity = SleepSessionEntity(
            sleepEpochDay = epochDay,
            sleepStartUtcMs = sleepStartMs,
            sleepEndUtcMs = sleepEndMs,
            timezoneId = timezoneId,
            totalDurationMinutes = totalMinutes,
            confidence = CONFIDENCE_DEFAULT,
            interruptionCount = interruptionCount,
            source = "SLEEP_API",
            createdAtUtcMs = System.currentTimeMillis(),
        )

        return sleepRepository.saveSleepSession(entity).fold(
            onSuccess = { Result.success() },
            onFailure = { e -> if (e is SQLiteException) Result.retry() else Result.success() },
        )
    }

    companion object {
        private const val CONFIDENCE_DEFAULT = 85
    }
}
