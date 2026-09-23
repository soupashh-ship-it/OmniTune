package com.omnitune.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchResultPolicyTest {
    private data class Result(val id: String, val title: String)

    @Test
    fun removesDuplicateAndBlankIdsWhilePreservingOrder() {
        val results = listOf(
            Result("artist-1", "First"),
            Result("artist-1", "Duplicate"),
            Result("", "Invalid"),
            Result("artist-2", "Second"),
        )

        assertEquals(
            listOf(Result("artist-1", "First"), Result("artist-2", "Second")),
            SearchResultPolicy.uniqueById(results) { it.id },
        )
    }

    @Test
    fun trimsAndDeduplicatesSuggestions() {
        assertEquals(
            listOf("hello", "world"),
            SearchResultPolicy.uniqueSuggestions(listOf(" hello ", "hello", "", "world")),
        )
    }
}
