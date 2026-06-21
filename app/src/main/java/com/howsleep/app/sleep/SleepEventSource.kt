package com.howsleep.app.sleep

/**
 * Abstração sobre o Android Sleep API.
 * RealSleepEventSource chama ActivityRecognition; MockSleepEventSource é no-op (DEBUG only).
 */
interface SleepEventSource {
    fun subscribe()
    fun unsubscribe()
}
