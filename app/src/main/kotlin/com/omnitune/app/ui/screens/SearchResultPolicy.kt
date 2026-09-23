package com.omnitune.app.ui.screens

internal object SearchResultPolicy {
    const val MAX_SUGGESTIONS = 20

    fun <T> uniqueById(items: List<T>, id: (T) -> String): List<T> {
        val seen = HashSet<String>(items.size)
        return items.filter { item ->
            val itemId = id(item)
            itemId.isNotBlank() && seen.add(itemId)
        }
    }

    fun uniqueSuggestions(suggestions: List<String>): List<String> =
        suggestions.asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .take(MAX_SUGGESTIONS)
            .toList()
}
