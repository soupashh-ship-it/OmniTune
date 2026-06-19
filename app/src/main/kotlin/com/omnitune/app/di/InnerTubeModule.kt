/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.di

import com.omnitune.innertube.InnerTube
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InnerTubeModule {

    @Provides
    @Singleton
    fun provideInnerTube(): InnerTube = InnerTube()
}
