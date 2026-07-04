/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.content.Context
import com.omnitune.app.constants.EnableLastFMScrobblingKey
import com.omnitune.app.constants.LastFMSessionKey
import com.omnitune.app.constants.LastFMUseNowPlaying
import com.omnitune.app.utils.dataStore
import com.omnitune.lastfm.LastFM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import timber.log.Timber

class ScrobblingManager(
    private val context: Context,
    private val scope: CoroutineScope,
) {

    private suspend fun isEnabled(): Boolean {
        val prefs = context.dataStore.data.first()
        val enabled = prefs[EnableLastFMScrobblingKey] ?: false
        val hasSession = prefs[LastFMSessionKey]?.isNotBlank() ?: false
        return enabled && hasSession
    }

    /**
     * Called when a new track starts playing. Sends "now playing" update to Last.fm
     * if the user has enabled scrobbling and the now-playing toggle.
     */
    fun onTrackChanged(title: String, artist: String, album: String?) {
        scope.launch(Dispatchers.IO) {
            try {
                if (!isEnabled()) return@launch
                val useNowPlaying = context.dataStore.data.first()[LastFMUseNowPlaying] ?: true
                if (!useNowPlaying) return@launch

                LastFM.updateNowPlaying(artist = artist, track = title, album = album)
                Timber.tag("Scrobbling").d("Now playing updated: %s - %s", artist, title)
            } catch (e: Exception) {
                Timber.tag("Scrobbling").e(e, "Failed to update now playing: %s - %s", artist, title)
            }
        }
    }

    /**
     * Called when the user has listened past the scrobble threshold. Submits
     * a scrobble to Last.fm.
     */
    fun onScrobbleThreshold(title: String, artist: String, album: String?, durationMs: Long) {
        scope.launch(Dispatchers.IO) {
            try {
                if (!isEnabled()) return@launch

                val timestamp = System.currentTimeMillis() / 1000L
                LastFM.scrobble(artist = artist, track = title, timestamp = timestamp, album = album)
                Timber.tag("Scrobbling").d("Scrobbled: %s - %s", artist, title)
            } catch (e: Exception) {
                Timber.tag("Scrobbling").e(e, "Failed to scrobble: %s - %s", artist, title)
            }
        }
    }
}
