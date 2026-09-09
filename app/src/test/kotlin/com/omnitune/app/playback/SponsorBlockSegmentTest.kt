package com.omnitune.app.playback

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SponsorBlockSegmentTest {
    @Test
    fun parserKeepsOnlyValidSkipSegmentsSortedByStartTime() {
        val segments = SponsorBlockSegmentParser.parseSkipSegments(
            """
            [
              {"category":"outro","actionType":"skip","segment":[120.0,130.5],"UUID":"end"},
              {"category":"selfpromo","actionType":"mute","segment":[40.0,50.0],"UUID":"muted"},
              {"category":"intro","actionType":"skip","segment":[5.25,9.75],"UUID":"start"},
              {"category":"bad","actionType":"skip","segment":[80.0,70.0],"UUID":"bad"}
            ]
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                SponsorBlockSegment(startMs = 5_250L, endMs = 9_750L, category = "intro", uuid = "start"),
                SponsorBlockSegment(startMs = 120_000L, endMs = 130_500L, category = "outro", uuid = "end"),
            ),
            segments,
        )
    }

    @Test
    fun parserTreatsMalformedResponsesAsNoSegments() {
        assertEquals(emptyList<SponsorBlockSegment>(), SponsorBlockSegmentParser.parseSkipSegments("{nope"))
    }

    @Test
    fun skipPolicySeeksPastCurrentSegment() {
        val target = SponsorBlockSkipPolicy.seekTargetMs(
            segments = listOf(SponsorBlockSegment(10_000L, 20_000L, "sponsor", "abc")),
            positionMs = 12_000L,
            durationMs = 90_000L,
        )

        assertEquals(20_250L, target)
    }

    @Test
    fun skipPolicyUsesFarthestEndForOverlappingSegments() {
        val target = SponsorBlockSkipPolicy.seekTargetMs(
            segments = listOf(
                SponsorBlockSegment(10_000L, 18_000L, "sponsor", "a"),
                SponsorBlockSegment(11_000L, 25_000L, "selfpromo", "b"),
            ),
            positionMs = 12_000L,
            durationMs = C.TIME_UNSET,
        )

        assertEquals(25_250L, target)
    }

    @Test
    fun skipPolicyDoesNotSeekOutsideSegmentsOrAtSegmentTail() {
        val segments = listOf(SponsorBlockSegment(10_000L, 20_000L, "sponsor", "abc"))

        assertNull(SponsorBlockSkipPolicy.seekTargetMs(segments, positionMs = 9_000L, durationMs = 90_000L))
        assertNull(SponsorBlockSkipPolicy.seekTargetMs(segments, positionMs = 19_800L, durationMs = 90_000L))
    }

    @Test
    fun skipPolicyClampsToKnownDuration() {
        val target = SponsorBlockSkipPolicy.seekTargetMs(
            segments = listOf(SponsorBlockSegment(10_000L, 20_000L, "outro", "abc")),
            positionMs = 12_000L,
            durationMs = 19_500L,
        )

        assertEquals(19_500L, target)
    }

    @Test
    fun videoIdResolverPrefersMediaIdWhenPresent() {
        val fromMediaId = MediaItem.Builder()
            .setMediaId("dQw4w9WgXcQ")
            .build()
        val nonYoutube = MediaItem.Builder()
            .setMediaId("local-file")
            .build()

        assertEquals("dQw4w9WgXcQ", SponsorBlockVideoIdResolver.fromMediaItem(fromMediaId))
        assertNull(SponsorBlockVideoIdResolver.fromMediaItem(nonYoutube))
    }

    @Test
    fun videoIdResolverRecognizesOnlyBareYouTubeVideoIds() {
        assertEquals(true, SponsorBlockVideoIdResolver.isYouTubeVideoId("dQw4w9WgXcQ"))
        assertEquals(false, SponsorBlockVideoIdResolver.isYouTubeVideoId("https://youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals(false, SponsorBlockVideoIdResolver.isYouTubeVideoId("too-short"))
    }
}
