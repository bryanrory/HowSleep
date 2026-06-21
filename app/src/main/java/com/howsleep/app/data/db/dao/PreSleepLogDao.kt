package com.howsleep.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.howsleep.app.data.db.entity.PreSleepLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PreSleepLogDao {

    // REPLACE: permite re-submissão do formulário no mesmo dia
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PreSleepLogEntity): Long

    @Query("SELECT * FROM pre_sleep_log WHERE sleep_epoch_day = :epochDay LIMIT 1")
    suspend fun getByEpochDay(epochDay: Long): PreSleepLogEntity?

    @Query("SELECT * FROM pre_sleep_log ORDER BY sleep_epoch_day DESC LIMIT 1")
    fun getLatest(): Flow<PreSleepLogEntity?>

    @Query("SELECT * FROM pre_sleep_log WHERE sleep_epoch_day BETWEEN :fromDay AND :toDay ORDER BY sleep_epoch_day ASC")
    suspend fun getRange(fromDay: Long, toDay: Long): List<PreSleepLogEntity>
}
