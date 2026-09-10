package com.omnitune.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeUrlPolicyTest {
    @Test
    fun acceptedHostRejectsAttackerControlledSuffixes() {
        assertTrue(YouTubeUrlPolicy.isAcceptedYouTubeHost("youtube.com"))
        assertTrue(YouTubeUrlPolicy.isAcceptedYouTubeHost("music.youtube.com."))
        assertTrue(YouTubeUrlPolicy.isAcceptedYouTubeHost("youtu.be"))

        assertFalse(YouTubeUrlPolicy.isAcceptedYouTubeHost("youtube.com.example.com"))
        assertFalse(YouTubeUrlPolicy.isAcceptedYouTubeHost("youtu.be.example.com"))
        assertFalse(YouTubeUrlPolicy.isAcceptedYouTubeHost("notyoutube.com"))
    }

    @Test
    fun playlistExtractionRequiresAcceptedYouTubeUrl() {
        assertEquals(
            "PL123",
            YouTubeUrlPolicy.extractPlaylistId("https://music.youtube.com/playlist?list=PL123"),
        )
        assertEquals(
            "PL999",
            YouTubeUrlPolicy.extractPlaylistId("music.youtube.com/playlist?list=VLPL999"),
        )

        assertNull(YouTubeUrlPolicy.extractPlaylistId("https://youtube.com.example.com/playlist?list=PL123"))
        assertNull(YouTubeUrlPolicy.extractPlaylistId("https://example.com/watch?list=PL123"))
    }

    @Test
    fun videoExtractionSupportsYouTubeFormsOnly() {
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeUrlPolicy.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
        )
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeUrlPolicy.extractVideoId("https://youtu.be/dQw4w9WgXcQ"),
        )
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeUrlPolicy.extractVideoId("youtube.com/shorts/dQw4w9WgXcQ"),
        )

        assertNull(YouTubeUrlPolicy.extractVideoId("https://youtube.com.example.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun bareVideoIdsAreRecognizedForCsvImports() {
        assertTrue(YouTubeUrlPolicy.isYouTubeVideoId("dQw4w9WgXcQ"))
        assertFalse(YouTubeUrlPolicy.isYouTubeVideoId("bad id!"))
    }
}
