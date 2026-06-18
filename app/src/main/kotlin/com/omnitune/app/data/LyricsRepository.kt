package com.omnitune.app.data

import com.omnitune.app.models.AppResult
import com.omnitune.app.models.LyricsLine

interface LyricsRepository {
    suspend fun loadLyrics(
        songId: String,
        title: String,
        artist: String,
        duration: Long
    ): AppResult<List<LyricsLine>>

    fun parseLrc(lrcText: String): List<LyricsLine>
}
