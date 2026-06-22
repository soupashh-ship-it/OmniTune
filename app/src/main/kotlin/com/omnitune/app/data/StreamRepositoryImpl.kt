package com.omnitune.app.data

import com.omnitune.app.models.AppResult
import com.omnitune.app.models.StreamInfo
import com.omnitune.app.models.StreamQuality
import com.omnitune.app.models.StreamResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class StreamRepositoryImpl @Inject constructor(
    private val streamExtractor: StreamExtractor,
    private val streamCache: StreamCache,
    private val dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
) : StreamRepository {

    private suspend fun getPreferredQuality(): StreamQuality {
        val prefs = dataStore.data.first()
        val qualityName = prefs[com.omnitune.app.constants.AudioQualityKey] ?: com.omnitune.app.constants.AudioQuality.HIGH.name
        val audioQuality = try { com.omnitune.app.constants.AudioQuality.valueOf(qualityName) } catch (e: Exception) { com.omnitune.app.constants.AudioQuality.HIGH }
        return when (audioQuality) {
            com.omnitune.app.constants.AudioQuality.LOW -> StreamQuality.LOW
            com.omnitune.app.constants.AudioQuality.HIGH -> StreamQuality.HIGH
            com.omnitune.app.constants.AudioQuality.HIGHEST -> StreamQuality.BEST
            com.omnitune.app.constants.AudioQuality.AUTO -> StreamQuality.HIGH
        }
    }

    override suspend fun extractStream(songId: String): AppResult<StreamInfo> {
        val cached = streamCache.get(songId)
        if (cached != null) {
            return AppResult.Success(cached)
        }
        return try {
            val result = streamExtractor.extract(songId, getPreferredQuality())
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
            val result = streamExtractor.extract(songId, getPreferredQuality())
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
