package com.omnitune.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchDebounceCoordinatorTest {
    @Test
    fun immediateSearchSkipsMatchingDebouncedEchoOnce() {
        val coordinator = SearchDebounceCoordinator()

        coordinator.markImmediateSearch("midnight city")

        assertTrue(coordinator.shouldSkipDebouncedSearch("midnight city"))
        assertFalse(coordinator.shouldSkipDebouncedSearch("midnight city"))
    }

    @Test
    fun differentDebouncedQueryIsNotSkippedAndClearsPendingImmediateMarker() {
        val coordinator = SearchDebounceCoordinator()

        coordinator.markImmediateSearch("midnight city")

        assertFalse(coordinator.shouldSkipDebouncedSearch("midnight remix"))
        assertFalse(coordinator.shouldSkipDebouncedSearch("midnight city"))
    }

    @Test
    fun immediateSearchMatchingUsesTrimmedQuery() {
        val coordinator = SearchDebounceCoordinator()

        coordinator.markImmediateSearch("  midnight city  ")

        assertTrue(coordinator.shouldSkipDebouncedSearch("midnight city"))
    }

    @Test
    fun clearingImmediateSearchAllowsDebouncedSearch() {
        val coordinator = SearchDebounceCoordinator()

        coordinator.markImmediateSearch("midnight city")
        coordinator.clearImmediateSearch()

        assertFalse(coordinator.shouldSkipDebouncedSearch("midnight city"))
    }
}
