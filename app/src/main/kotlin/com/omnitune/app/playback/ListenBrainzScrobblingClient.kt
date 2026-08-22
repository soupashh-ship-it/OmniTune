/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

/** Sends user-authorized ListenBrainz events without logging the token or request body. */
internal object ListenBrainzScrobblingClient {
    private const val endpoint = "https://api.listenbrainz.org/1/submit-listens"
    private val httpClient = OkHttpClient()

    fun submitPlayingNow(token: String, title: String, artist: String, album: String?) {
        submit(token, "playing_now", title, artist, album, listenedAtSeconds = null)
    }

    fun submitSingle(token: String, title: String, artist: String, album: String?, listenedAtSeconds: Long) {
        submit(token, "single", title, artist, album, listenedAtSeconds)
    }

    private fun submit(
        token: String,
        type: String,
        title: String,
        artist: String,
        album: String?,
        listenedAtSeconds: Long?,
    ) {
        if (token.isBlank() || title.isBlank() || artist.isBlank()) return
        val metadata = JSONObject()
            .put("artist_name", artist)
            .put("track_name", title)
            .put("additional_info", JSONObject().put("submission_client", "OmniTune"))
        album?.takeIf { it.isNotBlank() }?.let { metadata.put("release_name", it) }
        val listen = JSONObject().put("track_metadata", metadata)
        listenedAtSeconds?.let { listen.put("listened_at", it) }
        val body = JSONObject()
            .put("listen_type", type)
            .put("payload", JSONArray().put(listen))
            .toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .post(body)
            .header("Authorization", "Token $token")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.tag("ListenBrainz").w("%s submission failed with HTTP %s", type, response.code)
                }
            }
        } catch (error: Exception) {
            Timber.tag("ListenBrainz").w(error, "%s submission failed", type)
        }
    }
}
