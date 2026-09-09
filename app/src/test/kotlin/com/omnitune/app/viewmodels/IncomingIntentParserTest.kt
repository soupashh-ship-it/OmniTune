package com.omnitune.app.viewmodels

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingIntentParserTest {
    @Test
    fun acceptsOnlyStrictYouTubeHosts() {
        assertTrue(IncomingIntentParser.isAcceptedYouTubeHost("youtube.com"))
        assertTrue(IncomingIntentParser.isAcceptedYouTubeHost("www.youtube.com"))
        assertTrue(IncomingIntentParser.isAcceptedYouTubeHost("music.youtube.com"))
        assertTrue(IncomingIntentParser.isAcceptedYouTubeHost("m.youtube.com."))
        assertTrue(IncomingIntentParser.isAcceptedYouTubeHost("youtu.be"))

        assertFalse(IncomingIntentParser.isAcceptedYouTubeHost("youtube.com.example.com"))
        assertFalse(IncomingIntentParser.isAcceptedYouTubeHost("notyoutube.com"))
        assertFalse(IncomingIntentParser.isAcceptedYouTubeHost("youtu.be.example.com"))
    }

    @Test
    fun extractsSupportedYouTubeVideoIds() {
        assertEquals(
            "abc123_X-y",
            IncomingIntentParser.extractYouTubeVideoId(
                host = "youtube.com",
                pathSegments = listOf("watch"),
                queryParameter = query("v" to "abc123_X-y")
            )
        )
        assertEquals(
            "shorts1234",
            IncomingIntentParser.extractYouTubeVideoId(
                host = "www.youtube.com",
                pathSegments = listOf("shorts", "shorts1234"),
                queryParameter = query()
            )
        )
        assertEquals(
            "embed1234",
            IncomingIntentParser.extractYouTubeVideoId(
                host = "www.youtube.com",
                pathSegments = listOf("embed", "embed1234"),
                queryParameter = query()
            )
        )
        assertEquals(
            "youtu1234",
            IncomingIntentParser.extractYouTubeVideoId(
                host = "youtu.be",
                pathSegments = listOf("youtu1234"),
                queryParameter = query()
            )
        )
    }

    @Test
    fun rejectsUnsupportedVideoIdsAndHosts() {
        assertNull(
            IncomingIntentParser.extractYouTubeVideoId(
                host = "youtube.com.example.com",
                pathSegments = listOf("watch"),
                queryParameter = query("v" to "abc12345")
            )
        )
        assertNull(
            IncomingIntentParser.extractYouTubeVideoId(
                host = "youtube.com",
                pathSegments = listOf("watch"),
                queryParameter = query("v" to "bad id!")
            )
        )
        assertNull(
            IncomingIntentParser.extractYouTubeVideoId(
                host = "youtu.be.example.com",
                pathSegments = listOf("abc12345"),
                queryParameter = query()
            )
        )
    }

    @Test
    fun extractsYouTubePlaylistIds() {
        assertEquals(
            "PL12345",
            IncomingIntentParser.extractYouTubePlaylistId(
                host = "music.youtube.com",
                queryParameter = query("list" to "PL12345")
            )
        )
        assertNull(
            IncomingIntentParser.extractYouTubePlaylistId(
                host = "youtube.com.example.com",
                queryParameter = query("list" to "PL12345")
            )
        )
    }

    @Test
    fun acceptsOnlyLocalAudioSchemesAndMimeTypes() {
        assertTrue(IncomingIntentParser.isSupportedAudioUri("content", "audio/mpeg"))
        assertTrue(IncomingIntentParser.isSupportedAudioUri("file", "audio/flac"))
        assertTrue(IncomingIntentParser.isSupportedAudioUri("content", null))

        assertFalse(IncomingIntentParser.isSupportedAudioUri("https", "audio/mpeg"))
        assertFalse(IncomingIntentParser.isSupportedAudioUri("content", "text/plain"))
    }

    private fun query(vararg pairs: Pair<String, String>): (String) -> String? {
        val values = pairs.toMap()
        return { name -> values[name] }
    }
}
