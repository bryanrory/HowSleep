package com.howsleep.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sleep_session",
    indices = [Index(value = ["sleep_epoch_day"], unique = true)]
)
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Chave natural de JOIN com pre_sleep_log e post_sleep_log
    @ColumnInfo(name = "sleep_epoch_day") val sleepEpochDay: Long,

    @ColumnInfo(name = "sleep_start_utc_ms") val sleepStartUtcMs: Long,
    @ColumnInfo(name = "sleep_end_utc_ms") val sleepEndUtcMs: Long,

    // Congelado no momento do evento para queries de hora local
    @ColumnInfo(name = "timezone_id") val timezoneId: String,

    // Pré-computado no write para evitar cálculo em query
    @ColumnInfo(name = "total_duration_minutes") val totalDurationMinutes: Int,

    // 0–100; usado para exibir aviso "dados incertos" na UI
    @ColumnInfo(name = "confidence") val confidence: Int,

    @ColumnInfo(name = "interruption_count") val interruptionCount: Int = 0,

    // Reservados para integração futura com wearables
    @ColumnInfo(name = "light_sleep_minutes") val lightSleepMinutes: Int? = null,
    @ColumnInfo(name = "deep_sleep_minutes") val deepSleepMinutes: Int? = null,

    // "SLEEP_API" | "MANUAL"
    @ColumnInfo(name = "source") val source: String = "SLEEP_API",

    @ColumnInfo(name = "created_at_utc_ms") val createdAtUtcMs: Long,
)
