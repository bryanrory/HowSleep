package com.howsleep.app.sleep

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class RealSleepEventSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : SleepEventSource {

    // Fase 2: requestSleepSegmentUpdates via ActivityRecognition + PendingIntent → SleepReceiver
    override fun subscribe() {}

    // Fase 2: removeSleepSegmentUpdates
    override fun unsubscribe() {}
}
