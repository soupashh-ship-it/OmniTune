package com.omnitune.app.ui.screens

/**
 * Coordinates immediate searches launched from suggestions/trending chips with the normal
 * debounced typing pipeline. Updating the backing query flow drops older pending typed queries;
 * this helper then skips only the matching debounced echo of the immediate search.
 */
internal class SearchDebounceCoordinator {
    private var immediateQueryToSkip: String? = null

    fun markImmediateSearch(query: String) {
        immediateQueryToSkip = query.normalizedQuery().takeIf { it.isNotEmpty() }
    }

    fun clearImmediateSearch() {
        immediateQueryToSkip = null
    }

    fun shouldSkipDebouncedSearch(query: String): Boolean {
        val pending = immediateQueryToSkip ?: return false
        immediateQueryToSkip = null
        return pending == query.normalizedQuery()
    }

    private fun String.normalizedQuery(): String = trim()
}
