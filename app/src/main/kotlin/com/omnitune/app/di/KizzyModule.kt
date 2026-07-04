/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.di

import com.omnitune.kizzy.DefaultKizzyLogger
import com.omnitune.kizzy.KizzyLogger
import com.omnitune.kizzy.remote.ApiService
import com.omnitune.kizzy.repository.KizzyRepository
import com.omnitune.kizzy.rpc.KizzyRPC
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object KizzyModule {

    @Provides
    @Singleton
    fun provideKizzyLogger(): KizzyLogger = DefaultKizzyLogger()

    @Provides
    @Singleton
    fun provideApiService(client: HttpClient): ApiService = ApiService(client)

    @Provides
    @Singleton
    fun provideKizzyRepository(apiService: ApiService): KizzyRepository = KizzyRepository(apiService)

    @Provides
    @Singleton
    fun provideKizzyRPC(
        repository: KizzyRepository,
        apiService: ApiService,
        logger: KizzyLogger,
    ): KizzyRPC = KizzyRPC(repository, apiService, logger)
}
