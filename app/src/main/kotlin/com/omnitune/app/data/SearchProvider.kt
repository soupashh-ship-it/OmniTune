package com.omnitune.app.data

import com.omnitune.app.content.MusicContentPreferenceRepository
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.pages.SearchResult
import com.omnitune.innertube.pages.SearchSummaryPage
import javax.inject.Inject
import javax.inject.Singleton

/** Provider seam used by search orchestration and deterministic integration fixtures. */
interface SearchProvider {
    suspend fun search(query: String, filter: YouTube.SearchFilter): Result<SearchResult>
    suspend fun searchSummary(query: String): Result<SearchSummaryPage>
    suspend fun searchContinuation(continuation: String): Result<SearchResult>
}

@Singleton
class YouTubeSearchProvider @Inject constructor(
    private val musicContentPreferenceRepository: MusicContentPreferenceRepository,
) : SearchProvider {
    override suspend fun search(query: String, filter: YouTube.SearchFilter): Result<SearchResult> {
        musicContentPreferenceRepository.applyCurrentPreferenceToYouTube()
        return YouTube.search(query, filter)
    }

    override suspend fun searchSummary(query: String): Result<SearchSummaryPage> {
        musicContentPreferenceRepository.applyCurrentPreferenceToYouTube()
        return YouTube.searchSummary(query)
    }

    override suspend fun searchContinuation(continuation: String): Result<SearchResult> {
        musicContentPreferenceRepository.applyCurrentPreferenceToYouTube()
        return YouTube.searchContinuation(continuation)
    }
}
