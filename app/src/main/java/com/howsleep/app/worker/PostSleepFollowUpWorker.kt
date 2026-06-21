package com.howsleep.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.howsleep.app.data.db.dao.PostSleepLogDao
import com.howsleep.app.domain.util.TimeUtils
import com.howsleep.app.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.time.ZoneId

@HiltWorker
class PostSleepFollowUpWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val postSleepLogDao: PostSleepLogDao,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val zone = ZoneId.of(TimeUtils.currentTimezoneId())
        // Verifica se o pós-sono da noite anterior foi preenchido
        val yesterdayEpochDay = LocalDate.now(zone).minusDays(1).toEpochDay()
        val hasLog = postSleepLogDao.getByEpochDay(yesterdayEpochDay) != null
        if (!hasLog) {
            notificationHelper.sendPostSleepFollowUpReminder()
        }
        return Result.success()
    }
}
