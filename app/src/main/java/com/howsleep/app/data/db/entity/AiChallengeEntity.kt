package com.howsleep.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_challenge")
data class AiChallengeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Usado para checar "já gerou desafio hoje?" e evitar duplicatas
    @ColumnInfo(name = "generated_at_utc_ms") val generatedAtUtcMs: Long,

    @ColumnInfo(name = "valid_from_epoch_day") val validFromEpochDay: Long,

    // = validFromEpochDay + durationDays - 1
    @ColumnInfo(name = "valid_until_epoch_day") val validUntilEpochDay: Long,

    // max 60 chars — exibido como título na ChallengeScreen
    @ColumnInfo(name = "title") val title: String,

    // max 200 chars
    @ColumnInfo(name = "description") val description: String,

    // "CAFFEINE" | "SCREEN_TIME" | "STRESS" | "MEAL_TIMING" | "EXERCISE" | "ALCOHOL" | "SLEEP_SCHEDULE"
    @ColumnInfo(name = "habit_to_change") val habitToChange: String,

    // max 150 chars; instrução acionável exibida ao usuário
    @ColumnInfo(name = "habit_change_instruction") val habitChangeInstruction: String,

    // 5–14 dias
    @ColumnInfo(name = "duration_days") val durationDays: Int,

    // "SLEEP_DURATION" | "MOOD_SCORE" | "ENERGY_LEVEL" | "PERCEIVED_QUALITY"
    @ColumnInfo(name = "success_metric_type") val successMetricType: String,

    @ColumnInfo(name = "success_metric_target") val successMetricTarget: Float,

    // "ABOVE" | "BELOW"
    @ColumnInfo(name = "success_metric_direction") val successMetricDirection: String,

    // Média das 3 noites anteriores — snapshot imutável tirado no momento da geração
    @ColumnInfo(name = "baseline_value") val baselineValue: Float,

    // "AI_API" | "LOCAL_ENGINE" | "STATIC_DEFAULT"
    @ColumnInfo(name = "source") val source: String = "AI_API",

    // JSON do prompt enviado; apenas para source = "AI_API"; para debug/audit
    @ColumnInfo(name = "prompt_context_json") val promptContextJson: String? = null,

    // Resposta bruta pré-sanitização; para debug/audit
    @ColumnInfo(name = "raw_ai_response_json") val rawAiResponseJson: String? = null,

    // "ACTIVE" | "COMPLETED" | "ABANDONED" | "EXPIRED"
    @ColumnInfo(name = "status") val status: String = "ACTIVE",

    // Preenchidos no encerramento do desafio pelo ChallengeEvaluationWorker
    @ColumnInfo(name = "outcome_average") val outcomeAverage: Float? = null,
    @ColumnInfo(name = "outcome_delta_percent") val outcomeDeltaPercent: Float? = null,
)
