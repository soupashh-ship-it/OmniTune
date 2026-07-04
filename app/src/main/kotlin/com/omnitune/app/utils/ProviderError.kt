/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.utils

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Classifies provider errors (InnerTube/YouTube API calls) into typed, user-visible messages.
 */
enum class ProviderErrorType {
    /** No internet connection or network unavailable */
    NetworkUnavailable,
    /** Connection timeout or socket timeout */
    Timeout,
    /** HTTP 403 — YouTube blocked the request (bot check, geo-blocked, etc.) */
    Forbidden403,
    /** HTTP 404 — requested resource not found */
    NotFound404,
    /** HTTP 429 — rate limited, too many requests */
    TooManyRequests429,
    /** Server error (HTTP 5xx) */
    ServerError,
    /** JSON parsing failed — YouTube API format may have changed */
    ParserChanged,
    /** Catch-all for unexpected errors */
    Unknown,
}

data class ProviderError(
    val type: ProviderErrorType,
    val message: String,
    val canRetry: Boolean = true,
)

private fun statusCodeFromThrowable(throwable: Throwable): Int? {
    // Ktor ClientRequestException/ServerResponseException with expectSuccess=true
    var cursor: Throwable? = throwable
    while (cursor != null) {
        when {
            cursor is HttpRequestTimeoutException -> return -1
            cursor is ClientRequestException -> return cursor.response.status.value
            cursor is ServerResponseException -> return cursor.response.status.value
        }
        // Check message for status codes (Ktor sometimes wraps exceptions or sends text in message)
        val msg = cursor.message?.lowercase().orEmpty()
        val statusMatch = Regex("""\b(40[0-9]|429|50[0-9])\b""").find(msg)
        if (statusMatch != null) return statusMatch.value.toInt()
        cursor = cursor.cause
    }
    return null
}

fun classifyProviderError(throwable: Throwable): ProviderError {
    val statusCode = statusCodeFromThrowable(throwable)
    if (statusCode != null && statusCode != -1) {
        return when (statusCode) {
            403 -> ProviderError(
                type = ProviderErrorType.Forbidden403,
                message = "YouTube blocked this request. This may be a geo-restriction or temporary block. Try again later or use a different network.",
            )
            404 -> ProviderError(
                type = ProviderErrorType.NotFound404,
                message = "The requested content was not found. It may have been removed or the link may be incorrect.",
            )
            429 -> ProviderError(
                type = ProviderErrorType.TooManyRequests429,
                message = "YouTube rate limit hit. Too many requests in a short time. Wait a moment and try again.",
                canRetry = true,
            )
            in 500..599 -> ProviderError(
                type = ProviderErrorType.ServerError,
                message = "YouTube server error (HTTP $statusCode). Please try again later.",
            )
            else -> ProviderError(
                type = ProviderErrorType.Unknown,
                message = "Unexpected error (HTTP $statusCode). Please try again.",
            )
        }
    }

    return when (throwable) {
        is UnknownHostException, is ConnectException -> ProviderError(
            type = ProviderErrorType.NetworkUnavailable,
            message = "No internet connection. Check your network and try again.",
        )
        is SocketTimeoutException, is HttpRequestTimeoutException -> ProviderError(
            type = ProviderErrorType.Timeout,
            message = "Request timed out. Your connection may be slow. Please try again.",
        )
        is IOException -> {
            val msg = throwable.message?.lowercase().orEmpty()
            when {
                "timeout" in msg -> ProviderError(
                    type = ProviderErrorType.Timeout,
                    message = "Request timed out. Your connection may be slow. Please try again.",
                )
                "cancel" in msg || "abort" in msg || "reset" in msg || "eof" in msg -> ProviderError(
                    type = ProviderErrorType.NetworkUnavailable,
                    message = "Connection interrupted. Check your network and try again.",
                )
                else -> ProviderError(
                    type = ProviderErrorType.NetworkUnavailable,
                    message = "Network error. Check your connection and try again.",
                )
            }
        }
        is kotlinx.serialization.SerializationException -> ProviderError(
            type = ProviderErrorType.ParserChanged,
            message = "Couldn't read the response from YouTube. The data format may have changed. This usually resolves on its own.",
            canRetry = true,
        )
        else -> {
            val msg = throwable.message?.lowercase().orEmpty()
            val className = throwable::class.java.simpleName.lowercase()
            when {
                "json" in msg || "parse" in msg || "serializer" in msg || "unexpected" in msg ||
                    "json" in className || "serialization" in className -> ProviderError(
                    type = ProviderErrorType.ParserChanged,
                    message = "Couldn't read the response from YouTube. The data format may have changed.",
                    canRetry = true,
                )
                "timeout" in msg || "connect" in msg || "unknownhost" in msg || "network" in msg -> ProviderError(
                    type = ProviderErrorType.NetworkUnavailable,
                    message = "Network error. Check your connection and try again.",
                )
                "403" in msg || "forbidden" in msg -> ProviderError(
                    type = ProviderErrorType.Forbidden403,
                    message = "YouTube blocked this request. Try again later or use a different network.",
                )
                "404" in msg || "not found" in msg -> ProviderError(
                    type = ProviderErrorType.NotFound404,
                    message = "The requested content was not found.",
                )
                "429" in msg || "too many" in msg || "rate limit" in msg -> ProviderError(
                    type = ProviderErrorType.TooManyRequests429,
                    message = "YouTube rate limit hit. Wait a moment and try again.",
                )
                else -> ProviderError(
                    type = ProviderErrorType.Unknown,
                    message = throwable.localizedMessage?.take(200) ?: "An unexpected error occurred.",
                )
            }
        }
    }
}
