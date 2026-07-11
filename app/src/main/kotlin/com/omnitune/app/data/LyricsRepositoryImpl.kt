package com.omnitune.app.data

import com.omnitune.app.db.DatabaseDao
import com.omnitune.app.db.entities.LyricsEntity
import com.omnitune.app.lyrics.InlineLyrics
import com.omnitune.app.lyrics.LyricsHelper
import com.omnitune.app.models.AppResult
import com.omnitune.app.models.LyricsLine
import com.omnitune.app.models.MediaMetadata
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class LyricsRepositoryImpl @Inject constructor(
    private val lyricsHelper: LyricsHelper,
    private val databaseDao: DatabaseDao
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
            val dbLyrics = databaseDao.lyrics(songId).firstOrNull()?.lyrics
            if (!dbLyrics.isNullOrBlank() && dbLyrics != LyricsEntity.LYRICS_NOT_FOUND) {
                return AppResult.Success(parseLrc(dbLyrics))
            }
            val fetched = lyricsHelper.getLyrics(metadata)
            val lrcText = if (fetched != LyricsEntity.LYRICS_NOT_FOUND) {
                databaseDao.upsert(LyricsEntity(id = songId, lyrics = fetched))
                fetched
            } else {
                dbLyrics ?: LyricsEntity.LYRICS_NOT_FOUND
            }
            
            if (lrcText == LyricsEntity.LYRICS_NOT_FOUND) {
                AppResult.Error("Lyrics not found")
            } else {
                AppResult.Success(parseLrc(lrcText))
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Failed to load lyrics", e)
        }
    }

    override fun parseLrc(lrcText: String): List<LyricsLine> {
        val entries = InlineLyrics.parseSyncedEntries(lrcText)
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
