package com.howsleep.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "post_sleep_log",
    indices = [Index(value = ["sleep_epoch_day"], unique = true)]
)
data class PostSleepLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "sleep_epoch_day") val sleepEpochDay: Long,

    // Usado para verificar janela de corte das 12:00
    @ColumnInfo(name = "filled_at_utc_ms") val filledAtUtcMs: Long,
    @ColumnInfo(name = "timezone_id") val timezoneId: String,

    // Campos obrigatórios; escala 1–5; podem ser success_metric_type de um desafio
    @ColumnInfo(name = "mood_score") val moodScore: Int,
    @ColumnInfo(name = "energy_level") val energyLevel: Int,
    @ColumnInfo(name = "perceived_quality") val perceivedQuality: Int,

    // Delta com sleep_session.total_duration_minutes é insight por si só
    @ColumnInfo(name = "perceived_duration_hours") val perceivedDurationHours: Float? = null,

    // 0–120 min; correlaciona com qualidade do sono profundo
    @ColumnInfo(name = "morning_grogginess_minutes") val morningGrogginessMinutes: Int? = null,

    // Indicador indireto de sono REM
    @ColumnInfo(name = "dream_recall") val dreamRecall: Boolean = false,

    // Correlaciona com álcool e apneia
    @ColumnInfo(name = "headache") val headache: Boolean = false,

    // Campo livre; não enviado à IA por privacidade
    @ColumnInfo(name = "notes") val notes: String? = null,
)
