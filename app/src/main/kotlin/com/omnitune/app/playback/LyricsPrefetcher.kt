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
import kotlinx.coroutines.launch
import timber.log.Timber

class LyricsPrefetcher(
    private val database: MusicDatabase,
    private val lyricsHelper: LyricsHelper,
    private val scope: CoroutineScope,
) {
    fun prefetch(metadata: MediaMetadata?) {
        if (metadata == null) return

        scope.launch(Dispatchers.IO) {
            try {
                val lyrics = lyricsHelper.getLyrics(metadata)
                if (lyrics != LyricsEntity.LYRICS_NOT_FOUND && lyrics.isNotBlank()) {
                    database.upsert(LyricsEntity(id = metadata.id, lyrics = lyrics))
                    Timber.tag("MusicService").d("Fetched and cached lyrics for: ${metadata.title}")
                }
            } catch (e: Exception) {
                Timber.tag("MusicService").w(e, "Failed to fetch lyrics for: ${metadata.title}")
                reportException(e)
            }
        }
    }
}
