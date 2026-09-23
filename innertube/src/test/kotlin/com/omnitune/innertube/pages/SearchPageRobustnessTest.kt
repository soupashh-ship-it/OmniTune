package com.omnitune.innertube.pages

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchPageRobustnessTest {
    @Test
    fun skipsMalformedVideosAndDeduplicatesIds() {
        val response = buildJsonArray {
            add(buildJsonObject { putJsonObject("videoRenderer") { put("videoId", "incomplete") } })
            add(videoRenderer("same"))
            add(videoRenderer("same"))
            add(videoRenderer("second"))
        }

        assertEquals(listOf("same", "second"), SearchPage.parseVideoSearchResults(response).map { it.id })
    }

    @Test
    fun deeplyNestedResponseDoesNotUseRecursiveTraversal() {
        var response: JsonElement = JsonPrimitive("not a result")
        repeat(10_000) {
            response = JsonArray(listOf(response))
        }

        assertTrue(SearchPage.parseVideoSearchResults(response).isEmpty())
    }

    @Test
    fun capsParsedVideoResults() {
        val response = buildJsonArray {
            repeat(150) { index -> add(videoRenderer("video-$index")) }
        }

        assertEquals(120, SearchPage.parseVideoSearchResults(response).size)
    }

    private fun videoRenderer(id: String) = buildJsonObject {
        putJsonObject("videoRenderer") {
            put("videoId", id)
            putJsonObject("title") { put("simpleText", "Track $id") }
            putJsonObject("thumbnail") {
                putJsonArray("thumbnails") {
                    add(buildJsonObject { put("url", "https://example.test/$id.jpg") })
                }
            }
        }
    }
}
