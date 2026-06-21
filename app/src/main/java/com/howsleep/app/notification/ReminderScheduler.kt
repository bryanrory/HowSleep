package com.howsleep.app.notification

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.howsleep.app.worker.NightlyEvaluationWorker
import com.howsleep.app.worker.PreSleepReminderWorker
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    private val workManager: WorkManager,
) {

    fun schedulePreSleepReminder(hour: Int, minute: Int) {
        val now = LocalDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        val initialDelayMs = Duration.between(now, target).toMillis()

        val request = PeriodicWorkRequestBuilder<PreSleepReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .addTag(TAG_PRE_SLEEP_REMINDER)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_PRE_SLEEP_REMINDER,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request,
        )
    }

    fun cancelPreSleepReminder() {
        workManager.cancelUniqueWork(WORK_PRE_SLEEP_REMINDER)
    }

    fun scheduleNightlyEvaluation() {
        val now = LocalDateTime.now()
        var target = now.withHour(6).withMinute(0).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        val initialDelayMs = Duration.between(now, target).toMillis()

        val request = PeriodicWorkRequestBuilder<NightlyEvaluationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .addTag(TAG_NIGHTLY_EVALUATION)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NIGHTLY_EVALUATION,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        const val TAG_PRE_SLEEP_REMINDER = "pre_sleep_reminder"
        const val TAG_NIGHTLY_EVALUATION = "nightly_evaluation"
        private const val WORK_PRE_SLEEP_REMINDER = "pre_sleep_reminder"
        private const val WORK_NIGHTLY_EVALUATION = "nightly_evaluation"
    }
}
