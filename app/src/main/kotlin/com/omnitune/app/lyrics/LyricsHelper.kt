/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.lyrics

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.omnitune.app.constants.PreferredLyricsProvider
import com.omnitune.app.constants.PreferredLyricsProviderKey
import com.omnitune.app.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.omnitune.app.extensions.toEnum
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.utils.GlobalLog
import com.omnitune.app.utils.NetworkConnectivityObserver
import com.omnitune.app.utils.dataStore
import com.omnitune.app.utils.reportException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.selects.select
import javax.inject.Inject

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    private val baseProviders =
        listOf(
            YouTubeLyricsProvider,
            SimpMusicLyricsProvider,
            BetterLyricsProvider,
            LrcLibLyricsProvider,
            KuGouLyricsProvider,
            YouTubeSubtitleLyricsProvider,
        )

    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    private var currentLyricsJob: Job? = null
    private val helperScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun getLyrics(mediaMetadata: MediaMetadata, preferredProviderOnly: Boolean = false): String {
        val cached = cache.get(mediaMetadata.id)?.firstOrNull()
        if (cached != null) {
            GlobalLog.append(Log.DEBUG, "LyricsHelper", "Found lyrics in cache for ${mediaMetadata.title}")
            return cached.lyrics
        }

        GlobalLog.append(Log.DEBUG, "LyricsHelper", "Fetching lyrics for ${mediaMetadata.title} (Artist: ${mediaMetadata.artists.joinToString { it.name }}, Album: ${mediaMetadata.album?.title})")

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }

        if (!isNetworkAvailable) {
            GlobalLog.append(Log.WARN, "LyricsHelper", "Network unavailable, aborting lyrics fetch")
            return LYRICS_NOT_FOUND
        }

        val ordered = orderedProviders()
        val providers = if (preferredProviderOnly) listOf(ordered.first()) else ordered
        val lyrics = helperScope.async {
            providerGroups(providers).forEach { group ->
                fetchFirstMeaningful(group, mediaMetadata)?.let { lyrics ->
                    cache.put(mediaMetadata.id, listOf(LyricsResult("cache", lyrics)))
                    return@async lyrics
                }
            }
            LYRICS_NOT_FOUND
        }.await()
        return lyrics
    }

    private suspend fun fetchFirstMeaningful(
        providers: List<LyricsProvider>,
        mediaMetadata: MediaMetadata,
    ): String? = coroutineScope {
        val jobs = providers.filter { it.isEnabled(context) }.map { provider ->
            async {
                try {
                    kotlinx.coroutines.withTimeoutOrNull(6000L) {
                        provider.getLyrics(
                            mediaMetadata.id,
                            mediaMetadata.title,
                            mediaMetadata.artists.joinToString { it.name },
                            mediaMetadata.album?.title,
                            mediaMetadata.duration,
                        ).fold(
                            onSuccess = { lyrics -> lyrics.takeIf(::isMeaningfulLyrics) },
                            onFailure = {
                                reportException(it)
                                null
                            },
                        )
                    }
                } catch (e: Exception) {
                    reportException(e)
                    null
                }
            }
        }

        val pending = jobs.toMutableList()
        try {
            while (pending.isNotEmpty()) {
                val (job, lyrics) = awaitNext(pending)
                pending.remove(job)
                if (lyrics != null) return@coroutineScope lyrics
            }
            null
        } finally {
            pending.forEach { it.cancel() }
        }
    }

    private suspend fun awaitNext(jobs: List<Deferred<String?>>): Pair<Deferred<String?>, String?> =
        select {
            jobs.forEach { job ->
                job.onAwait { lyrics -> job to lyrics }
            }
        }

    private fun providerGroups(providers: List<LyricsProvider>): List<List<LyricsProvider>> {
        if (providers.size <= 1) return listOf(providers)
        val trusted = listOf(YouTubeLyricsProvider, SimpMusicLyricsProvider).filter { it in providers }
        val subtitles = listOf(YouTubeSubtitleLyricsProvider).filter { it in providers }
        val searchBased = providers.filterNot { it in trusted || it in subtitles }
        return listOf(trusted, searchBased, subtitles).filter { it.isNotEmpty() }
    }

    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        songAlbum: String?,
        duration: Int,
        callback: (LyricsResult) -> Unit,
    ) {
        currentLyricsJob?.cancel()

        val cacheKey = "$songArtists-$songTitle".replace(" ", "")
        cache.get(cacheKey)?.let { results ->
            results.forEach {
                callback(it)
            }
            return
        }

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }

        if (!isNetworkAvailable) {
            return
        }

        val allResult = mutableListOf<LyricsResult>()
        val providers = orderedProviders()
        val job = helperScope.async {
            providers.forEach { provider ->
                if (provider.isEnabled(context)) {
                    try {
                        kotlinx.coroutines.withTimeoutOrNull(10000L) {
                            provider.getAllLyrics(mediaId, songTitle, songArtists, songAlbum, duration) lyricsCallback@{ lyrics ->
                                if (!isMeaningfulLyrics(lyrics)) return@lyricsCallback
                                val result = LyricsResult(provider.name, lyrics)
                                allResult += result
                                callback(result)
                            }
                        }
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }
            cache.put(cacheKey, allResult)
        }
        currentLyricsJob = job
        job.join()
    }

    private suspend fun orderedProviders(): List<LyricsProvider> {
        val preferred =
            context.dataStore.data
                .first()[PreferredLyricsProviderKey]
                .toEnum(PreferredLyricsProvider.LRCLIB)

        val first =
            when (preferred) {
                PreferredLyricsProvider.LRCLIB -> LrcLibLyricsProvider
                PreferredLyricsProvider.KUGOU -> KuGouLyricsProvider
                PreferredLyricsProvider.BETTER_LYRICS -> BetterLyricsProvider
                PreferredLyricsProvider.SIMPMUSIC -> SimpMusicLyricsProvider
            }

        return (listOf(first) + baseProviders).distinct()
    }

    private fun isMeaningfulLyrics(lyrics: String): Boolean {
        val normalized =
            lyrics
                .replace("\uFEFF", "")
                .replace(INVISIBLE_CHARS_REGEX, "")
                .trim { it.isWhitespace() || it == '\u00A0' }

        if (normalized.isEmpty()) return false
        if (normalized == LYRICS_NOT_FOUND) return false

        val remaining =
            TIMESTAMP_REGEX
                .replace(normalized, "")
                .replace(INVISIBLE_CHARS_REGEX, "")
                .trim { it.isWhitespace() || it == '\u00A0' }

        return remaining.any { !it.isWhitespace() && it != '\u00A0' }
    }

    fun cancelCurrentLyricsJob() {
        currentLyricsJob?.cancel()
        currentLyricsJob = null
    }

    companion object {
        private const val MAX_CACHE_SIZE = 3
        private val TIMESTAMP_REGEX = Regex("""\[[0-9]{1,2}:[0-9]{2}(?:\.[0-9]{1,3})?]""")
        private val INVISIBLE_CHARS_REGEX = Regex("""[\u200B\u200C\u200D\u2060\u00AD]""")
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)
