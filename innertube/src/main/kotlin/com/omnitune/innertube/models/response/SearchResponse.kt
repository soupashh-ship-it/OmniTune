/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.omnitune.innertube.models.response

import com.omnitune.innertube.models.Continuation
import com.omnitune.innertube.models.MusicResponsiveListItemRenderer
import com.omnitune.innertube.models.Tabs
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val contents: Contents?,
    val continuationContents: ContinuationContents?,
) {
    @Serializable
    data class Contents(
        val tabbedSearchResultsRenderer: Tabs?,
        val twoColumnSearchResultsRenderer: TwoColumnSearchResultsRenderer?,
        val sectionListRenderer: com.omnitune.innertube.models.SectionListRenderer?
    ) {
        @Serializable
        data class TwoColumnSearchResultsRenderer(
            val primaryContents: PrimaryContents?,
        ) {
            @Serializable
            data class PrimaryContents(
                val sectionListRenderer: com.omnitune.innertube.models.SectionListRenderer?,
            )
        }
    }

    @Serializable
    data class ContinuationContents(
        val musicShelfContinuation: MusicShelfContinuation,
    ) {
        @Serializable
        data class MusicShelfContinuation(
            val contents: List<Content>,
            val continuations: List<Continuation>?,
        ) {
            @Serializable
            data class Content(
                val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer,
            )
        }
    }
}
