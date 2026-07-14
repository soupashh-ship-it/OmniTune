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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import com.omnitune.app.utils.dataStore
import com.omnitune.app.constants.SkipSilenceKey
import com.omnitune.app.constants.AudioOffload
import com.omnitune.app.constants.AudioCrossfadeDurationKey
import com.omnitune.app.constants.SeekExtraSeconds
import com.omnitune.app.extensions.setOffloadEnabled
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

        val prefs = runBlocking(Dispatchers.IO) { context.dataStore.data.first() }
        val skipSilence = prefs[SkipSilenceKey] ?: false
        val audioOffload = prefs[AudioOffload] ?: false
        val crossfadeDuration = prefs[AudioCrossfadeDurationKey] ?: 0
        val progressiveSeek = prefs[SeekExtraSeconds] ?: false

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
            .setSeekBackIncrementMs(if (progressiveSeek) 10_000L else 5_000L)
            .setSeekForwardIncrementMs(if (progressiveSeek) 15_000L else 10_000L)
            .build()
            .apply {
                skipSilenceEnabled = skipSilence
                setOffloadEnabled(audioOffload && crossfadeDuration == 0 && !skipSilence)
            }
    }

    fun createCacheDataSourceFactory(
        context: Context,
        okHttpClient: OkHttpClient,
        downloadUtil: DownloadUtil
    ): CacheDataSource.Factory {
        val playbackHttpClient = okHttpClient.newBuilder()
            .addInterceptor(youtubeStreamHeaderInterceptor())
            .build()
        val dataSourceFactory = DefaultDataSource.Factory(
            context,
            OkHttpDataSource.Factory(playbackHttpClient).setUserAgent(DEFAULT_PLAYBACK_USER_AGENT)
        )
        return CacheDataSource.Factory()
            .setCache(downloadUtil.playbackCache)
            .setUpstreamDataSourceFactory(dataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
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
