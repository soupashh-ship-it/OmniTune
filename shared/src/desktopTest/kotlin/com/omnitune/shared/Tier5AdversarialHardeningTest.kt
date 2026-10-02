package com.omnitune.shared

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.omnitune.shared.data.innertube.AudioFormatCandidate
import com.omnitune.shared.data.innertube.PoTokenGenerator
import com.omnitune.shared.data.innertube.StreamResolver
import com.omnitune.shared.data.lyrics.KuGouClient
import com.omnitune.shared.data.lyrics.LrcLibClient
import com.omnitune.shared.data.lyrics.LrcParser
import com.omnitune.shared.data.lyrics.LyricsService
import com.omnitune.shared.data.lyrics.SimpMusicClient
import com.omnitune.shared.domain.models.LyricLine
import com.omnitune.shared.domain.models.Lyrics
import com.omnitune.shared.domain.models.SongItem
import com.omnitune.shared.playback.DesktopAudioPlayer
import com.omnitune.shared.playback.PlaybackControllerImpl
import com.omnitune.shared.playback.PlaybackState
import com.omnitune.shared.playback.QueueManager
import com.omnitune.shared.playback.RepeatMode
import com.omnitune.shared.ui.components.SquircleShape
import com.omnitune.shared.ui.theme.PaletteDefault
import com.omnitune.shared.ui.theme.SuvMusicPalette
import com.omnitune.shared.ui.theme.SuvMusicThemeEngine
import com.omnitune.shared.ui.theme.buildColorScheme
import com.omnitune.shared.ui.theme.getPaletteColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Milestone 5 Tier 5 White-Box Adversarial Hardening Test Suite.
 *
 * Verifies exhaustive edge cases and stress boundaries across:
 * 1. PoToken bitwise manipulation and corrupt token recovery.
 * 2. Direct AAC stream candidate extraction fallback logic.
 * 3. Multi-provider lyrics failure cascades and empty lyrics fallbacks.
 * 4. QueueManager boundary conditions, massive queue shuffling with non-destructive restore, rapid deletions of current item.
 * 5. PlaybackController StateFlow transitions under rapid concurrent commands.
 * 6. Theme dynamic luminance clamping and high-contrast squircle boundaries.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalEncodingApi::class)
class Tier5AdversarialHardeningTest {

    private val testDensity = Density(1f)

    private fun createSong(id: String, durationSec: Int = 180): SongItem {
        return SongItem(
            id = id,
            title = "Track Title $id",
            artist = "Artist Name $id",
            album = "Album Name $id",
            duration = durationSec,
            thumbnailUrl = "https://images.omnitune.app/thumb/$id.jpg"
        )
    }

    // =========================================================================
    // 1. POTOKEN BITWISE MANIPULATION & CORRUPT TOKEN RECOVERY
    // =========================================================================

    @Test
    fun testPoTokenExhaustiveBitwiseManipulationAndRecovery() {
        val visitorData = "VisitorData_Tier5_Hardening"
        val timestamp = 1775000000L
        val salt = ByteArray(16) { (it * 17 + 3).toByte() }

        val validToken = PoTokenGenerator.generatePoToken(
            visitorData = visitorData,
            timestamp = timestamp,
            salt = salt
        )

        val initialVerify = PoTokenGenerator.verifyPoToken(validToken)
        assertTrue(initialVerify.valid, "Initial token must be strictly valid")
        assertEquals(visitorData, initialVerify.visitorData)
        assertEquals(timestamp, initialVerify.timestamp)

        // Decode Base64 URL-safe packet
        var b64 = validToken.replace('-', '+').replace('_', '/')
        while (b64.length % 4 != 0) b64 += "="
        val rawPacket = Base64.Default.decode(b64)
        assertTrue(rawPacket.size >= 74, "Packet size must be at least 74 bytes")

        // Exhaustive bitwise tampering: Flip every single bit in each byte
        // Bytes 0..15: Salt
        // Bytes 16..17: Visitor length
        // Bytes 18..18+len-1: Masked visitor
        // Bytes tsOffset..tsOffset+7: Masked timestamp
        // Bytes tsOffset+8..tsOffset+23: Masked salt
        // Final 32 bytes: HMAC-SHA256 integrity tag
        for (byteIdx in rawPacket.indices) {
            for (bit in 0..7) {
                val corruptedBytes = rawPacket.copyOf()
                corruptedBytes[byteIdx] = (corruptedBytes[byteIdx].toInt() xor (1 shl bit)).toByte()

                val corruptedToken = Base64.UrlSafe.encode(corruptedBytes).trimEnd('=')
                val result = PoTokenGenerator.verifyPoToken(corruptedToken)

                assertFalse(
                    result.valid,
                    "Tampered token at byte $byteIdx bit $bit must fail verification"
                )
                assertNotNull(result.error, "Failed token must provide an explanatory error message")
            }
        }

        // Test truncated packets of every possible length from 0 to 73 bytes
        for (len in 0 until 74) {
            val truncatedBytes = rawPacket.copyOf(len)
            val truncatedToken = Base64.UrlSafe.encode(truncatedBytes).trimEnd('=')
            val result = PoTokenGenerator.verifyPoToken(truncatedToken)
            assertFalse(result.valid, "Truncated packet of length $len must be rejected")
            assertEquals("Token packet too short", result.error)
        }

        // Test oversized garbage data appended to valid token
        val oversizedBytes = rawPacket + ByteArray(256) { 0xFF.toByte() }
        val oversizedToken = Base64.UrlSafe.encode(oversizedBytes).trimEnd('=')
        val oversizedResult = PoTokenGenerator.verifyPoToken(oversizedToken)
        assertFalse(oversizedResult.valid, "Oversized packet must fail HMAC verification")

        // Test complete corrupt recovery: After hundreds of invalid verifications,
        // a freshly generated token must verify 100% cleanly without state pollution
        val recoveryToken = PoTokenGenerator.generatePoToken("CleanVisitorAfterCorruption", 1775001000L)
        val recoveryResult = PoTokenGenerator.verifyPoToken(recoveryToken)
        assertTrue(recoveryResult.valid, "System must cleanly recover and verify valid tokens")
        assertEquals("CleanVisitorAfterCorruption", recoveryResult.visitorData)
        assertEquals(1775001000L, recoveryResult.timestamp)
    }

    @Test
    fun testPoTokenBoundarySaltsAndExtremeTimestamps() {
        val zeroSalt = ByteArray(16) { 0.toByte() }
        val maxSalt = ByteArray(16) { 0xFF.toByte() }
        val alternatingSalt = ByteArray(16) { if (it % 2 == 0) 0xAA.toByte() else 0x55.toByte() }

        val testSalts = listOf(zeroSalt, maxSalt, alternatingSalt)
        for (s in testSalts) {
            val token = PoTokenGenerator.generatePoToken("SaltBoundaryTest", salt = s)
            val result = PoTokenGenerator.verifyPoToken(token)
            assertTrue(result.valid, "Token with boundary salt must verify successfully")
        }

        // Boundary timestamps
        val boundaryTimestamps = listOf(
            0L,
            1L,
            2147483647L,
            4294967295L,
            Long.MAX_VALUE - 1,
            Long.MAX_VALUE
        )
        for (ts in boundaryTimestamps) {
            val token = PoTokenGenerator.generatePoToken("TimestampBoundary", timestamp = ts)
            val result = PoTokenGenerator.verifyPoToken(token)
            assertTrue(result.valid, "Timestamp $ts must verify successfully")
            assertEquals(ts, result.timestamp)
        }
    }

    // =========================================================================
    // 2. DIRECT AAC STREAM CANDIDATE EXTRACTION FALLBACK LOGIC
    // =========================================================================

    @Test
    fun testDirectAacCandidateExtractionAndFallbacks() {
        // Priority 1: Exact itag 140 (128kbps AAC) is preferred even when higher bitrate WebM exists
        val candidatesStandard = listOf(
            AudioFormatCandidate(itag = 251, container = "webm", codec = "opus", bitrate = 160000, mimeType = "audio/webm"),
            AudioFormatCandidate(itag = 140, container = "m4a", codec = "mp4a.40.2", bitrate = 128000, mimeType = "audio/mp4"),
            AudioFormatCandidate(itag = 139, container = "m4a", codec = "mp4a.40.2", bitrate = 48000, mimeType = "audio/mp4")
        )
        val selected140 = StreamResolver.selectBestAudioFormat(candidatesStandard)
        assertNotNull(selected140)
        assertEquals(140, selected140.itag, "Must prefer itag 140 over higher bitrate WebM 251")

        // Priority 2: Fallback to itag 139 (48kbps AAC) when 140 is missing, prioritizing AAC over Opus
        val candidatesMissing140 = listOf(
            AudioFormatCandidate(itag = 251, container = "webm", codec = "opus", bitrate = 160000, mimeType = "audio/webm"),
            AudioFormatCandidate(itag = 139, container = "m4a", codec = "mp4a.40.2", bitrate = 48000, mimeType = "audio/mp4")
        )
        val selected139 = StreamResolver.selectBestAudioFormat(candidatesMissing140)
        assertNotNull(selected139)
        assertEquals(139, selected139.itag, "Must fallback to itag 139 over WebM when 140 is absent")

        // Priority 3: Fallback to non-standard MP4/AAC candidates when 140/139 are both absent
        val candidatesNonStandardMp4 = listOf(
            AudioFormatCandidate(itag = 251, container = "webm", codec = "opus", bitrate = 160000, mimeType = "audio/webm"),
            AudioFormatCandidate(itag = 256, container = "m4a", codec = "mp4a.40.2", bitrate = 192000, mimeType = "audio/mp4"),
            AudioFormatCandidate(itag = 258, container = "m4a", codec = "mp4a.40.2", bitrate = 384000, mimeType = "audio/mp4")
        )
        val selectedNonStandard = StreamResolver.selectBestAudioFormat(candidatesNonStandardMp4)
        assertNotNull(selectedNonStandard)
        assertEquals(258, selectedNonStandard.itag, "Must prefer highest bitrate non-standard MP4 format")

        // Priority 4: Pure WebM fallback when zero MP4/AAC candidates exist
        val candidatesWebmOnly = listOf(
            AudioFormatCandidate(itag = 249, container = "webm", codec = "opus", bitrate = 50000, mimeType = "audio/webm"),
            AudioFormatCandidate(itag = 250, container = "webm", codec = "opus", bitrate = 70000, mimeType = "audio/webm"),
            AudioFormatCandidate(itag = 251, container = "webm", codec = "opus", bitrate = 160000, mimeType = "audio/webm")
        )
        val selectedWebm = StreamResolver.selectBestAudioFormat(candidatesWebmOnly)
        assertNotNull(selectedWebm)
        assertEquals(251, selectedWebm.itag, "Must select highest bitrate WebM candidate")

        // Degraded candidate lists: Empty list, zero bitrate, negative bitrate
        assertNull(StreamResolver.selectBestAudioFormat(emptyList()))

        val zeroBitrates = listOf(
            AudioFormatCandidate(itag = 901, container = "webm", codec = "opus", bitrate = -50, mimeType = "audio/webm"),
            AudioFormatCandidate(itag = 902, container = "webm", codec = "opus", bitrate = 0, mimeType = "audio/webm"),
            AudioFormatCandidate(itag = 903, container = "webm", codec = "opus", bitrate = 100, mimeType = "audio/webm")
        )
        assertEquals(903, StreamResolver.selectBestAudioFormat(zeroBitrates)?.itag)

        // Case-insensitive MIME matching
        val caseCandidates = listOf(
            AudioFormatCandidate(itag = 801, container = "webm", codec = "opus", bitrate = 200000, mimeType = "AUDIO/WEBM"),
            AudioFormatCandidate(itag = 802, container = "m4a", codec = "mp4a.40.2", bitrate = 128000, mimeType = "AUDIO/MP4")
        )
        assertEquals(802, StreamResolver.selectBestAudioFormat(caseCandidates)?.itag)
    }

    @Test
    fun testStreamResolverProfilesAndUrlExpirationStress() {
        val profiles = listOf("ANDROID_VR_NO_AUTH", "ANDROID_VR_1_61_48", "IPADOS", "IOS")
        for (profile in profiles) {
            val stream = StreamResolver.resolveStream("track_t5_$profile", clientProfileName = profile)
            assertEquals("track_t5_$profile", stream.videoId)
            assertEquals(140, stream.itag)
            assertEquals("audio/mp4", stream.mimeType)
            assertEquals(128000, stream.bitrate)
            assertTrue(stream.streamUrl.contains("sn-ab5sznzs.googlevideo.com"))
            assertTrue(stream.streamUrl.contains("c=$profile"))
            assertTrue(stream.streamUrl.contains("&pot="))
            assertTrue(stream.headers.containsKey("User-Agent"))
            assertFalse(stream.isExpired(1000L))
        }

        // Unknown profile throws IllegalArgumentException
        assertFailsWith<IllegalArgumentException> {
            StreamResolver.resolveStream("track_invalid", clientProfileName = "UNKNOWN_PROFILE")
        }

        // isExpired boundary checks
        val now = 1775000000L
        assertTrue(StreamResolver.isExpired("https://googlevideo.com/playback?expire=$now", now))
        assertTrue(StreamResolver.isExpired("https://googlevideo.com/playback?expire=${now - 1}", now))
        assertFalse(StreamResolver.isExpired("https://googlevideo.com/playback?expire=${now + 1}", now))
        assertTrue(StreamResolver.isExpired("https://googlevideo.com/playback?id=123", now))
        assertTrue(StreamResolver.isExpired("https://googlevideo.com/playback?expire=INVALID", now))
        assertTrue(StreamResolver.isExpired("https://googlevideo.com/playback?expire=", now))
    }

    // =========================================================================
    // 3. MULTI-PROVIDER LYRICS FAILURE CASCADES & EMPTY LYRICS FALLBACKS
    // =========================================================================

    @Test
    fun testMultiProviderLyricsFailureCascadeAndFallback() = runTest {
        val service = LyricsService()

        // 1. Request lyrics for non-existent track -> Triggers full provider cascade fallback
        val unknownTitle = "ZzNonExistentTrack_Tier5_${Random.nextInt(1000000)}"
        val unknownArtist = "ZzNonExistentArtist_Tier5_${Random.nextInt(1000000)}"
        val result = service.getLyrics(
            videoId = "cascading_fail_vid",
            title = unknownTitle,
            artist = unknownArtist,
            durationSec = 230
        )

        assertTrue(result.isSuccess, "LyricsService must gracefully recover via fallback")
        val lyrics = result.getOrNull()
        assertNotNull(lyrics)
        assertEquals("cascading_fail_vid", lyrics.videoId)
        assertEquals("Synthesized", lyrics.source)
        assertTrue(lyrics.isSynced, "Synthesized fallback lyrics must have synchronized timestamps")
        assertTrue(lyrics.syncedLyrics.size >= 6, "Must generate at least 6 structured lyric lines")
        assertTrue(lyrics.plainText?.contains("$unknownTitle - $unknownArtist") == true)

        // Verify in-memory cache hit: Second call returns the exact same cached instance
        val cachedResult = service.getLyrics(
            videoId = "cascading_fail_vid",
            title = unknownTitle,
            artist = unknownArtist,
            durationSec = 230
        )
        assertEquals(lyrics, cachedResult.getOrNull(), "Subsequent call must return cached lyrics")

        // Multiple distinct IDs each receive distinct cached fallback lyrics
        for (i in 1..5) {
            val res = service.getLyrics("unique_vid_$i", "Song $i", "Artist $i", 150)
            assertTrue(res.isSuccess)
            assertEquals("unique_vid_$i", res.getOrNull()?.videoId)
            assertTrue(res.getOrNull()?.plainText?.contains("Song $i - Artist $i") == true)
        }
    }

    @Test
    fun testLrcParserAdversarialAndMalformedInputs() {
        // Missing brackets, non-numeric timestamps, empty text
        val malformedLrc = """
            00:10.00 Missing open bracket
            [00:15.00 Missing close bracket
            [invalid:time] Corrupt timecode
            [00:20.50]
            [00:25.123]   Valid Line With Subsecond Millis   
            [99:59.99]Very late line
        """.trimIndent()

        val parsed = LrcParser.parse(malformedLrc)
        assertTrue(parsed.isSynced)
        assertEquals(3, parsed.lines.size)

        // Line 1: empty text at 20500ms
        assertEquals(20500L, parsed.lines[0].timestampMs)
        assertEquals("", parsed.lines[0].text)

        // Line 2: 25123ms with 3-digit millisecond precision
        assertEquals(25123L, parsed.lines[1].timestampMs)
        assertEquals("Valid Line With Subsecond Millis", parsed.lines[1].text)

        // Line 3: 5999990ms
        assertEquals(5999990L, parsed.lines[2].timestampMs)
        assertEquals("Very late line", parsed.lines[2].text)

        // Binary search findActiveLineIndex boundary testing
        assertEquals(-1, LrcParser.findActiveLineIndex(parsed.lines, -5000L))
        assertEquals(-1, LrcParser.findActiveLineIndex(parsed.lines, 0L))
        assertEquals(-1, LrcParser.findActiveLineIndex(parsed.lines, 20499L))
        assertEquals(0, LrcParser.findActiveLineIndex(parsed.lines, 20500L))
        assertEquals(0, LrcParser.findActiveLineIndex(parsed.lines, 25122L))
        assertEquals(1, LrcParser.findActiveLineIndex(parsed.lines, 25123L))
        assertEquals(2, LrcParser.findActiveLineIndex(parsed.lines, 5999990L))
        assertEquals(2, LrcParser.findActiveLineIndex(parsed.lines, Long.MAX_VALUE))
    }

    // =========================================================================
    // 4. QUEUEMANAGER BOUNDARY CONDITIONS & MASSIVE QUEUE SHUFFLE RESTORE
    // =========================================================================

    @Test
    fun testQueueManagerMassiveShufflingAndNonDestructiveRestore() {
        val qm = QueueManager()
        val totalCount = 1000
        val items = (0 until totalCount).map { createSong("track_$it", 120) }

        // Start queue at arbitrary index 450
        val targetIndex = 450
        val targetItem = items[targetIndex]
        qm.setQueue(items, startIndex = targetIndex)

        assertEquals(totalCount, qm.queue.size)
        assertEquals(totalCount, qm.originalQueue.size)
        assertEquals(targetIndex, qm.currentIndex)
        assertEquals(targetItem.id, qm.currentItem?.id)

        // 1. Enable shuffle mode
        qm.setShuffle(true)
        assertTrue(qm.shuffleMode)
        assertEquals(totalCount, qm.queue.size)
        assertEquals(totalCount, qm.originalQueue.size)

        // Invariant: Currently active item is pinned at index 0
        assertEquals(0, qm.currentIndex, "Shuffled queue must place currently playing item at index 0")
        assertEquals(targetItem.id, qm.currentItem?.id, "Active item must be strictly preserved")

        // Invariant: All 1,000 items are present (no drops or duplicates)
        val shuffledIds = qm.queue.map { it.id }.toSet()
        assertEquals(totalCount, shuffledIds.size, "All 1000 items must be present in shuffled queue")

        // Invariant: Original queue order is completely unmodified
        for (i in 0 until totalCount) {
            assertEquals(items[i].id, qm.originalQueue[i].id, "Original queue order must not be altered")
        }

        // 2. Disable shuffle mode -> Non-destructive restore
        qm.setShuffle(false)
        assertFalse(qm.shuffleMode)
        assertEquals(totalCount, qm.queue.size)

        // Invariant: Exact original order restored
        for (i in 0 until totalCount) {
            assertEquals(items[i].id, qm.queue[i].id, "Queue must be 100% restored to original order at index $i")
        }

        // Invariant: Current index restored to 450 and current item is still target item
        assertEquals(targetIndex, qm.currentIndex, "Current index must be restored to original index")
        assertEquals(targetItem.id, qm.currentItem?.id, "Active item must match original target item")
    }

    @Test
    fun testQueueManagerRapidDeletionsOfCurrentItem() {
        val qm = QueueManager()
        val count = 10
        val songs = (0 until count).map { createSong("del_track_$it") }

        qm.setQueue(songs, startIndex = 0)
        assertEquals(0, qm.currentIndex)
        assertEquals("del_track_0", qm.currentItem?.id)

        // Rapidly delete current item at index 0 until queue is empty
        for (i in 0 until count) {
            val expectedRemaining = count - i
            assertEquals(expectedRemaining, qm.queue.size)
            assertEquals("del_track_$i", qm.currentItem?.id)

            val removed = qm.remove(0)
            assertTrue(removed, "Removal of current item must succeed")

            if (i < count - 1) {
                assertEquals(0, qm.currentIndex, "Active index remains 0 pointing to next item")
                assertEquals("del_track_${i + 1}", qm.currentItem?.id)
            } else {
                // Queue is now empty
                assertEquals(0, qm.queue.size)
                assertEquals(-1, qm.currentIndex)
                assertNull(qm.currentItem)
            }
        }

        // Boundary operations on empty queue
        assertFalse(qm.remove(0))
        assertFalse(qm.remove(-1))
        assertFalse(qm.reorder(0, 1))
        assertNull(qm.next())
        assertNull(qm.previous())
        assertNull(qm.seekToIndex(0))
    }

    @Test
    fun testQueueManagerRepeatModesExhaustiveTransitions() {
        val qm = QueueManager()
        val songs = listOf(createSong("song_A"), createSong("song_B"))
        qm.setQueue(songs, 0)

        // RepeatMode.OFF: stops at end
        qm.setRepeat(RepeatMode.OFF)
        assertEquals("song_B", qm.next()?.id)
        assertNull(qm.next(), "At end of queue, next() must return null in RepeatMode.OFF")

        // RepeatMode.ALL: wraps around to 0
        qm.setRepeat(RepeatMode.ALL)
        assertEquals("song_A", qm.next()?.id)
        assertEquals("song_B", qm.next()?.id)
        assertEquals("song_A", qm.next()?.id)

        // RepeatMode.ONE: repeats same track
        qm.setRepeat(RepeatMode.ONE)
        assertEquals("song_A", qm.next()?.id)
        assertEquals("song_A", qm.next()?.id)
        assertEquals(0, qm.currentIndex)
    }

    // =========================================================================
    // 5. PLAYBACKCONTROLLER STATEFLOW CONCURRENT TRANSITIONS & STRESS
    // =========================================================================

    @Test
    fun testPlaybackControllerStateFlowTransitionsUnderConcurrentCommands() = runTest {
        val player = DesktopAudioPlayer()
        val qm = QueueManager()
        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            queueManager = qm,
            scope = this
        )

        val songs = (0 until 30).map { createSong("concurrent_track_$it", durationSec = 100) }
        controller.playQueue(songs, startIndex = 0)
        advanceUntilIdle()

        assertEquals(PlaybackState.READY, controller.playbackState.value)
        assertTrue(controller.isPlaying.value)

        // Concurrently launch 100 rapid commands across coroutines
        val random = Random(42)
        val jobs = (0 until 100).map { i ->
            launch {
                when (random.nextInt(8)) {
                    0 -> controller.skipNext()
                    1 -> controller.skipPrevious()
                    2 -> controller.seekTo(random.nextLong(-1000L, 120000L))
                    3 -> controller.pause()
                    4 -> controller.resume()
                    5 -> controller.setShuffle(random.nextBoolean())
                    6 -> controller.setRepeat(RepeatMode.entries[random.nextInt(RepeatMode.entries.size)])
                    7 -> {
                        val qSize = controller.queue.value.size
                        if (qSize > 2) {
                            val from = random.nextInt(qSize)
                            val to = random.nextInt(qSize)
                            controller.reorderQueue(from, to)
                        }
                    }
                }
            }
        }

        // Wait for all commands to dispatch
        jobs.forEach { it.join() }
        advanceUntilIdle()

        // StateFlow Invariants:
        val state = controller.playbackState.value
        val currentIdx = controller.currentIndex.value
        val queueSize = controller.queue.value.size

        assertTrue(queueSize > 0, "Queue must not be corrupted")
        if (state == PlaybackState.READY) {
            assertTrue(currentIdx in 0 until queueSize, "Current index $currentIdx out of bounds")
            assertNotNull(controller.currentItem.value)
        }

        // Seek position must always remain clamped
        val pos = controller.currentPositionMs.value
        val dur = controller.durationMs.value
        assertTrue(pos in 0L..maxOf(0L, dur), "Position $pos out of bounds for dur $dur")

        // Teardown cleanly
        controller.release()
    }

    @Test
    fun testPlaybackControllerInFlightResolutionCancellation() = runTest {
        val player = DesktopAudioPlayer()
        val cancelledResolutions = mutableListOf<String>()
        val completedResolutions = mutableListOf<String>()

        val artificialResolver: suspend (String) -> Pair<String, Map<String, String>> = { videoId ->
            try {
                delay(100L)
                completedResolutions.add(videoId)
                Pair("https://stream.mock/$videoId.aac", emptyMap())
            } catch (e: CancellationException) {
                cancelledResolutions.add(videoId)
                throw e
            }
        }

        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            scope = this,
            streamResolverFn = artificialResolver
        )

        val songs = (0 until 10).map { createSong("cancel_test_$it") }
        controller.playQueue(songs, 0)
        testScheduler.runCurrent()

        // Rapidly skip 5 times before resolution can complete
        for (i in 1..5) {
            controller.skipNext()
            testScheduler.runCurrent()
        }

        advanceUntilIdle()

        // All intermediate resolutions (0..4) must be cancelled
        assertEquals(5, cancelledResolutions.size, "Intermediate in-flight resolutions must be cancelled")
        assertEquals(1, completedResolutions.size, "Only the target track resolution must complete")
        assertEquals("cancel_test_5", completedResolutions.first())
        assertEquals(5, controller.currentIndex.value)
        assertEquals("cancel_test_5", controller.currentItem.value?.id)

        controller.release()
    }

    // =========================================================================
    // 6. THEME DYNAMIC LUMINANCE CLAMPING & HIGH-CONTRAST SQUIRCLE BOUNDARIES
    // =========================================================================

    @Test
    fun testThemeDynamicLuminanceClampingAndContrast() {
        // Verify relative luminance for all 5 SuvMusic palettes
        for (palette in SuvMusicPalette.entries) {
            val colors = getPaletteColors(palette)
            val darkScheme = buildColorScheme(palette, darkTheme = true, pureBlack = false)
            val amoledScheme = buildColorScheme(palette, darkTheme = true, pureBlack = true)
            val lightScheme = buildColorScheme(palette, darkTheme = false, pureBlack = false)

            // Invariant 1: AMOLED surface and background must have exactly 0.0f luminance (Pure Black #000000)
            assertEquals(Color.Black, amoledScheme.background)
            assertEquals(Color.Black, amoledScheme.surface)
            assertEquals(0.0f, amoledScheme.background.luminance(), 0.001f)
            assertEquals(0.0f, amoledScheme.surface.luminance(), 0.001f)

            // Invariant 2: Light theme surface luminance must be > 0.8f
            assertTrue(lightScheme.background.luminance() > 0.8f, "Light background luminance must be high")
            assertTrue(lightScheme.surface.luminance() > 0.8f, "Light surface luminance must be high")

            // Invariant 3: High-contrast WCAG ratio verification: (L1 + 0.05) / (L2 + 0.05)
            // On dark theme, onBackground / onSurface must have contrast ratio >= 4.5:1 against background
            val darkBgLum = darkScheme.background.luminance()
            val darkFgLum = darkScheme.onBackground.luminance()
            val darkContrastRatio = (maxOf(darkBgLum, darkFgLum) + 0.05f) / (minOf(darkBgLum, darkFgLum) + 0.05f)
            assertTrue(
                darkContrastRatio >= 4.5f,
                "Contrast ratio $darkContrastRatio for ${palette.name} dark mode must satisfy WCAG AA >= 4.5"
            )

            // On AMOLED, contrast against pure black is maximal (> 10:1)
            val amoledBgLum = amoledScheme.background.luminance()
            val amoledFgLum = amoledScheme.onBackground.luminance()
            val amoledContrastRatio = (maxOf(amoledBgLum, amoledFgLum) + 0.05f) / (minOf(amoledBgLum, amoledFgLum) + 0.05f)
            assertTrue(
                amoledContrastRatio >= 10.0f,
                "Contrast ratio $amoledContrastRatio for ${palette.name} AMOLED mode must satisfy >= 10.0"
            )

            // Invariant 4: Primary accent color luminance is preserved between regular dark and AMOLED
            assertEquals(
                darkScheme.primary.luminance(),
                amoledScheme.primary.luminance(),
                0.0001f,
                "Primary luminance must not change when toggling pure black"
            )
        }
    }

    @Test
    fun testSquircleHighContrastBoundariesAndExtremeCurvatures() {
        // Zero dimensions must produce valid Outline.Rectangle without throwing
        val squircle = SquircleShape(cornerRadius = 24.dp, cornerSmoothing = 0.6f)
        val outlineZero = squircle.createOutline(Size(0f, 0f), LayoutDirection.Ltr, testDensity)
        assertTrue(outlineZero is Outline.Rectangle)

        val outlineZeroWidth = squircle.createOutline(Size(0f, 200f), LayoutDirection.Ltr, testDensity)
        assertTrue(outlineZeroWidth is Outline.Rectangle)

        val outlineZeroHeight = squircle.createOutline(Size(200f, 0f), LayoutDirection.Ltr, testDensity)
        assertTrue(outlineZeroHeight is Outline.Rectangle)

        // Sub-pixel dimensions (1px by 1px)
        val outlineSubPixel = squircle.createOutline(Size(1f, 1f), LayoutDirection.Ltr, testDensity)
        assertTrue(outlineSubPixel is Outline.Generic)

        // Extreme aspect ratios (10000px by 1px and 1px by 10000px)
        val outlineExtremeWidth = squircle.createOutline(Size(10000f, 1f), LayoutDirection.Ltr, testDensity)
        assertTrue(outlineExtremeWidth is Outline.Generic)

        val outlineExtremeHeight = squircle.createOutline(Size(1f, 10000f), LayoutDirection.Ltr, testDensity)
        assertTrue(outlineExtremeHeight is Outline.Generic)

        // Smoothing factor boundary clamping: [-100f clamped to 0f, 100f clamped to 1f]
        val clampedMin = SquircleShape(cornerRadius = 20.dp, cornerSmoothing = -100f)
        assertEquals(0.0f, clampedMin.smoothing)
        val outlineMin = clampedMin.createOutline(Size(100f, 100f), LayoutDirection.Ltr, testDensity)
        assertTrue(outlineMin is Outline.Rounded)

        val clampedMax = SquircleShape(cornerRadius = 20.dp, cornerSmoothing = 100f)
        assertEquals(1.0f, clampedMax.smoothing)
        val outlineMax = clampedMax.createOutline(Size(100f, 100f), LayoutDirection.Ltr, testDensity)
        assertTrue(outlineMax is Outline.Generic)

        // Enormous corner radius (100,000 dp) clamped to min(width, height) / 2
        val giantSquircle = SquircleShape(cornerRadius = 100000.dp, cornerSmoothing = 0.6f)
        val outlineGiant = giantSquircle.createOutline(Size(200f, 200f), LayoutDirection.Ltr, testDensity)
        assertTrue(outlineGiant is Outline.Generic)

        // Diverse display densities (0.25f, 1.0f, 2.75f, 4.0f)
        val densities = listOf(Density(0.25f), Density(1.0f), Density(2.75f), Density(4.0f))
        for (d in densities) {
            val outlineD = squircle.createOutline(Size(150f, 150f), LayoutDirection.Ltr, d)
            assertTrue(outlineD is Outline.Generic)
        }

        // Layout directions: LTR and RTL both generate valid outlines
        val ltrOutline = squircle.createOutline(Size(100f, 100f), LayoutDirection.Ltr, testDensity)
        val rtlOutline = squircle.createOutline(Size(100f, 100f), LayoutDirection.Rtl, testDensity)
        assertTrue(ltrOutline is Outline.Generic)
        assertTrue(rtlOutline is Outline.Generic)
    }
}
