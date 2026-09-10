package com.omnitune.app.utils

import java.net.IDN
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

data class ParsedYouTubeUrl(
    val host: String,
    val pathSegments: List<String>,
    private val queryParameters: Map<String, List<String>>,
) {
    fun queryParameter(name: String): String? = queryParameters[name]?.firstOrNull()
}

object YouTubeUrlPolicy {
    private val youtubeVideoId = Regex("^[A-Za-z0-9_-]{11}$")
    private val noSchemeHosts = listOf(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
        "music.youtube.com",
        "youtu.be",
    )

    fun isAcceptedYouTubeHost(host: String?): Boolean =
        normalizedAcceptedYouTubeHost(host) != null

    fun normalizedAcceptedYouTubeHost(host: String?): String? {
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

    fun parseYouTubeUrl(value: String): ParsedYouTubeUrl? {
        val raw = value.trim()
        if (raw.isBlank()) return null

        val candidate = if (raw.needsHttpsSchemePrefix()) "https://$raw" else raw
        val uri = runCatching { URI(candidate) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.US) ?: return null
        if (scheme != "https" && scheme != "http") return null

        val host = normalizedAcceptedYouTubeHost(uri.host) ?: return null
        val pathSegments = uri.rawPath
            .orEmpty()
            .split('/')
            .filter { it.isNotBlank() }
            .map(::decodeUrlComponent)

        return ParsedYouTubeUrl(
            host = host,
            pathSegments = pathSegments,
            queryParameters = parseQuery(uri.rawQuery),
        )
    }

    fun extractPlaylistId(value: String): String? =
        parseYouTubeUrl(value)
            ?.queryParameter("list")
            ?.removePrefix("VL")
            ?.trim()
            ?.takeIf { it.isNotBlank() }

    fun extractVideoId(value: String): String? {
        val parsed = parseYouTubeUrl(value) ?: return null
        parsed.queryParameter("v")?.takeIf(::isYouTubeVideoId)?.let { return it }

        val pathId = when {
            parsed.host == "youtu.be" -> parsed.pathSegments.firstOrNull()
            parsed.pathSegments.firstOrNull() == "shorts" -> parsed.pathSegments.getOrNull(1)
            parsed.pathSegments.firstOrNull() == "embed" -> parsed.pathSegments.getOrNull(1)
            else -> null
        }

        return pathId?.takeIf(::isYouTubeVideoId)
    }

    fun isYouTubeVideoId(value: String): Boolean =
        youtubeVideoId.matches(value.trim())

    private fun String.needsHttpsSchemePrefix(): Boolean {
        val lower = lowercase(Locale.US)
        if ("://" in lower) return false
        return noSchemeHosts.any { host ->
            lower == host || lower.startsWith("$host/") || lower.startsWith("$host?")
        }
    }

    private fun parseQuery(rawQuery: String?): Map<String, List<String>> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        return rawQuery.split('&')
            .mapNotNull { pair ->
                val rawName = pair.substringBefore('=').takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val rawValue = pair.substringAfter('=', "")
                decodeUrlComponent(rawName) to decodeUrlComponent(rawValue)
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second },
            )
    }

    private fun decodeUrlComponent(value: String): String =
        runCatching {
            URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        }.getOrDefault(value)
}
