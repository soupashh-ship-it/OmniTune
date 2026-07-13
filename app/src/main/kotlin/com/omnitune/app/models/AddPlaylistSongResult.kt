/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.models

sealed interface AddPlaylistSongResult {
    data object Added : AddPlaylistSongResult
    data object Duplicate : AddPlaylistSongResult
    data class Failed(val message: String? = null) : AddPlaylistSongResult
}
