package com.howsleep.app.sleep

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepApiManager @Inject constructor(
    private val source: SleepEventSource,
) {
    fun start() = source.subscribe()
    fun stop() = source.unsubscribe()
}
