package com.omnitune.innertube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchQueryPolicyTest {
    @Test
    fun normalizesWhitespaceAndControlCharacters() {
        assertEquals("hello world", SearchQueryPolicy.normalize(" \nhello\u0000\t  world \n"))
    }

    @Test
    fun preservesUnicodeAndEmoji() {
        assertEquals("हिंदी 🎵", SearchQueryPolicy.normalize("  हिंदी   🎵  "))
    }

    @Test
    fun limitsInputWithoutSplittingSurrogatePairs() {
        val normalized = SearchQueryPolicy.normalize("🎵".repeat(SearchQueryPolicy.MAX_QUERY_CODE_POINTS + 5))

        assertEquals(SearchQueryPolicy.MAX_QUERY_CODE_POINTS, normalized.codePointCount(0, normalized.length))
        assertTrue(normalized.all { Character.isSurrogate(it) })
    }

    @Test
    fun dropsUnpairedSurrogatesAndBlankQueries() {
        assertEquals("music", SearchQueryPolicy.normalize("mu\uD800sic"))
        assertEquals("", SearchQueryPolicy.normalize(" \u0000\n "))
    }
}
