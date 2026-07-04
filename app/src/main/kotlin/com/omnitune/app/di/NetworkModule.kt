/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.di

import android.content.Context
import com.omnitune.app.utils.NetworkConnectivityObserver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Volatile
    private var httpClient: HttpClient? = null

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient().also { httpClient = it }
    }

    @Provides
    @Singleton
    fun provideNetworkConnectivityObserver(
        @ApplicationContext context: Context,
    ): NetworkConnectivityObserver = NetworkConnectivityObserver(context)

    /**
     * Closes the created [HttpClient] to release connections and thread pools.
     * Safe to call multiple times; subsequent calls are no-ops.
     */
    fun shutdown() {
        httpClient?.close()
        httpClient = null
    }
}
