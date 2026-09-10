/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.viewmodels

import android.net.Uri
import com.omnitune.app.utils.YouTubeUrlPolicy
import java.util.Locale

internal object IncomingIntentParser {
    private val supportedVideoId = Regex("^[A-Za-z0-9_-]{6,64}$")

    fun isAcceptedYouTubeHost(host: String?): Boolean {
        return YouTubeUrlPolicy.isAcceptedYouTubeHost(host)
    }

    fun extractYouTubeVideoId(uri: Uri): String? {
        return extractYouTubeVideoId(
            host = uri.host,
            pathSegments = uri.pathSegments,
            queryParameter = uri::getQueryParameter
        )
    }

    fun extractYouTubeVideoId(
        host: String?,
        pathSegments: List<String>,
        queryParameter: (String) -> String?
    ): String? {
        val host = YouTubeUrlPolicy.normalizedAcceptedYouTubeHost(host) ?: return null
        val segments = pathSegments
        val candidate = when {
            host == "youtu.be" -> segments.firstOrNull()
            segments.firstOrNull() == "shorts" -> segments.getOrNull(1)
            segments.firstOrNull() == "embed" -> segments.getOrNull(1)
            else -> queryParameter("v")
        }

        return candidate
            ?.trim()
            ?.takeIf { supportedVideoId.matches(it) }
    }

    fun extractYouTubePlaylistId(uri: Uri): String? {
        return extractYouTubePlaylistId(
            host = uri.host,
            queryParameter = uri::getQueryParameter
        )
    }

    fun extractYouTubePlaylistId(
        host: String?,
        queryParameter: (String) -> String?
    ): String? {
        if (!YouTubeUrlPolicy.isAcceptedYouTubeHost(host)) return null
        return queryParameter("list")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun isSupportedAudioUri(uri: Uri, mimeType: String?): Boolean {
        return isSupportedAudioUri(
            scheme = uri.scheme,
            mimeType = mimeType
        )
    }

    fun isSupportedAudioUri(scheme: String?, mimeType: String?): Boolean {
        val scheme = scheme?.lowercase(Locale.US) ?: return false
        if (scheme != "content" && scheme != "file") return false
        return mimeType == null || mimeType.lowercase(Locale.US).startsWith("audio/")
    }

}
