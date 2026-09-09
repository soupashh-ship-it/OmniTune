/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.viewmodels

import android.net.Uri
import java.net.IDN
import java.util.Locale

internal object IncomingIntentParser {
    private val supportedVideoId = Regex("^[A-Za-z0-9_-]{6,64}$")

    fun isAcceptedYouTubeHost(host: String?): Boolean {
        return normalizedAcceptedYouTubeHost(host) != null
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
        val host = normalizedAcceptedYouTubeHost(host) ?: return null
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
        if (normalizedAcceptedYouTubeHost(host) == null) return null
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

    private fun normalizedAcceptedYouTubeHost(host: String?): String? {
        val normalizedHost = host
            ?.trim()
            ?.trimEnd('.')
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { IDN.toASCII(it) }.getOrDefault(it) }
            ?.lowercase(Locale.US)
            ?: return null

        return normalizedHost.takeIf {
            it == "youtu.be" ||
                it == "youtube.com" ||
                it.endsWith(".youtube.com")
        }
    }
}
