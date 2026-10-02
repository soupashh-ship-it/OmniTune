package com.omnitune.shared.data.lyrics

import com.omnitune.shared.data.network.createPlatformHttpClient
import com.omnitune.shared.domain.models.Lyrics
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.abs

@Serializable
data class KuGouSearchSongResponse(
    val data: KuGouSongData? = null,
)

@Serializable
data class KuGouSongData(
    val info: List<KuGouSongInfo> = emptyList(),
)

@Serializable
data class KuGouSongInfo(
    val hash: String = "",
    val duration: Int = 0,
    val songname: String = "",
)

@Serializable
data class KuGouSearchLyricsResponse(
    val candidates: List<KuGouCandidate> = emptyList(),
)

@Serializable
data class KuGouCandidate(
    val id: Long = 0L,
    val accesskey: String = "",
    val duration: Int = 0,
)

@Serializable
data class KuGouDownloadLyricsResponse(
    val content: String = "",
    val fmt: String = "lrc",
)

@OptIn(ExperimentalEncodingApi::class)
class KuGouClient(
    private val httpClient: HttpClient = createPlatformHttpClient(),
) {
    suspend fun getLyrics(
        videoId: String,
        title: String,
        artist: String,
        durationSec: Int = 0,
    ): Result<Lyrics> = runCatching {
        val keyword = "$title $artist"
        val candidate = searchCandidate(keyword, durationSec)
            ?: throw IllegalStateException("No KuGou lyrics candidate found")

        val download = httpClient.get("https://lyrics.kugou.com/download") {
            parameter("ver", 1)
            parameter("client", "pc")
            parameter("id", candidate.id)
            parameter("accesskey", candidate.accesskey)
            parameter("fmt", "lrc")
            parameter("charset", "utf8")
        }.body<KuGouDownloadLyricsResponse>()

        if (download.content.isBlank()) {
            throw IllegalStateException("Empty KuGou lyrics content")
        }

        val decodedLrc = Base64.Default.decode(download.content).decodeToString()
        val parsed = LrcParser.parse(decodedLrc)

        Lyrics(
            videoId = videoId,
            plainText = decodedLrc,
            syncedLyrics = parsed.lines,
            source = "KuGou"
        )
    }

    private suspend fun searchCandidate(keyword: String, durationSec: Int): KuGouCandidate? {
        return try {
            val response = httpClient.get("https://lyrics.kugou.com/search") {
                parameter("ver", 1)
                parameter("man", "yes")
                parameter("client", "pc")
                parameter("keyword", keyword)
                if (durationSec > 0) {
                    parameter("duration", durationSec * 1000)
                }
            }.body<KuGouSearchLyricsResponse>()

            if (durationSec > 0) {
                response.candidates.minByOrNull { abs(it.duration / 1000 - durationSec) }
            } else {
                response.candidates.firstOrNull()
            }
        } catch (_: Exception) {
            null
        }
    }
}
