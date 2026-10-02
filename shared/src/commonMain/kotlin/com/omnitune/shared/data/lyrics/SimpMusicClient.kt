package com.omnitune.shared.data.lyrics

import com.omnitune.shared.data.network.createPlatformHttpClient
import com.omnitune.shared.domain.models.Lyrics
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class SimpMusicLyricsResponse(
    val type: String? = null,
    val data: List<SimpMusicLyricsData> = emptyList(),
)

@Serializable
data class SimpMusicLyricsData(
    val id: String? = null,
    val videoId: String? = null,
    val songTitle: String? = null,
    val artistName: String? = null,
    val durationSeconds: Int? = null,
    val syncedLyrics: String? = null,
    val plainLyric: String? = null,
)

class SimpMusicClient(
    private val httpClient: HttpClient = createPlatformHttpClient(),
) {
    private val baseUrl = "https://api-lyrics.simpmusic.org/v1"

    suspend fun getLyrics(
        videoId: String,
        durationSec: Int = 0,
    ): Result<Lyrics> = runCatching {
        val response = httpClient.get("$baseUrl/$videoId") {
            header("Accept", "application/json")
            header("User-Agent", "SimpMusicLyrics/1.0")
        }.body<SimpMusicLyricsResponse>()

        val tracks = response.data
        if (tracks.isEmpty()) {
            throw IllegalStateException("No lyrics found on SimpMusic")
        }

        val bestMatch = if (durationSec > 0 && tracks.size > 1) {
            tracks.minByOrNull { abs((it.durationSeconds ?: 0) - durationSec) }
        } else {
            tracks.firstOrNull()
        } ?: throw IllegalStateException("No valid lyric item")

        val rawLyrics = bestMatch.syncedLyrics ?: bestMatch.plainLyric
            ?: throw IllegalStateException("Empty lyric payload")

        val parsed = LrcParser.parse(rawLyrics)
        Lyrics(
            videoId = videoId,
            plainText = bestMatch.plainLyric ?: rawLyrics,
            syncedLyrics = parsed.lines,
            source = "SimpMusic"
        )
    }
}
