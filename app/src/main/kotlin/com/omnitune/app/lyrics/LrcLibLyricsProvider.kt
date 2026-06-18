/*
 * OmniTune - based on Velune
 * Nikhil / Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0
 */

package com.omnitune.app.lyrics

import android.content.Context
import com.omnitune.app.constants.EnableLrcLibKey
import com.omnitune.app.utils.dataStore
import com.omnitune.app.utils.get
import com.omnitune.lrclib.LrcLib

object LrcLibLyricsProvider : LyricsProvider {
    override val name = "LrcLib"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableLrcLibKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> = LrcLib.getLyrics(title, artist, duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        LrcLib.getAllLyrics(title, artist, duration, null, callback)
    }
}
