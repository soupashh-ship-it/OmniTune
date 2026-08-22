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
import androidx.media3.datasource.DataSource
import okhttp3.OkHttpClient
import androidx.media3.datasource.okhttp.OkHttpDataSource

import com.omnitune.app.utils.StreamClientUtils
import okhttp3.Interceptor

object PlayerFactory {

    private const val DEFAULT_PLAYBACK_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36 OmniTune"

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
                250,   // bufferForPlaybackMs
                1000   // bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // Preference-derived settings (skip silence, offload, seek increments) are applied
        // reactively by PlaybackPreferenceObserver; reading DataStore here would block the
        // main thread during service start.
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
    ): DataSource.Factory {
        val playbackHttpClient = okHttpClient.newBuilder()
            .addInterceptor(youtubeStreamHeaderInterceptor())
            .build()
        val networkDataSourceFactory = DefaultDataSource.Factory(
            context,
            OkHttpDataSource.Factory(playbackHttpClient).setUserAgent(DEFAULT_PLAYBACK_USER_AGENT)
        )

        val streamCacheSourceFactory = CacheDataSource.Factory()
            .setCache(downloadUtil.playbackCache)
            .setUpstreamDataSourceFactory(networkDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val completedDownloadSourceFactory = CacheDataSource.Factory()
            .setCache(downloadUtil.downloadCache)
            .setUpstreamDataSourceFactory(null)
            .setCacheWriteDataSinkFactory(null)
            .setFlags(CacheDataSource.FLAG_BLOCK_ON_CACHE)

        return OfflineCacheRoutingDataSourceFactory(
            completedDownloadSourceFactory = completedDownloadSourceFactory,
            streamSourceFactory = streamCacheSourceFactory,
            isCompletedDownloadCacheKey = downloadUtil::isPlayableCacheKey,
        )
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
                false
            )
            .setHandleAudioBecomingNoisy(false)
            .build()
    }

    private fun youtubeStreamHeaderInterceptor(): Interceptor =
        Interceptor { chain ->
            val request = chain.request()
            val clientParam = request.url.queryParameter("c")?.trim().orEmpty()

            if (!request.url.host.endsWith("googlevideo.com") || clientParam.isBlank()) {
                return@Interceptor chain.proceed(request)
            }

            val originReferer = StreamClientUtils.resolveOriginReferer(clientParam)
            val builder = request.newBuilder()
                .header("User-Agent", StreamClientUtils.resolveUserAgent(clientParam))
            originReferer.origin?.let { builder.header("Origin", it) }
            originReferer.referer?.let { builder.header("Referer", it) }

            chain.proceed(builder.build())
        }
}
