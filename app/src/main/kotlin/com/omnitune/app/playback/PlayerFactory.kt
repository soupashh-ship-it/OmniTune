package com.omnitune.app.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.offline.DownloadManager
import okhttp3.OkHttpClient
import androidx.media3.datasource.okhttp.OkHttpDataSource

object PlayerFactory {

    fun createPlayer(
        context: Context,
        okHttpClient: OkHttpClient,
        downloadUtil: DownloadUtil,
    ): ExoPlayer {
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().apply {
                setMaxVideoSizeSd()
                setPreferredAudioLanguages("en")
            })
        }

        val cacheDataSourceFactory = createCacheDataSourceFactory(context, okHttpClient, downloadUtil)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15000, // minBufferMs
                50000, // maxBufferMs
                1000,  // bufferForPlaybackMs
                1500   // bufferForPlaybackAfterRebufferMs
            )
            .build()

        return ExoPlayer.Builder(context)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(cacheDataSourceFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(5_000L)
            .setSeekForwardIncrementMs(10_000L)
            .build()
    }

    fun createCacheDataSourceFactory(
        context: Context,
        okHttpClient: OkHttpClient,
        downloadUtil: DownloadUtil
    ): CacheDataSource.Factory {
        val dataSourceFactory = DefaultDataSource.Factory(
            context, 
            OkHttpDataSource.Factory(okHttpClient).setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36 OmniTune")
        )
        return CacheDataSource.Factory()
            .setCache(downloadUtil.downloadCache)
            .setUpstreamDataSourceFactory(dataSourceFactory)
            .setCacheWriteDataSinkFactory(null)
    }

    fun createOverlapPlayer(
        context: Context,
        okHttpClient: OkHttpClient,
        downloadUtil: DownloadUtil
    ): ExoPlayer {
        val cacheDataSourceFactory = createCacheDataSourceFactory(context, okHttpClient, downloadUtil)
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(cacheDataSourceFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
}
