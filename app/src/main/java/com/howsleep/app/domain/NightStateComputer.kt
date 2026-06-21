package com.howsleep.app.domain

enum class NightState {
    PENDING_PRE_SLEEP,
    SKIPPED_PRE_SLEEP,
    PRE_SLEEP_LOGGED,
    SLEEP_DETECTED,
    NO_SLEEP_DATA,
    POST_SLEEP_LOGGED,
    CHALLENGE_ACTIVE,
    CHALLENGE_EVALUATED,
    INCOMPLETE
}

object NightStateComputer {
    fun compute(
        hasPreSleep: Boolean,
        hasSleepSession: Boolean,
        hasPostSleep: Boolean,
        activeChallengeCoversDay: Boolean,
        evaluatedChallengeCoversDay: Boolean,
        isExpiredWithoutPostSleep: Boolean,
    ): NightState = when {
        isExpiredWithoutPostSleep -> NightState.INCOMPLETE
        evaluatedChallengeCoversDay -> NightState.CHALLENGE_EVALUATED
        activeChallengeCoversDay -> NightState.CHALLENGE_ACTIVE
        hasPostSleep -> NightState.POST_SLEEP_LOGGED
        hasSleepSession && !hasPostSleep -> NightState.SLEEP_DETECTED
        hasPreSleep && !hasSleepSession && !hasPostSleep -> NightState.PRE_SLEEP_LOGGED
        !hasPreSleep && (hasSleepSession || hasPostSleep) -> NightState.SKIPPED_PRE_SLEEP
        !hasPreSleep && !hasSleepSession && hasPostSleep -> NightState.SKIPPED_PRE_SLEEP
        else -> NightState.PENDING_PRE_SLEEP
    }
}
