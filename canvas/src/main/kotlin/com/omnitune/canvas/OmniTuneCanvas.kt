package com.omnitune.canvas

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class CanvasArtwork(
    val song: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val videoId: String? = null,
    val animated: String? = null,
    val video: String? = null,
    val image: String? = null,
    val fanArt: String? = null,
    val thumbnail: String? = null
) {
    val preferredAnimationUrl: String?
        get() = animated ?: video
}

object OmniTuneCanvas {

    private const val BASE_URL = "https://artwork-archivetune.koiiverse.cloud"

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 18_000
            connectTimeoutMillis = 12_000
        }
    }

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    private data class CacheEntry(
        val data: CanvasArtwork?,
        val timestamp: Long
    ) {
        val isExpired: Boolean get() = System.currentTimeMillis() - timestamp > 60_000L
    }

    suspend fun getBySongAndArtist(song: String, artist: String): CanvasArtwork? {
        val cacheKey = "song:$song:$artist"
        return getCached(cacheKey) ?: fetchAndCache(cacheKey) {
            val response = client.get("$BASE_URL/v1/artwork") {
                parameter("song", song)
                parameter("artist", artist)
            }
            if (response.status == HttpStatusCode.OK) response.body() else null
        }
    }

    suspend fun getByAlbumId(albumId: String): CanvasArtwork? {
        val cacheKey = "album:$albumId"
        return getCached(cacheKey) ?: fetchAndCache(cacheKey) {
            val response = client.get("$BASE_URL/v1/artwork/album") {
                parameter("albumId", albumId)
            }
            if (response.status == HttpStatusCode.OK) response.body() else null
        }
    }

    suspend fun fetchByUrl(url: String): CanvasArtwork? {
        val response = client.get(url)
        return if (response.status == HttpStatusCode.OK) response.body() else null
    }

    private fun getCached(key: String): CanvasArtwork? {
        val entry = cache[key] ?: return null
        return if (!entry.isExpired) entry.data else {
            cache.remove(key)
            null
        }
    }

    private suspend fun fetchAndCache(key: String, fetcher: suspend () -> CanvasArtwork?): CanvasArtwork? {
        val data = fetcher()
        cache[key] = CacheEntry(data, System.currentTimeMillis())
        return data
    }
}
