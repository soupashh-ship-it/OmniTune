package com.omnitune.app.data

import com.omnitune.app.lyrics.LyricsHelper
import com.omnitune.app.lyrics.LyricsUtils
import com.omnitune.app.models.AppResult
import com.omnitune.app.models.LyricsLine
import com.omnitune.app.models.MediaMetadata
import javax.inject.Inject

class LyricsRepositoryImpl @Inject constructor(
    private val lyricsHelper: LyricsHelper
) : LyricsRepository {

    override suspend fun loadLyrics(
        songId: String,
        title: String,
        artist: String,
        duration: Long
    ): AppResult<List<LyricsLine>> {
        return try {
            val metadata = MediaMetadata(
                id = songId,
                title = title,
                artists = listOf(MediaMetadata.Artist(id = "", name = artist)),
                duration = duration.toInt()
            )
            val lrcText = lyricsHelper.getLyrics(metadata)
            if (lrcText == com.omnitune.app.db.entities.LyricsEntity.LYRICS_NOT_FOUND) {
                AppResult.Error("Lyrics not found")
            } else {
                AppResult.Success(parseLrc(lrcText))
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Failed to load lyrics", e)
        }
    }

    override fun parseLrc(lrcText: String): List<LyricsLine> {
        val entries = LyricsUtils.parseLyrics(lrcText)
        if (entries.isEmpty() && lrcText.isNotBlank()) {
            // Unsynced lyrics fallback
            return lrcText.lines().map { line ->
                LyricsLine(timestamp = -1L, text = line)
            }
        }
        return entries.map { entry ->
            LyricsLine(
                timestamp = entry.time,
                text = entry.text,
                isTranslated = false
            )
        }
    }
}
