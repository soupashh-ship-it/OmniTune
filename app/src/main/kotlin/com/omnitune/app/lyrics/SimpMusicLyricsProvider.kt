/*
 * OmniTune - based on Velune
 * Nikhil / Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0
 */

package com.omnitune.app.lyrics

import android.content.Context
import com.omnitune.app.constants.EnableSimpMusicLyricsKey
import com.omnitune.simpmusic.SimpMusicLyrics
import com.omnitune.app.utils.dataStore
import com.omnitune.app.utils.get

object SimpMusicLyricsProvider : LyricsProvider {
    override val name: String = "SimpMusic"

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableSimpMusicLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> = SimpMusicLyrics.getLyrics(videoId = id, duration = duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        SimpMusicLyrics.getAllLyrics(videoId = id, duration = duration, callback = callback)
    }
}
