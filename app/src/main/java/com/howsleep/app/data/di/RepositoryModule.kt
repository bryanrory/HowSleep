package com.howsleep.app.data.di

import com.howsleep.app.data.repository.AiChallengeRepository
import com.howsleep.app.data.repository.HabitLogRepository
import com.howsleep.app.data.repository.SleepRepository
import com.howsleep.app.data.repository.impl.AiChallengeRepositoryImpl
import com.howsleep.app.data.repository.impl.HabitLogRepositoryImpl
import com.howsleep.app.data.repository.impl.SleepRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSleepRepository(impl: SleepRepositoryImpl): SleepRepository

    @Binds
    @Singleton
    abstract fun bindHabitLogRepository(impl: HabitLogRepositoryImpl): HabitLogRepository

    @Binds
    @Singleton
    abstract fun bindAiChallengeRepository(impl: AiChallengeRepositoryImpl): AiChallengeRepository
}
