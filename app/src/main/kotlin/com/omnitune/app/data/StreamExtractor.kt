package com.omnitune.app.data

import android.content.Context
import android.net.ConnectivityManager
import com.omnitune.app.constants.AudioQuality
import com.omnitune.app.constants.PlayerStreamClient
import com.omnitune.app.models.StreamQuality
import com.omnitune.app.models.StreamResult
import com.omnitune.app.utils.YTPlayerUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clientRotator: ClientRotator,
) {

    suspend fun extractWithFallback(songId: String, quality: StreamQuality, maxAttempts: Int = 3): StreamResult? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        val audioQuality = when (quality) {
            StreamQuality.LOW -> AudioQuality.LOW
            StreamQuality.MEDIUM, StreamQuality.HIGH -> AudioQuality.HIGH
            StreamQuality.BEST -> AudioQuality.HIGHEST
        }

        for (attempt in 0 until maxAttempts) {
            val client = clientRotator.getNextClient(songId)
            val result = YTPlayerUtils.playerResponseForPlayback(
                videoId = songId,
                audioQuality = audioQuality,
                connectivityManager = cm,
                preferredStreamClient = client,
            )
            
            val streamResult = result.fold(
                onSuccess = { data ->
                    clientRotator.reportSuccess(songId)
                    StreamResult(
                        url = data.streamUrl,
                        contentType = data.format.mimeType,
                        contentLength = data.format.contentLength,
                        bitrate = data.format.bitrate,
                    )
                },
                onFailure = {
                    clientRotator.reportFailure(songId)
                    null
                }
            )

            if (streamResult != null) {
                return streamResult
            }
        }
        return null
    }

    suspend fun extract(songId: String, quality: StreamQuality): StreamResult? {
        return extractWithFallback(songId, quality)
    }

    fun invalidate(songId: String) {
        YTPlayerUtils.invalidateCachedStreamUrls(songId)
    }
}
