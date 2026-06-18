/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 * Licensed Under GPL-3.0
 */

package com.omnitune.app.di

import com.omnitune.app.data.LyricsRepository
import com.omnitune.app.data.MusicRepository
import com.omnitune.app.data.MusicRepositoryImpl
import com.omnitune.app.data.StreamRepository
import com.omnitune.app.data.StreamRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindStreamRepository(
        impl: StreamRepositoryImpl
    ): StreamRepository

    @Binds
    @Singleton
    abstract fun bindMusicRepository(
        impl: MusicRepositoryImpl
    ): MusicRepository
}
