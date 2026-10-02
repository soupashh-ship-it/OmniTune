package com.omnitune.shared.data.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class InnertubeContext(
    val client: InnertubeClientContext = InnertubeClientContext(),
)

@Serializable
data class InnertubeClientContext(
    val clientName: String = "WEB_REMIX",
    val clientVersion: String = "1.20260114.01.00",
    val hl: String = "en",
    val gl: String = "US",
)

@Serializable
data class SearchRequestBody(
    val context: InnertubeContext = InnertubeContext(),
    val query: String,
    val params: String? = null,
)

@Serializable
data class SuggestionsRequestBody(
    val context: InnertubeContext = InnertubeContext(),
    val input: String,
)

@Serializable
data class BrowseRequestBody(
    val context: InnertubeContext = InnertubeContext(),
    val browseId: String,
)

@Serializable
data class SearchSuggestionsResponse(
    val contents: List<SearchSuggestionContent>? = null,
)

@Serializable
data class SearchSuggestionContent(
    val searchSuggestionsSectionRenderer: SearchSuggestionsSectionRenderer? = null,
)

@Serializable
data class SearchSuggestionsSectionRenderer(
    val contents: List<SearchSuggestionItem>? = null,
)

@Serializable
data class SearchSuggestionItem(
    val searchSuggestionRenderer: SearchSuggestionRenderer? = null,
)

@Serializable
data class SearchSuggestionRenderer(
    val suggestion: SearchRuns? = null,
)

@Serializable
data class SearchRuns(
    val runs: List<SearchRunText>? = null,
)

@Serializable
data class SearchRunText(
    val text: String = "",
)

@Serializable
data class StreamingDataResponse(
    val formats: List<FormatDto> = emptyList(),
    val adaptiveFormats: List<AdaptiveFormatDto> = emptyList(),
    val expiresInSeconds: String? = null,
)

@Serializable
data class FormatDto(
    val itag: Int,
    val url: String? = null,
    val mimeType: String = "",
    val bitrate: Int = 0,
    val audioQuality: String? = null,
)

@Serializable
data class AdaptiveFormatDto(
    val itag: Int,
    val url: String? = null,
    val mimeType: String = "",
    val bitrate: Int = 0,
    val averageBitrate: Int? = null,
    val contentLength: String? = null,
    val audioQuality: String? = null,
    val audioSampleRate: String? = null,
    val audioChannels: Int? = null,
)
