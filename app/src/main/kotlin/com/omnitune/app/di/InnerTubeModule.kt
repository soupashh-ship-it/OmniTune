/*
 * OmniTune - based on Velune
 * Nikhil / Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0
 */

package com.omnitune.app.di

import com.omnitune.app.innertube.InnerTube
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
