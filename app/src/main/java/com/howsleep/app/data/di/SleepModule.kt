package com.howsleep.app.data.di

import android.content.Context
import com.howsleep.app.BuildConfig
import com.howsleep.app.sleep.MockSleepEventSource
import com.howsleep.app.sleep.RealSleepEventSource
import com.howsleep.app.sleep.SleepEventSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SleepModule {

    @Provides
    @Singleton
    fun provideSleepEventSource(@ApplicationContext context: Context): SleepEventSource =
        // R-10: BuildConfig.DEBUG deve SEMPRE estar presente para impedir mock em release
        // Fase 2: adicionar leitura do toggle DataStore para permitir alternar em runtime
        if (BuildConfig.DEBUG) MockSleepEventSource() else RealSleepEventSource(context)
}
