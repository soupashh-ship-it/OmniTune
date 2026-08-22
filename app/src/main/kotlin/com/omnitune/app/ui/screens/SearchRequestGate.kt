package com.omnitune.app.ui.screens

/**
 * Rejects late asynchronous search work after a query, tab, or preference change.
 *
 * Provider calls may finish after cancellation, so cancellation alone cannot be the publication
 * boundary. Every UI update from a search request must pass this gate.
 */
internal data class SearchRequest(
    val generation: Long,
    val query: String,
    val filter: SearchFilterTab,
)

internal class SearchRequestGate {
    private var generation = 0L
    private var current: SearchRequest? = null

    fun begin(query: String, filter: SearchFilterTab): SearchRequest =
        SearchRequest(
            generation = ++generation,
            query = query,
            filter = filter,
        ).also { current = it }

    fun invalidate() {
        generation++
        current = null
    }

    fun currentFor(query: String, filter: SearchFilterTab): SearchRequest? =
        current?.takeIf { it.query == query && it.filter == filter && it.generation == generation }

    fun accepts(request: SearchRequest): Boolean =
        request == current && request.generation == generation
}
