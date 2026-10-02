package com.omnitune.shared.data.lyrics

import com.omnitune.shared.data.network.createPlatformHttpClient
import com.omnitune.shared.domain.models.Lyrics
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

@Serializable
data class LrcLibResponse(
    val id: Long? = null,
    val name: String? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
)

class LrcLibClient(
    private val httpClient: HttpClient = createPlatformHttpClient(),
) {
    private val baseUrl = "https://lrclib.net/api"

    suspend fun getLyrics(
        videoId: String,
        title: String,
        artist: String,
        album: String? = null,
        durationSec: Int = 0,
    ): Result<Lyrics> = runCatching {
        val response = httpClient.get("$baseUrl/get") {
            parameter("track_name", title)
            parameter("artist_name", artist)
            if (!album.isNullOrBlank()) parameter("album_name", album)
            if (durationSec > 0) parameter("duration", durationSec)
        }.body<LrcLibResponse>()

        val synced = response.syncedLyrics
        val plain = response.plainLyrics

        if (synced.isNullOrBlank() && plain.isNullOrBlank()) {
            throw IllegalStateException("No lyrics found on LRCLIB")
        }

        val parsed = LrcParser.parse(synced ?: plain)
        Lyrics(
            videoId = videoId,
            plainText = plain ?: synced,
            syncedLyrics = parsed.lines,
            source = "LRCLIB"
        )
    }
}
