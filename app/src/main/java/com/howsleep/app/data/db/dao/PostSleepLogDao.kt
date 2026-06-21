package com.howsleep.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.howsleep.app.data.db.entity.PostSleepLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostSleepLogDao {

    // REPLACE: permite correção até 23:59 do mesmo dia; trigger do AiCallWorker via repository
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PostSleepLogEntity): Long

    @Query("SELECT * FROM post_sleep_log WHERE sleep_epoch_day = :epochDay LIMIT 1")
    suspend fun getByEpochDay(epochDay: Long): PostSleepLogEntity?

    @Query("SELECT * FROM post_sleep_log ORDER BY sleep_epoch_day DESC LIMIT 1")
    fun getLatest(): Flow<PostSleepLogEntity?>

    @Query("SELECT * FROM post_sleep_log WHERE sleep_epoch_day BETWEEN :fromDay AND :toDay ORDER BY sleep_epoch_day ASC")
    suspend fun getRange(fromDay: Long, toDay: Long): List<PostSleepLogEntity>
}
