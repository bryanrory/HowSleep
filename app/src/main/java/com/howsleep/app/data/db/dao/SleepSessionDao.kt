package com.howsleep.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.howsleep.app.data.db.entity.SleepSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepSessionDao {

    // REPLACE: Sleep API pode emitir múltiplos eventos para a mesma noite
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SleepSessionEntity): Long

    @Query("SELECT * FROM sleep_session WHERE sleep_epoch_day = :epochDay LIMIT 1")
    suspend fun getByEpochDay(epochDay: Long): SleepSessionEntity?

    // Últimas N noites ordenadas do mais recente para o mais antigo — alimenta o Dashboard
    @Query("SELECT * FROM sleep_session ORDER BY sleep_epoch_day DESC LIMIT :limit")
    fun getLatest(limit: Int): Flow<List<SleepSessionEntity>>

    @Query("SELECT * FROM sleep_session WHERE sleep_epoch_day BETWEEN :fromDay AND :toDay ORDER BY sleep_epoch_day ASC")
    suspend fun getRange(fromDay: Long, toDay: Long): List<SleepSessionEntity>
}
