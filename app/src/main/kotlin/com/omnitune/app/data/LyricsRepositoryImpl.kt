package com.omnitune.app.data

import com.omnitune.app.db.DatabaseDao
import com.omnitune.app.db.entities.LyricsEntity
import com.omnitune.app.lyrics.InlineLyrics
import com.omnitune.app.lyrics.LyricsHelper
import com.omnitune.app.models.AppResult
import com.omnitune.app.models.LyricsLine
import com.omnitune.app.models.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
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
    ): AppResult<List<LyricsLine>> = withContext(Dispatchers.IO) {
        try {
            val metadata = MediaMetadata(
                id = songId,
                title = title,
                artists = listOf(MediaMetadata.Artist(id = "", name = artist)),
                duration = duration.toInt()
            )
            val fetched = lyricsHelper.getLyrics(metadata)
            if (fetched != LyricsEntity.LYRICS_NOT_FOUND && fetched.isNotBlank()) {
                databaseDao.upsert(LyricsEntity(id = songId, lyrics = fetched))
                return@withContext AppResult.Success(parseLrc(fetched))
            }

            // Database rows written before track identity validation may belong
            // to a different song with the same title. Preserve the row for
            // recovery, but never display unverified cached lyrics.
            val dbLyrics = databaseDao.lyrics(songId).firstOrNull()?.lyrics
            if (dbLyrics == LyricsEntity.LYRICS_NOT_FOUND || dbLyrics.isNullOrBlank()) {
                AppResult.Error("Lyrics not found")
            } else {
                AppResult.Error("Verified lyrics are not available for this track")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Failed to load lyrics", e)
        }
    }

    override fun parseLrc(lrcText: String): List<LyricsLine> {
        val entries = InlineLyrics.parseSyncedEntries(lrcText)
        if (entries.isNotEmpty()) {
            return entries.map { entry ->
                LyricsLine(
                    timestamp = entry.time,
                    text = entry.text,
                    isTranslated = false
                )
            }
        }

        return lrcText
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && it != LyricsEntity.LYRICS_NOT_FOUND }
            .map { line ->
                LyricsLine(
                    timestamp = -1L,
                    text = line,
                    isTranslated = false
                )
            }
            .toList()
    }
}
