package com.howsleep.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.howsleep.app.data.db.entity.ChallengeDayLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeDayLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ChallengeDayLogEntity): Long

    @Update
    suspend fun update(entity: ChallengeDayLogEntity)

    @Query("SELECT * FROM challenge_day_log WHERE challenge_id = :challengeId ORDER BY sleep_epoch_day ASC")
    fun getForChallenge(challengeId: Long): Flow<List<ChallengeDayLogEntity>>

    @Query("SELECT * FROM challenge_day_log WHERE challenge_id = :challengeId ORDER BY sleep_epoch_day ASC")
    suspend fun getForChallengeOnce(challengeId: Long): List<ChallengeDayLogEntity>

    @Query("SELECT * FROM challenge_day_log WHERE challenge_id = :challengeId AND sleep_epoch_day = :epochDay LIMIT 1")
    suspend fun getByDay(challengeId: Long, epochDay: Long): ChallengeDayLogEntity?
}
