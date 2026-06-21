package com.howsleep.app.domain.util

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Utilitário puro de tempo — zero imports Android. Testável com JUnit puro.
 *
 * Regra de sleep_epoch_day:
 *   Pré-sono:  hora local 00:00–03:59 → epoch day do dia anterior (quem dorme à 1h estava "ontem")
 *              hora local 04:00+       → epoch day do dia atual
 *   Pós-sono:  hora local < 12:00     → noite anterior (quem acorda às 7h dormiu ontem)
 *              hora local >= 12:00    → dia atual (preenchimento tardio)
 */
object TimeUtils {

    fun resolveSleepEpochDay(utcMs: Long, timezoneId: String): Long {
        val local = toZonedDateTime(utcMs, timezoneId)
        return if (local.hour < 4) {
            local.toLocalDate().minusDays(1).toEpochDay()
        } else {
            local.toLocalDate().toEpochDay()
        }
    }

    fun resolvePostSleepEpochDay(utcMs: Long, timezoneId: String): Long {
        val local = toZonedDateTime(utcMs, timezoneId)
        return if (local.hour < 12) {
            local.toLocalDate().minusDays(1).toEpochDay()
        } else {
            local.toLocalDate().toEpochDay()
        }
    }

    /** Extrai a hora local (0–23) de um timestamp UTC. Resultado armazenado nos campos _local_hour. */
    fun localHourFromUtcMs(utcMs: Long, timezoneId: String): Int =
        toZonedDateTime(utcMs, timezoneId).hour

    fun currentTimezoneId(): String = ZoneId.systemDefault().id

    private fun toZonedDateTime(utcMs: Long, timezoneId: String): ZonedDateTime =
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(utcMs), ZoneId.of(timezoneId))
}
