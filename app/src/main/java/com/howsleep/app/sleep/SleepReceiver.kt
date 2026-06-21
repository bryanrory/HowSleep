package com.howsleep.app.sleep

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.gms.location.SleepSegmentEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// R-04: BroadcastReceiver ULTRALEVE — zero IO, zero DAO. Apenas serializa e enfileira o Worker.
class SleepReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val events = SleepSegmentEvent.extractEvents(intent)
        if (events.isEmpty()) return

        val dtos = events
            .filter { it.status == SleepSegmentEvent.STATUS_SUCCESSFUL }
            .map { SleepEventDto(startMs = it.startTimeMillis, endMs = it.endTimeMillis, status = "ASLEEP") }

        if (dtos.isEmpty()) return

        val eventsJson = Json.encodeToString(dtos)
        val request = OneTimeWorkRequestBuilder<SleepSessionWorker>()
            .setInputData(workDataOf("events_json" to eventsJson))
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
