package com.howsleep.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "challenge_day_log",
    indices = [
        Index(value = ["challenge_id"]),
        // Garante um registro por dia por desafio
        Index(value = ["challenge_id", "sleep_epoch_day"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = AiChallengeEntity::class,
            parentColumns = ["id"],
            childColumns = ["challenge_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class ChallengeDayLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "challenge_id") val challengeId: Long,
    @ColumnInfo(name = "sleep_epoch_day") val sleepEpochDay: Long,

    // null = não avaliado ainda; preenchido pelo ChallengeEvaluationWorker
    @ColumnInfo(name = "habit_followed") val habitFollowed: Boolean? = null,

    // Evidência legível da heurística — ex: "caffeine_last_intake_local_hour=11, target<14 → true"
    @ColumnInfo(name = "habit_followed_evidence") val habitFollowedEvidence: String? = null,

    // Valor real da success_metric_type nesta noite
    @ColumnInfo(name = "outcome_metric_value") val outcomeMetricValue: Float? = null,

    // Computed: outcomeMetricValue satisfaz successMetricTarget na direction correta?
    @ColumnInfo(name = "outcome_improved") val outcomeImproved: Boolean? = null,
)
