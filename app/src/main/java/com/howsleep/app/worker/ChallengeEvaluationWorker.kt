package com.howsleep.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.howsleep.app.data.db.dao.AiChallengeDao
import com.howsleep.app.data.db.dao.ChallengeDayLogDao
import com.howsleep.app.data.db.dao.PostSleepLogDao
import com.howsleep.app.data.db.dao.PreSleepLogDao
import com.howsleep.app.data.db.dao.SleepSessionDao
import com.howsleep.app.data.db.entity.ChallengeDayLogEntity
import com.howsleep.app.domain.util.TimeUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.ZoneId

@HiltWorker
class ChallengeEvaluationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val aiChallengeDao: AiChallengeDao,
    private val challengeDayLogDao: ChallengeDayLogDao,
    private val preSleepLogDao: PreSleepLogDao,
    private val postSleepLogDao: PostSleepLogDao,
    private val sleepSessionDao: SleepSessionDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val challenge = aiChallengeDao.getActiveOnce() ?: return Result.success()

        val timezoneId = TimeUtils.currentTimezoneId()
        val zone = ZoneId.of(timezoneId)
        val todayEpochDay = LocalDate.now(zone).toEpochDay()

        // Só avalia dias dentro do período do desafio
        if (todayEpochDay < challenge.validFromEpochDay || todayEpochDay > challenge.validUntilEpochDay) {
            return Result.success()
        }

        val pre = preSleepLogDao.getByEpochDay(todayEpochDay)
        val post = postSleepLogDao.getByEpochDay(todayEpochDay)
        val session = sleepSessionDao.getByEpochDay(todayEpochDay)

        val (habitFollowed, evidence) = evaluateHabit(challenge.habitToChange, pre, session)
        val (outcomeValue, outcomeImproved) = evaluateOutcome(
            challenge.successMetricType,
            challenge.successMetricTarget,
            challenge.successMetricDirection,
            post,
            session,
        )

        val existing = challengeDayLogDao.getByDay(challenge.id, todayEpochDay)
        val dayLog = ChallengeDayLogEntity(
            id = existing?.id ?: 0,
            challengeId = challenge.id,
            sleepEpochDay = todayEpochDay,
            habitFollowed = habitFollowed,
            habitFollowedEvidence = evidence,
            outcomeMetricValue = outcomeValue,
            outcomeImproved = outcomeImproved,
        )
        runCatching { challengeDayLogDao.upsert(dayLog) }.onFailure {
            Log.e(TAG, "Falha ao salvar challenge_day_log", it)
            return Result.retry()
        }

        // Se é o último dia do desafio, calcula o veredicto final
        if (todayEpochDay >= challenge.validUntilEpochDay) {
            finalizeChallenge(challenge.id, challenge.durationDays)
        }

        return Result.success()
    }

    private fun evaluateHabit(
        habitToChange: String,
        pre: com.howsleep.app.data.db.entity.PreSleepLogEntity?,
        session: com.howsleep.app.data.db.entity.SleepSessionEntity?,
    ): Pair<Boolean?, String?> {
        if (pre == null) return Pair(null, "pre_sleep_log ausente")

        return when (habitToChange) {
            "CAFFEINE" -> {
                val hour = pre.caffeineLastIntakeLocalHour
                val noIntake = (pre.caffeineMg ?: 0) == 0
                val earlyIntake = hour != null && hour < 14
                val followed = noIntake || earlyIntake
                Pair(followed, "caffeine_mg=${pre.caffeineMg}, hour=$hour → $followed")
            }
            "SCREEN_TIME" -> {
                val mins = pre.screenTimeMinutes2hBefore
                val followed = mins == null || mins <= 30
                Pair(followed, "screen_time=${mins}min → $followed")
            }
            "STRESS" -> {
                val followed = pre.stressLevel <= 2
                Pair(followed, "stress_level=${pre.stressLevel} → $followed")
            }
            "MEAL_TIMING" -> {
                val lastMealMs = pre.lastMealUtcMs
                val sleepStartMs = session?.sleepStartUtcMs
                if (lastMealMs == null || sleepStartMs == null) {
                    return Pair(null, "last_meal ou sleep_start ausente")
                }
                val hoursSince = (sleepStartMs - lastMealMs) / 3_600_000f
                val followed = hoursSince >= 3f
                Pair(followed, "hours_since_last_meal=%.1f → $followed".format(hoursSince))
            }
            "EXERCISE" -> {
                Pair(pre.exerciseDone, "exercise_done=${pre.exerciseDone}")
            }
            "ALCOHOL" -> {
                val followed = (pre.alcoholUnits ?: 0f) == 0f
                Pair(followed, "alcohol_units=${pre.alcoholUnits} → $followed")
            }
            "SLEEP_SCHEDULE" -> Pair(null, "SLEEP_SCHEDULE requer confirmação manual")
            else -> Pair(null, "habit desconhecido: $habitToChange")
        }
    }

    private fun evaluateOutcome(
        metricType: String,
        target: Float,
        direction: String,
        post: com.howsleep.app.data.db.entity.PostSleepLogEntity?,
        session: com.howsleep.app.data.db.entity.SleepSessionEntity?,
    ): Pair<Float?, Boolean?> {
        val value = when (metricType) {
            "SLEEP_DURATION" -> session?.totalDurationMinutes?.let { it / 60f }
            "MOOD_SCORE" -> post?.moodScore?.toFloat()
            "ENERGY_LEVEL" -> post?.energyLevel?.toFloat()
            "PERCEIVED_QUALITY" -> post?.perceivedQuality?.toFloat()
            else -> null
        } ?: return Pair(null, null)

        val improved = if (direction == "ABOVE") value >= target else value <= target
        return Pair(value, improved)
    }

    private suspend fun finalizeChallenge(challengeId: Long, durationDays: Int) {
        val allDays = challengeDayLogDao.getForChallengeOnce(challengeId)
        val evaluated = allDays.filter { it.habitFollowed != null }
        if (evaluated.isEmpty()) return

        val minDaysForEval = durationDays * 0.5
        val followed = evaluated.count { it.habitFollowed == true }
        val adherenceRate = followed.toFloat() / evaluated.size

        val challenge = aiChallengeDao.getActiveOnce() ?: return
        val newStatus = when {
            evaluated.size < minDaysForEval -> "EXPIRED"
            adherenceRate < 0.7f -> "ABANDONED"
            else -> "COMPLETED"
        }
        runCatching {
            aiChallengeDao.update(challenge.copy(status = newStatus))
        }.onFailure {
            Log.e(TAG, "Falha ao finalizar desafio", it)
        }
    }

    companion object {
        private const val TAG = "ChallengeEvaluationWorker"
    }
}
