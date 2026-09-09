/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import java.util.concurrent.TimeUnit
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import timber.log.Timber

internal class SponsorBlockApiClient(
    baseClient: OkHttpClient,
) {
    private val httpClient = baseClient.newBuilder()
        .callTimeout(ApiTimeoutSeconds, TimeUnit.SECONDS)
        .build()

    fun fetchSkipSegments(
        videoId: String,
        categories: List<String> = SponsorBlockDefaults.SkipCategories,
    ): List<SponsorBlockSegment> {
        val cleanVideoId = videoId.trim()
        if (!SponsorBlockVideoIdResolver.isYouTubeVideoId(cleanVideoId)) return emptyList()

        val url = ApiUrl.newBuilder()
            .addQueryParameter("videoID", cleanVideoId)
            .addQueryParameter("categories", categories.toJsonArrayString())
            .addQueryParameter("actionTypes", listOf("skip").toJsonArrayString())
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "OmniTune")
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (response.code == 404) return emptyList()
                if (!response.isSuccessful) {
                    Timber.tag("SponsorBlock").w("Segment lookup failed with HTTP %s", response.code)
                    return emptyList()
                }
                SponsorBlockSegmentParser.parseSkipSegments(response.body.string())
            }
        } catch (error: Exception) {
            Timber.tag("SponsorBlock").w(error, "Segment lookup failed")
            emptyList()
        }
    }

    private fun List<String>.toJsonArrayString(): String =
        JSONArray().also { array -> forEach(array::put) }.toString()

    private companion object {
        const val ApiTimeoutSeconds = 4L
        val ApiUrl = "https://sponsor.ajay.app/api/skipSegments".toHttpUrl()
    }
}
