/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.menu

import android.content.res.Resources
import com.omnitune.app.R

internal fun playlistAddMessage(
    resources: Resources,
    songCount: Int = 1,
    playlistNames: List<String>,
): String = when {
    songCount == 1 && playlistNames.size == 1 ->
        resources.getString(R.string.added_to_playlist, playlistNames.first())
    songCount > 1 && playlistNames.size == 1 ->
        resources.getString(R.string.added_n_songs_to_playlist, songCount, playlistNames.first())
    songCount == 1 ->
        resources.getString(R.string.added_to_n_playlists, playlistNames.size)
    else ->
        resources.getString(R.string.added_n_songs_to_n_playlists, songCount, playlistNames.size)
}
