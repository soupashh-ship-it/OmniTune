/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.di

import coil3.ImageLoader
import coil3.request.crossfade
import com.omnitune.app.db.InternalDatabase
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.utils.dataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMusicDatabase(
        @ApplicationContext context: android.content.Context,
    ): MusicDatabase = InternalDatabase.newInstance(context)

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: android.content.Context,
    ): DataStore<Preferences> = context.dataStore

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: android.content.Context,
    ): ImageLoader = ImageLoader.Builder(context)
        .crossfade(300)
        .build()

    @Provides
    @Singleton
    @androidx.media3.common.util.UnstableApi
    fun provideDownloadUtil(
        @ApplicationContext context: android.content.Context,
        okHttpClient: okhttp3.OkHttpClient
    ): com.omnitune.app.playback.DownloadUtil {
        return com.omnitune.app.playback.DownloadUtil(context, okHttpClient)
    }

    @Provides
    @Singleton
    fun provideDatabaseDao(
        database: MusicDatabase
    ): com.omnitune.app.db.DatabaseDao {
        return database as com.omnitune.app.db.DatabaseDao
    }
}
