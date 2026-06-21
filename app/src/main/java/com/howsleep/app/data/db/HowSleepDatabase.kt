package com.howsleep.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.howsleep.app.data.db.converter.DateConverters
import com.howsleep.app.data.db.dao.AiChallengeDao
import com.howsleep.app.data.db.dao.ChallengeDayLogDao
import com.howsleep.app.data.db.dao.PostSleepLogDao
import com.howsleep.app.data.db.dao.PreSleepLogDao
import com.howsleep.app.data.db.dao.SleepSessionDao
import com.howsleep.app.data.db.entity.AiChallengeEntity
import com.howsleep.app.data.db.entity.ChallengeDayLogEntity
import com.howsleep.app.data.db.entity.PostSleepLogEntity
import com.howsleep.app.data.db.entity.PreSleepLogEntity
import com.howsleep.app.data.db.entity.SleepSessionEntity

@Database(
    entities = [
        SleepSessionEntity::class,
        PreSleepLogEntity::class,
        PostSleepLogEntity::class,
        AiChallengeEntity::class,
        ChallengeDayLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(DateConverters::class)
abstract class HowSleepDatabase : RoomDatabase() {
    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun preSleepLogDao(): PreSleepLogDao
    abstract fun postSleepLogDao(): PostSleepLogDao
    abstract fun aiChallengeDao(): AiChallengeDao
    abstract fun challengeDayLogDao(): ChallengeDayLogDao
}
