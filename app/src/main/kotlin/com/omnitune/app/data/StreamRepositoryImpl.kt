package com.omnitune.app.data

import com.omnitune.app.models.AppResult
import com.omnitune.app.models.StreamInfo
import com.omnitune.app.models.StreamQuality
import com.omnitune.app.models.StreamResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamRepositoryImpl @Inject constructor(
    private val streamExtractor: StreamExtractor,
    private val streamCache: StreamCache
) : StreamRepository {

    override suspend fun extractStream(songId: String): AppResult<StreamInfo> {
        val cached = streamCache.get(songId)
        if (cached != null) {
            return AppResult.Success(cached)
        }
        return try {
            val result = streamExtractor.extract(songId, StreamQuality.HIGH)
            if (result != null) {
                val mapped = mapResult(result)
                streamCache.put(songId, mapped)
                AppResult.Success(mapped)
            } else {
                AppResult.Error("No stream found")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Extraction failed", e)
        }
    }

    override suspend fun extractWithFallbacks(songId: String): AppResult<StreamInfo> {
        val cached = streamCache.get(songId)
        if (cached != null) {
            return AppResult.Success(cached)
        }
        return try {
            val result = streamExtractor.extract(songId, StreamQuality.HIGH)
            if (result != null) {
                val mapped = mapResult(result)
                streamCache.put(songId, mapped)
                AppResult.Success(mapped)
            } else {
                AppResult.Error("All extraction tiers exhausted")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Extraction failed", e)
        }
    }

    override suspend fun probeStream(url: String): Boolean = true

    override fun getCachedStream(songId: String): StreamInfo? = streamCache.get(songId)

    override fun clearCache() = streamCache.clear()

    private fun mapResult(result: StreamResult): StreamInfo = StreamInfo(
        url = result.url,
        contentType = result.contentType,
        contentLength = result.contentLength,
        bitrate = result.bitrate,
        cacheHit = result.cacheHit
    )
}
