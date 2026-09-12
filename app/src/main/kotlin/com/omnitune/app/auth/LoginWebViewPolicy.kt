/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.auth

import java.net.URI
import java.util.Locale

object LoginWebViewPolicy {
    private val allowedAuthDomains = setOf(
        "google.com",
        "youtube.com",
        "gstatic.com",
        "googleusercontent.com",
    )

    private val successfulYoutubeHosts = setOf(
        "music.youtube.com",
        "www.youtube.com",
        "youtube.com",
    )

    fun isAllowedNavigation(rawUrl: String?): Boolean {
        val uri = rawUrl.toUriOrNull() ?: return false
        if (uri.scheme.equals("about", ignoreCase = true) && uri.schemeSpecificPart == "blank") {
            return true
        }
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        val host = uri.normalizedHost() ?: return false
        return allowedAuthDomains.any { allowed -> host == allowed || host.endsWith(".$allowed") }
    }

    fun shouldCompleteLogin(rawUrl: String?, cookieHeader: String?): Boolean =
        isSuccessfulLoginReturnUrl(rawUrl) && hasRequiredSessionCookie(cookieHeader)

    internal fun isSuccessfulLoginReturnUrl(rawUrl: String?): Boolean {
        val uri = rawUrl.toUriOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        val host = uri.normalizedHost() ?: return false
        if (host !in successfulYoutubeHosts) return false

        val path = uri.rawPath.orEmpty().ifBlank { "/" }
        return when (host) {
            "music.youtube.com" -> path == "/" || path.startsWith("/watch") || path.startsWith("/playlist")
            "www.youtube.com",
            "youtube.com" -> path == "/signin" && uri.rawQuery.orEmpty().contains("action_handle_signin=true")
            else -> false
        }
    }

    internal fun hasRequiredSessionCookie(cookieHeader: String?): Boolean {
        val cookieNames = cookieHeader.orEmpty()
            .split(';')
            .mapNotNull { cookie ->
                cookie.substringBefore('=').trim().takeIf { it.isNotEmpty() }
            }
            .toSet()
        return cookieNames.any { it == "SAPISID" || it == "__Secure-3PAPISID" }
    }

    private fun String?.toUriOrNull(): URI? =
        runCatching { URI(this ?: return null) }.getOrNull()

    private fun URI.normalizedHost(): String? =
        host
            ?.trimEnd('.')
            ?.lowercase(Locale.ROOT)
}
