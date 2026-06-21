package com.howsleep.app.sleep

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.SleepSegmentRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class RealSleepEventSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : SleepEventSource {

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, SleepReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    override fun subscribe() {
        ActivityRecognition.getClient(context)
            .requestSleepSegmentUpdates(
                pendingIntent,
                SleepSegmentRequest.getDefaultSleepSegmentRequest(),
            )
    }

    override fun unsubscribe() {
        ActivityRecognition.getClient(context).removeSleepSegmentUpdates(pendingIntent)
    }
}
