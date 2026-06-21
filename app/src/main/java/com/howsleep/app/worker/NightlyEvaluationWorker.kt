package com.howsleep.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.howsleep.app.data.db.dao.AiChallengeDao
import com.howsleep.app.data.db.dao.ChallengeDayLogDao
import com.howsleep.app.domain.util.TimeUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.ZoneId

@HiltWorker
class NightlyEvaluationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val aiChallengeDao: AiChallengeDao,
    private val challengeDayLogDao: ChallengeDayLogDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val challenge = aiChallengeDao.getActiveOnce() ?: return Result.success()

        val zone = ZoneId.of(TimeUtils.currentTimezoneId())
        val todayEpochDay = LocalDate.now(zone).toEpochDay()

        // Só age se o prazo do desafio já passou
        if (challenge.validUntilEpochDay >= todayEpochDay) return Result.success()

        val allDays = challengeDayLogDao.getForChallengeOnce(challenge.id)
        val evaluated = allDays.filter { it.habitFollowed != null }
        val minDays = challenge.durationDays * 0.5

        val newStatus = when {
            evaluated.size < minDays -> "EXPIRED"
            evaluated.count { it.habitFollowed == true }.toFloat() / evaluated.size < 0.7f -> "ABANDONED"
            else -> "COMPLETED"
        }

        runCatching {
            aiChallengeDao.update(challenge.copy(status = newStatus))
        }.onFailure {
            Log.e(TAG, "Falha ao finalizar desafio expirado", it)
            return Result.retry()
        }

        return Result.success()
    }

    companion object {
        private const val TAG = "NightlyEvaluationWorker"
    }
}
