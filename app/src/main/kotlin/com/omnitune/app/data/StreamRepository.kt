package com.omnitune.app.data

import com.omnitune.app.models.AppResult
import com.omnitune.app.models.StreamInfo

interface StreamRepository {
    suspend fun extractStream(songId: String): AppResult<StreamInfo>
    suspend fun extractWithFallbacks(songId: String): AppResult<StreamInfo>
    suspend fun probeStream(url: String): Boolean
    fun getCachedStream(songId: String): StreamInfo?
    fun clearCache()
}
