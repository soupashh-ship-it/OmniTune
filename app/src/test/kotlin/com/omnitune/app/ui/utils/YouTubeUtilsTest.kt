package com.omnitune.app.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeUtilsTest {
    @Test
    fun resizeUpgradesYouTubeVideoThumbnails() {
        val thumbnail = "https://i.ytimg.com/vi/video123/hqdefault.jpg"

        assertEquals(
            "https://i.ytimg.com/vi/video123/maxresdefault.jpg",
            thumbnail.resize(960, 960),
        )
    }

    @Test
    fun resizeUpgradesYouTubeWebpThumbnails() {
        val thumbnail = "https://i.ytimg.com/vi_webp/video123/mqdefault.webp"

        assertEquals(
            "https://i.ytimg.com/vi_webp/video123/maxresdefault.webp",
            thumbnail.resize(960, 960),
        )
    }

    @Test
    fun resizeReplacesYt3SizeParameter() {
        val thumbnail = "https://yt3.ggpht.com/example=s88-c-k-c0x00ffffff-no-rj"

        assertEquals(
            "https://yt3.ggpht.com/example=s960",
            thumbnail.resize(960, 960),
        )
    }
}
