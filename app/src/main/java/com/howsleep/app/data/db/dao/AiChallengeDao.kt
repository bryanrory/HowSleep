package com.howsleep.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.howsleep.app.data.db.entity.AiChallengeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiChallengeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AiChallengeEntity): Long

    @Update
    suspend fun update(entity: AiChallengeEntity)

    @Query("SELECT * FROM ai_challenge WHERE status = 'ACTIVE' ORDER BY generated_at_utc_ms DESC LIMIT 1")
    fun getActive(): Flow<AiChallengeEntity?>

    @Query("SELECT * FROM ai_challenge WHERE status = 'ACTIVE' ORDER BY generated_at_utc_ms DESC LIMIT 1")
    suspend fun getActiveOnce(): AiChallengeEntity?

    @Query("SELECT * FROM ai_challenge ORDER BY generated_at_utc_ms DESC")
    fun getAll(): Flow<List<AiChallengeEntity>>

    // Verifica se já existe desafio gerado no epoch day atual para evitar duplicatas
    @Query("SELECT COUNT(*) FROM ai_challenge WHERE generated_at_utc_ms >= :dayStartUtcMs AND generated_at_utc_ms < :dayEndUtcMs")
    suspend fun countGeneratedBetween(dayStartUtcMs: Long, dayEndUtcMs: Long): Int

    @Query("SELECT * FROM ai_challenge WHERE status != 'ACTIVE' ORDER BY generated_at_utc_ms DESC")
    fun getHistory(): Flow<List<AiChallengeEntity>>
}
