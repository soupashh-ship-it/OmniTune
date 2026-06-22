package com.omnitune.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.omnitune.app.constants.AudioQuality
import com.omnitune.app.constants.PlayerStreamClient
import com.omnitune.app.models.StreamQuality
import com.omnitune.app.models.StreamResult
import com.omnitune.app.utils.YTPlayerUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class StreamResolveResult {
    data class Success(
        val stream: StreamResult,
        val clientUsed: PlayerStreamClient
    ) : StreamResolveResult()

    data class Failure(
        val videoId: String,
        val reason: PlaybackResolveError,
        val attemptedClients: List<PlayerStreamClient>,
        val cause: Throwable? = null
    ) : StreamResolveResult()
}

sealed class PlaybackResolveError {
    data object NoNetwork : PlaybackResolveError()
    data object NoPlayableFormat : PlaybackResolveError()
    data object ClientBlocked : PlaybackResolveError()
    data object UrlExpired : PlaybackResolveError()
    data object RegionBlocked : PlaybackResolveError()
    data object LoginRequired : PlaybackResolveError()
    data class Unknown(val message: String?) : PlaybackResolveError()
}

@Singleton
class StreamExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clientRotator: ClientRotator,
) {

    suspend fun resolveWithFallback(songId: String, quality: StreamQuality): StreamResolveResult {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return StreamResolveResult.Failure(songId, PlaybackResolveError.NoNetwork, emptyList())
        if (!hasNetwork(cm)) {
            return StreamResolveResult.Failure(songId, PlaybackResolveError.NoNetwork, emptyList())
        }

        val audioQuality = when (quality) {
            StreamQuality.LOW -> AudioQuality.LOW
            StreamQuality.MEDIUM, StreamQuality.HIGH -> AudioQuality.HIGH
            StreamQuality.BEST -> AudioQuality.HIGHEST
        }

        val attemptedClients = mutableListOf<PlayerStreamClient>()
        var lastFailure: Throwable? = null
        var lastReason: PlaybackResolveError = PlaybackResolveError.NoPlayableFormat

        for (client in clientRotator.getClientSequence(songId)) {
            attemptedClients += client
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
                onFailure = { throwable ->
                    lastFailure = throwable
                    lastReason = classifyFailure(throwable)
                    clientRotator.reportFailure(songId)
                    null
                }
            )

            if (streamResult != null) {
                return StreamResolveResult.Success(streamResult, client)
            }
        }
        return StreamResolveResult.Failure(songId, lastReason, attemptedClients, lastFailure)
    }

    suspend fun extractWithFallback(songId: String, quality: StreamQuality): StreamResult? {
        return when (val result = resolveWithFallback(songId, quality)) {
            is StreamResolveResult.Success -> result.stream
            is StreamResolveResult.Failure -> null
        }
    }

    suspend fun extract(songId: String, quality: StreamQuality): StreamResult? {
        return extractWithFallback(songId, quality)
    }

    fun invalidate(songId: String) {
        YTPlayerUtils.invalidateCachedStreamUrls(songId)
    }

    private fun hasNetwork(connectivityManager: ConnectivityManager): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun classifyFailure(throwable: Throwable): PlaybackResolveError {
        val message = throwable.message.orEmpty()
        val lower = message.lowercase()
        return when {
            "login" in lower || "sign in" in lower -> PlaybackResolveError.LoginRequired
            "region" in lower || "country" in lower || "not available" in lower -> PlaybackResolveError.RegionBlocked
            "expired" in lower || "403" in lower || "404" in lower -> PlaybackResolveError.UrlExpired
            "blocked" in lower || "bot" in lower || "captcha" in lower -> PlaybackResolveError.ClientBlocked
            "format" in lower || "stream" in lower -> PlaybackResolveError.NoPlayableFormat
            else -> PlaybackResolveError.Unknown(message.ifBlank { throwable::class.java.simpleName })
        }
    }
}
