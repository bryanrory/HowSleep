package com.howsleep.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pre_sleep_log",
    indices = [Index(value = ["sleep_epoch_day"], unique = true)]
)
data class PreSleepLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "sleep_epoch_day") val sleepEpochDay: Long,

    // Atualizado a cada REPLACE (re-submissão no mesmo dia)
    @ColumnInfo(name = "filled_at_utc_ms") val filledAtUtcMs: Long,
    @ColumnInfo(name = "timezone_id") val timezoneId: String,

    @ColumnInfo(name = "caffeine_mg") val caffeineMg: Int? = null,
    @ColumnInfo(name = "caffeine_last_intake_utc_ms") val caffeineLastIntakeUtcMs: Long? = null,

    // Derivado e armazenado no write — consultado diretamente em SQL (WHERE caffeine_last_intake_local_hour >= 14)
    @ColumnInfo(name = "caffeine_last_intake_local_hour") val caffeineLastIntakeLocalHour: Int? = null,

    // 0–120 minutos
    @ColumnInfo(name = "screen_time_minutes_2h_before") val screenTimeMinutes2hBefore: Int? = null,

    // Único campo obrigatório além de filled_at / timezone; escala 1–5
    @ColumnInfo(name = "stress_level") val stressLevel: Int,

    @ColumnInfo(name = "last_meal_utc_ms") val lastMealUtcMs: Long? = null,

    // "LIGHT" | "HEAVY" | "SNACK"
    @ColumnInfo(name = "last_meal_type") val lastMealType: String? = null,

    @ColumnInfo(name = "exercise_done") val exerciseDone: Boolean = false,

    // "LOW" | "MODERATE" | "HIGH"; null quando exerciseDone = false
    @ColumnInfo(name = "exercise_intensity") val exerciseIntensity: String? = null,

    @ColumnInfo(name = "exercise_minutes_before_bed") val exerciseMinutesBeforeBed: Int? = null,

    // 0.0–10.0 unidades padrão (~14g etanol cada)
    @ColumnInfo(name = "alcohol_units") val alcoholUnits: Float? = null,

    // Campo livre; não enviado à IA por privacidade
    @ColumnInfo(name = "notes") val notes: String? = null,
)
