package com.howsleep.app.sleep

import javax.inject.Inject

/**
 * Implementação de no-op usada em builds DEBUG.
 * Sessões sintéticas são geradas pelo MockSleepWorker, disparado manualmente via SettingsScreen.
 */
class MockSleepEventSource @Inject constructor() : SleepEventSource {
    override fun subscribe() {}
    override fun unsubscribe() {}
}
