package com.omnitune.app.data

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
class YouTubeSearchProvider @Inject constructor() : SearchProvider {
    override suspend fun search(query: String, filter: YouTube.SearchFilter): Result<SearchResult> =
        YouTube.search(query, filter)

    override suspend fun searchSummary(query: String): Result<SearchSummaryPage> =
        YouTube.searchSummary(query)

    override suspend fun searchContinuation(continuation: String): Result<SearchResult> =
        YouTube.searchContinuation(continuation)
}
