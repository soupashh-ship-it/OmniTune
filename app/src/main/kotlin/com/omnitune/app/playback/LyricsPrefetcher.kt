/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.LyricsEntity
import com.omnitune.app.lyrics.LyricsHelper
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.utils.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

class LyricsPrefetcher(
    private val database: MusicDatabase,
    private val lyricsHelper: LyricsHelper,
    private val scope: CoroutineScope,
) {
    private val activeIds = ConcurrentHashMap.newKeySet<String>()

    fun prefetch(metadata: MediaMetadata?) {
        if (metadata == null) return

        scope.launch(Dispatchers.IO) {
            fetchAndCache(metadata, preferredProviderOnly = false)
        }
    }

    fun prefetchUpcoming(queue: List<MediaMetadata>, currentIndex: Int, count: Int = 3) {
        if (currentIndex < 0 || currentIndex >= queue.lastIndex) return
        val upcoming = queue.drop(currentIndex + 1).take(count)
        scope.launch(Dispatchers.IO) {
            upcoming.forEach { fetchAndCache(it, preferredProviderOnly = true) }
        }
    }

    private suspend fun fetchAndCache(metadata: MediaMetadata, preferredProviderOnly: Boolean) {
        if (!activeIds.add(metadata.id)) return
        try {
            val cached = database.lyrics(metadata.id).first()?.lyrics
            if (!cached.isNullOrBlank() && cached != LyricsEntity.LYRICS_NOT_FOUND) return

            val lyrics = lyricsHelper.getLyrics(metadata, preferredProviderOnly)
            if (lyrics != LyricsEntity.LYRICS_NOT_FOUND && lyrics.isNotBlank()) {
                database.upsert(LyricsEntity(id = metadata.id, lyrics = lyrics))
                Timber.tag("MusicService").d("Fetched and cached lyrics for: ${metadata.title}")
            }
        } catch (e: Exception) {
            Timber.tag("MusicService").w(e, "Failed to fetch lyrics for: ${metadata.title}")
            reportException(e)
        } finally {
            activeIds.remove(metadata.id)
        }
    }
}
