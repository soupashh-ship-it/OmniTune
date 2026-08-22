/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.content.Context
import com.omnitune.app.constants.ListenBrainzEnabledKey
import com.omnitune.app.constants.ListenBrainzNowPlayingKey
import com.omnitune.app.constants.ListenBrainzTokenKey
import com.omnitune.app.utils.SecurePreferenceCipher
import com.omnitune.app.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Owns user-authorized ListenBrainz scrobbling for the playback service. */
class ScrobblingManager(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private suspend fun activeToken(): String? {
        val preferences = context.dataStore.data.first()
        if (preferences[ListenBrainzEnabledKey] != true) return null
        return SecurePreferenceCipher.decryptOrPlain(preferences[ListenBrainzTokenKey])
            .takeIf { it.isNotBlank() }
    }

    fun onTrackChanged(title: String, artist: String, album: String?) {
        scope.launch(Dispatchers.IO) {
            val preferences = context.dataStore.data.first()
            if (preferences[ListenBrainzNowPlayingKey] != true) return@launch
            val token = activeToken() ?: return@launch
            ListenBrainzScrobblingClient.submitPlayingNow(token, title, artist, album)
        }
    }

    fun onScrobbleThreshold(title: String, artist: String, album: String?, durationMs: Long) {
        scope.launch(Dispatchers.IO) {
            val token = activeToken() ?: return@launch
            val startedAt = (System.currentTimeMillis() - durationMs.coerceAtLeast(0L)) / 1000L
            ListenBrainzScrobblingClient.submitSingle(token, title, artist, album, startedAt)
        }
    }
}
