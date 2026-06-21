package com.howsleep.app.data.di

import android.content.Context
import androidx.room.Room
import com.howsleep.app.data.db.HowSleepDatabase
import com.howsleep.app.data.db.dao.AiChallengeDao
import com.howsleep.app.data.db.dao.ChallengeDayLogDao
import com.howsleep.app.data.db.dao.PostSleepLogDao
import com.howsleep.app.data.db.dao.PreSleepLogDao
import com.howsleep.app.data.db.dao.SleepSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HowSleepDatabase =
        Room.databaseBuilder(context, HowSleepDatabase::class.java, "howsleep.db")
            // MVP: migração destrutiva aceitável; substituir por migrações explícitas pré-beta
            .fallbackToDestructiveMigration()
            .build()

    // Cada DAO tem @Provides próprio (não @Singleton) — Room gerencia a instância via a database
    @Provides
    fun provideSleepSessionDao(db: HowSleepDatabase): SleepSessionDao = db.sleepSessionDao()

    @Provides
    fun providePreSleepLogDao(db: HowSleepDatabase): PreSleepLogDao = db.preSleepLogDao()

    @Provides
    fun providePostSleepLogDao(db: HowSleepDatabase): PostSleepLogDao = db.postSleepLogDao()

    @Provides
    fun provideAiChallengeDao(db: HowSleepDatabase): AiChallengeDao = db.aiChallengeDao()

    @Provides
    fun provideChallengeDayLogDao(db: HowSleepDatabase): ChallengeDayLogDao = db.challengeDayLogDao()
}
