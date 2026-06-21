package com.howsleep.app.sleep

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.howsleep.app.data.db.dao.SleepSessionDao
import com.howsleep.app.data.db.entity.SleepSessionEntity
import com.howsleep.app.domain.util.TimeUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker de desenvolvimento: insere uma sessão de sono sintética de 8h para testar
 * o fluxo pós-sono sem depender do Android Sleep API (que não funciona em emulador).
 * Enfileirado pela SettingsScreen; ativo apenas em builds DEBUG.
 */
@HiltWorker
class MockSleepWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val sleepSessionDao: SleepSessionDao,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val nowMs = System.currentTimeMillis()
        val timezoneId = TimeUtils.currentTimezoneId()
        val sleepStartMs = nowMs - MOCK_DURATION_MS

        val entity = SleepSessionEntity(
            sleepEpochDay = TimeUtils.resolveSleepEpochDay(sleepStartMs, timezoneId),
            sleepStartUtcMs = sleepStartMs,
            sleepEndUtcMs = nowMs,
            timezoneId = timezoneId,
            totalDurationMinutes = MOCK_DURATION_MINUTES,
            confidence = 100,
            interruptionCount = 1,
            source = "MANUAL",
            createdAtUtcMs = nowMs,
        )

        return try {
            sleepSessionDao.upsert(entity)
            Result.success()
        } catch (e: SQLiteException) {
            Result.retry()
        }
    }

    companion object {
        private const val MOCK_DURATION_MINUTES = 480
        private const val MOCK_DURATION_MS = MOCK_DURATION_MINUTES * 60 * 1000L
    }
}
