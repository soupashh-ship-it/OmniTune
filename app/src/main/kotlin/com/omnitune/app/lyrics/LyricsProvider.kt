/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.lyrics

import android.content.Context

interface LyricsProvider {
    val name: String

    /**
     * True only when a provider retrieves lyrics from the exact media/video ID.
     * Search-only sources can return another song with the same title, so they
     * are accepted only after strict metadata evidence is present.
     */
    val isTrackBound: Boolean
        get() = false

    /**
     * True when the provider resolves a result using title, artist, and duration
     * together. This is weaker than an exact media ID, but strong enough to
     * prefer a synchronized match over exact-video lyrics that have no timing.
     */
    val isMetadataBound: Boolean
        get() = false

    fun isEnabled(context: Context): Boolean

    suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String>

    suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        getLyrics(id, title, artist, album, duration).onSuccess(callback)
    }
}
