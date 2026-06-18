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
        return try {
            val result = streamExtractor.extract(songId, StreamQuality.HIGH)
            if (result != null) {
                AppResult.Success(mapResult(result))
            } else {
                AppResult.Error("No stream found")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Extraction failed", e)
        }
    }

    override suspend fun extractWithFallbacks(songId: String): AppResult<StreamInfo> {
        return try {
            val result = streamExtractor.extract(songId, StreamQuality.HIGH)
            if (result != null) {
                AppResult.Success(mapResult(result))
            } else {
                AppResult.Error("All extraction tiers exhausted")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Extraction failed", e)
        }
    }

    override suspend fun probeStream(url: String): Boolean = true

    override fun getCachedStream(songId: String): StreamInfo? = null

    override fun clearCache() = streamCache.clear()

    private fun mapResult(result: StreamResult): StreamInfo = StreamInfo(
        url = result.url,
        contentType = result.contentType,
        contentLength = result.contentLength,
        bitrate = result.bitrate,
        cacheHit = result.cacheHit
    )
}
