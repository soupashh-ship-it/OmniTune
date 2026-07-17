/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.lyrics

import com.omnitune.app.constants.PreloadQueueLyricsEnabledKey
import com.omnitune.app.constants.QueueLyricsPreloadCountKey
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.LyricsEntity
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.utils.NetworkConnectivityObserver
import com.omnitune.app.utils.dataStore
import com.omnitune.app.utils.reportException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Manages pre-loading of lyrics for upcoming songs in the queue.
 * This improves user experience by having lyrics ready when songs change.
 */
class LyricsPreloadManager @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val database: MusicDatabase,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var preloadJob: Job? = null

    private var currentQueueIds: List<String> = emptyList()
    private var currentIndex: Int = -1

    /**
     * Called when the current song changes in the player.
     * Triggers pre-loading of lyrics for the next N songs in the queue.
     */
    fun onSongChanged(currentIndex: Int, queue: List<MediaMetadata>) {
        preloadJob?.cancel()

        scope.launch {
            try {
                val preferences = context.dataStore.data.first()
                val isEnabled = preferences[PreloadQueueLyricsEnabledKey] ?: true

                if (!isEnabled) {
                    Timber.tag(TAG).d("Queue lyrics pre-load is disabled")
                    return@launch
                }

                val isNetworkAvailable = try {
                    networkConnectivity.isCurrentlyConnected()
                } catch (e: Exception) {
                    true
                }

                if (!isNetworkAvailable) {
                    Timber.tag(TAG).w("Network unavailable, skipping lyrics pre-load")
                    return@launch
                }

                val preloadCount = preferences[QueueLyricsPreloadCountKey] ?: DEFAULT_PRELOAD_COUNT

                val nextSongs = getNextSongs(queue, currentIndex, preloadCount)

                if (nextSongs.isEmpty()) {
                    Timber.tag(TAG).d("No songs to pre-load")
                    return@launch
                }

                Timber.tag(TAG).d("Starting pre-load for ${nextSongs.size} songs")
                preloadLyrics(nextSongs)

            } catch (e: Exception) {
                reportException(e)
            }
        }
    }

    private fun getNextSongs(queue: List<MediaMetadata>, currentIndex: Int, count: Int): List<MediaMetadata> {
        if (queue.isEmpty() || currentIndex < 0) return emptyList()

        val startIndex = currentIndex + 1
        val endIndex = minOf(startIndex + count, queue.size)

        if (startIndex >= queue.size) return emptyList()

        return queue.subList(startIndex, endIndex)
    }

    private fun preloadLyrics(songs: List<MediaMetadata>) {
        preloadJob = scope.launch {
            try {
                songs.forEach { song ->
                    val existingLyrics = database.lyrics(song.id).first()
                    if (existingLyrics != null && existingLyrics.lyrics != LyricsEntity.LYRICS_NOT_FOUND) {
                        Timber.tag(TAG).d("Lyrics already cached for: ${song.title}")
                        return@forEach
                    }

                    try {
                        val lyrics = fetchLyricsForSong(song)
                        if (lyrics != null && lyrics != LyricsEntity.LYRICS_NOT_FOUND) {
                            database.query {
                                upsert(
                                    LyricsEntity(
                                        id = song.id,
                                        lyrics = lyrics,
                                    )
                                )
                            }
                            Timber.tag(TAG).d("Pre-loaded lyrics for: ${song.title}")
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).w(e, "Failed to pre-load lyrics for ${song.title}")
                    }
                }
            } catch (e: Exception) {
                reportException(e)
            }
        }
    }

    private suspend fun fetchLyricsForSong(song: MediaMetadata): String? {
        val lyricsHelper = LyricsHelper(context, networkConnectivity)

        return try {
            lyricsHelper.getLyrics(song, preferredProviderOnly = true)
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Error fetching lyrics for ${song.title}")
            null
        }
    }

    fun cancel() {
        preloadJob?.cancel()
        preloadJob = null
    }

    fun destroy() {
        cancel()
        scope.cancel()
    }

    companion object {
        private const val TAG = "LyricsPreloadManager"
        private const val DEFAULT_PRELOAD_COUNT = 3
    }
}
