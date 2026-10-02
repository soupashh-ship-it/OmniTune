package com.omnitune.shared.playback

import com.omnitune.shared.domain.models.SongItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class M3AdversarialVerificationTest {

    private fun createSong(id: String, durationSec: Int = 180): SongItem {
        return SongItem(
            id = id,
            title = "Title $id",
            artist = "Artist $id",
            album = "Album $id",
            duration = durationSec,
            thumbnailUrl = "https://thumb/$id.jpg"
        )
    }

    // =========================================================================
    // 1. SKIP PREVIOUS & QUEUE STATE SYNCHRONIZATION ADVERSARIAL MATRIX
    // =========================================================================

    @Test
    fun testSkipPreviousBoundaryMatrix() = runTest {
        val player = DesktopAudioPlayer()
        val qm = QueueManager()
        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            queueManager = qm,
            scope = this
        )
        val songs = listOf(
            createSong("s0", 100),
            createSong("s1", 100),
            createSong("s2", 100)
        )

        // Case A: Exact threshold boundary checks (2999ms, 3000ms, 3001ms)
        controller.playQueue(songs, startIndex = 1)
        advanceUntilIdle()

        // 3001ms (> 3000ms threshold) -> restarts track s1 in-place at 0ms
        controller.seekTo(3001L)
        controller.skipPrevious()
        advanceUntilIdle()
        assertEquals(1, controller.currentIndex.value)
        assertEquals(1, qm.currentIndex)
        assertEquals("s1", controller.currentItem.value?.id)
        assertEquals(0L, controller.currentPositionMs.value)

        // 3000ms (<= 3000ms threshold) -> navigates to previous track s0
        controller.seekTo(3000L)
        controller.skipPrevious()
        advanceUntilIdle()
        assertEquals(0, controller.currentIndex.value)
        assertEquals(0, qm.currentIndex)
        assertEquals("s0", controller.currentItem.value?.id)
        assertEquals(0L, controller.currentPositionMs.value)

        // Reset to track 1 with 2999ms (<= 3000ms) -> navigates to s0
        controller.playQueue(songs, startIndex = 1)
        advanceUntilIdle()
        controller.seekTo(2999L)
        controller.skipPrevious()
        advanceUntilIdle()
        assertEquals(0, controller.currentIndex.value)
        assertEquals(0, qm.currentIndex)
        assertEquals("s0", controller.currentItem.value?.id)

        // Case B: Single-item queue behavior under all repeat modes
        val singleSong = listOf(createSong("single", 120))
        controller.playQueue(singleSong, 0)
        advanceUntilIdle()

        // Repeat OFF on single song -> restart at 0ms
        controller.setRepeat(RepeatMode.OFF)
        controller.seekTo(1500L)
        controller.skipPrevious()
        advanceUntilIdle()
        assertEquals(0, controller.currentIndex.value)
        assertEquals(0, qm.currentIndex)
        assertEquals("single", controller.currentItem.value?.id)
        assertEquals(0L, controller.currentPositionMs.value)

        // Repeat ALL on single song -> restart at 0ms
        controller.setRepeat(RepeatMode.ALL)
        controller.seekTo(1500L)
        controller.skipPrevious()
        advanceUntilIdle()
        assertEquals(0, controller.currentIndex.value)
        assertEquals(0, qm.currentIndex)
        assertEquals("single", controller.currentItem.value?.id)
        assertEquals(0L, controller.currentPositionMs.value)

        // Repeat ONE on single song -> restart at 0ms
        controller.setRepeat(RepeatMode.ONE)
        controller.seekTo(1500L)
        controller.skipPrevious()
        advanceUntilIdle()
        assertEquals(0, controller.currentIndex.value)
        assertEquals(0, qm.currentIndex)
        assertEquals("single", controller.currentItem.value?.id)
        assertEquals(0L, controller.currentPositionMs.value)
    }

    // =========================================================================
    // 2. RAPID BURST SKIPS & IN-FLIGHT RESOLUTION CANCELLATION STRESS
    // =========================================================================

    @Test
    fun testRapidBurstSkipsWithRandomDelays() = runTest {
        val player = DesktopAudioPlayer()
        val cancelledResolutions = mutableListOf<String>()
        val completedResolutions = mutableListOf<String>()

        val resolvingDelayMap = mapOf(
            "burst_0" to 150L,
            "burst_1" to 300L,
            "burst_2" to 50L,
            "burst_3" to 200L,
            "burst_4" to 100L,
            "burst_5" to 400L,
            "burst_6" to 80L
        )

        val variableResolver: suspend (String) -> Pair<String, Map<String, String>> = { videoId ->
            val delayMs = resolvingDelayMap[videoId] ?: 100L
            try {
                delay(delayMs)
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
            streamResolverFn = variableResolver
        )

        val count = 7
        val songs = (0 until count).map { createSong("burst_$it") }
        controller.playQueue(songs, 0)
        testScheduler.runCurrent()

        // Rapidly skip 5 times
        for (i in 1..5) {
            controller.skipNext()
            testScheduler.runCurrent()
        }

        // Fast forward all scheduled tasks
        advanceUntilIdle()

        // Invariant: The first 5 items (0..4) were cancelled in flight
        assertEquals(5, cancelledResolutions.size, "Intermediate in-flight resolutions must be cancelled")
        assertEquals(1, completedResolutions.size, "Only the destination track must complete")
        assertEquals("burst_5", completedResolutions.first(), "Track burst_5 must be the one resolved")

        // Invariant: Active state is burst_5 at index 5
        assertEquals(5, controller.currentIndex.value)
        assertEquals("burst_5", controller.currentItem.value?.id)
        assertTrue(controller.isPlaying.value)
        assertEquals(PlaybackState.READY, controller.playbackState.value)
    }

    // =========================================================================
    // 3. EXHAUSTIVE SEEK CLAMPING & ZERO-DURATION RESILIENCE
    // =========================================================================

    @Test
    fun testExhaustiveSeekClampingAndEdgeCases() = runTest {
        val player = DesktopAudioPlayer()
        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            scope = this
        )
        val song = createSong("clamp_test", durationSec = 120) // 120,000 ms
        controller.play(song)
        advanceUntilIdle()

        // 1. Extreme negative values
        controller.seekTo(Long.MIN_VALUE)
        assertEquals(0L, controller.currentPositionMs.value)
        assertEquals(0L, player.currentPositionMs)

        controller.seekTo(-1L)
        assertEquals(0L, controller.currentPositionMs.value)
        assertEquals(0L, player.currentPositionMs)

        // 2. Exact boundaries
        controller.seekTo(0L)
        assertEquals(0L, controller.currentPositionMs.value)

        controller.seekTo(120000L)
        assertEquals(120000L, controller.currentPositionMs.value)
        assertEquals(120000L, player.currentPositionMs)

        // 3. Extreme positive values
        controller.seekTo(120001L)
        assertEquals(120000L, controller.currentPositionMs.value)

        controller.seekTo(Long.MAX_VALUE)
        assertEquals(120000L, controller.currentPositionMs.value)
        assertEquals(120000L, player.currentPositionMs)

        // 4. Zero duration song handling
        val zeroDurationSong = createSong("zero_dur", durationSec = 0)
        controller.play(zeroDurationSong)
        advanceUntilIdle()

        assertEquals(0L, controller.durationMs.value)
        controller.seekTo(5000L)
        assertEquals(0L, controller.currentPositionMs.value)
        assertEquals(0L, player.currentPositionMs)

        controller.seekTo(-5000L)
        assertEquals(0L, controller.currentPositionMs.value)
    }

    // =========================================================================
    // 4. SKIP NEXT BOUNDARY BEHAVIOR ACROSS ALL REPEAT MODES
    // =========================================================================

    @Test
    fun testSkipNextBoundaryMatrix() = runTest {
        val player = DesktopAudioPlayer()
        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            scope = this
        )
        val songs = listOf(
            createSong("track_0", 60),
            createSong("track_1", 60)
        )

        // 1. Repeat OFF: Multiple skips past end of queue
        controller.playQueue(songs, 1)
        advanceUntilIdle()

        assertEquals("track_1", controller.currentItem.value?.id)
        controller.skipNext()
        advanceUntilIdle()

        assertFalse(controller.isPlaying.value)
        assertEquals(PlaybackState.ENDED, controller.playbackState.value)
        assertEquals(1, controller.currentIndex.value)

        // Additional skipNext() while in ENDED state must remain resilient
        controller.skipNext()
        advanceUntilIdle()
        assertFalse(controller.isPlaying.value)
        assertEquals(PlaybackState.ENDED, controller.playbackState.value)

        // 2. Repeat ALL: Loops around continuously
        controller.playQueue(songs, 1)
        advanceUntilIdle()
        controller.setRepeat(RepeatMode.ALL)

        controller.skipNext()
        advanceUntilIdle()
        assertEquals(0, controller.currentIndex.value)
        assertEquals("track_0", controller.currentItem.value?.id)
        assertTrue(controller.isPlaying.value)

        controller.skipNext()
        advanceUntilIdle()
        assertEquals(1, controller.currentIndex.value)
        assertEquals("track_1", controller.currentItem.value?.id)
        assertTrue(controller.isPlaying.value)

        controller.skipNext()
        advanceUntilIdle()
        assertEquals(0, controller.currentIndex.value)
        assertEquals("track_0", controller.currentItem.value?.id)

        // 3. Repeat ONE: Replays current item with position reset
        controller.setRepeat(RepeatMode.ONE)
        controller.seekTo(45000L)
        assertEquals(45000L, controller.currentPositionMs.value)

        controller.skipNext()
        advanceUntilIdle()
        assertEquals(0, controller.currentIndex.value)
        assertEquals("track_0", controller.currentItem.value?.id)
        assertEquals(0L, controller.currentPositionMs.value)
        assertTrue(controller.isPlaying.value)
    }

    // =========================================================================
    // 5. QUEUE MUTATIONS DURING ACTIVE PLAYBACK
    // =========================================================================

    @Test
    fun testQueueMutationsWhilePlaying() = runTest {
        val player = DesktopAudioPlayer()
        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            scope = this
        )
        val songs = listOf(
            createSong("m_0", 100),
            createSong("m_1", 100),
            createSong("m_2", 100)
        )

        // Start playing track m_1
        controller.playQueue(songs, 1)
        advanceUntilIdle()
        assertEquals("m_1", controller.currentItem.value?.id)
        assertEquals(1, controller.currentIndex.value)

        // Reorder: Move m_1 (index 1) to end (index 2)
        controller.reorderQueue(1, 2)
        advanceUntilIdle()
        assertEquals(2, controller.currentIndex.value)
        assertEquals("m_1", controller.currentItem.value?.id)
        assertEquals("m_1", controller.queue.value[2].id)

        // Reorder: Move m_1 (index 2) to front (index 0)
        controller.reorderQueue(2, 0)
        advanceUntilIdle()
        assertEquals(0, controller.currentIndex.value)
        assertEquals("m_1", controller.currentItem.value?.id)
        assertEquals("m_1", controller.queue.value[0].id)

        // Remove active item m_1 (index 0)
        controller.removeFromQueue(0)
        advanceUntilIdle()
        // Next item at index 0 (m_0) should now be playing
        assertEquals(0, controller.currentIndex.value)
        assertEquals("m_0", controller.currentItem.value?.id)
        assertEquals(2, controller.queue.value.size)
        assertTrue(controller.isPlaying.value)

        // Remove all remaining items until empty
        controller.removeFromQueue(1) // Remove m_2
        advanceUntilIdle()
        assertEquals(1, controller.queue.value.size)

        controller.removeFromQueue(0) // Remove m_0
        advanceUntilIdle()
        assertEquals(0, controller.queue.value.size)
        assertEquals(-1, controller.currentIndex.value)
        assertNull(controller.currentItem.value)
        assertFalse(controller.isPlaying.value)
        assertEquals(PlaybackState.IDLE, controller.playbackState.value)
        assertEquals(0L, controller.currentPositionMs.value)
        assertEquals(0L, controller.durationMs.value)
    }

    // =========================================================================
    // 6. STREAM RESOLUTION FAILURE & RECOVERY RESILIENCE
    // =========================================================================

    @Test
    fun testStreamResolutionFailureAndRecovery() = runTest {
        val player = DesktopAudioPlayer()
        var shouldFail = true

        val faultyResolver: suspend (String) -> Pair<String, Map<String, String>> = { videoId ->
            if (shouldFail) {
                throw RuntimeException("Simulated network stream resolution failure for $videoId")
            } else {
                Pair("https://stream.mock/$videoId.aac", emptyMap())
            }
        }

        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            scope = this,
            streamResolverFn = faultyResolver
        )

        val songA = createSong("song_faulty", 150)
        val songB = createSong("song_good", 150)

        // 1. Play songA which fails resolution
        controller.play(songA)
        advanceUntilIdle()

        // Must degrade cleanly to IDLE, isPlaying=false, without crashing coroutine scope
        assertEquals(PlaybackState.IDLE, controller.playbackState.value)
        assertFalse(controller.isPlaying.value)
        assertFalse(player.isPlaying)

        // 2. Recover by playing songB with functioning resolver
        shouldFail = false
        controller.play(songB)
        advanceUntilIdle()

        assertEquals(PlaybackState.READY, controller.playbackState.value)
        assertTrue(controller.isPlaying.value)
        assertTrue(player.isPlaying)
        assertEquals("song_good", controller.currentItem.value?.id)
    }

    // =========================================================================
    // 7. AUDIO PLAYER LISTENER DISPATCH INTEGRITY
    // =========================================================================

    @Test
    fun testAudioPlayerListenerDispatches() = runTest {
        val player = DesktopAudioPlayer()
        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            scope = this
        )
        val song = createSong("listener_test", 100)
        controller.play(song)
        advanceUntilIdle()

        assertTrue(controller.isPlaying.value)
        assertEquals(PlaybackState.READY, controller.playbackState.value)

        // Simulate position discontinuity event from audio engine via seekTo
        player.seekTo(42000L)
        assertEquals(42000L, controller.currentPositionMs.value)

        // Simulate player error via helper method
        player.simulateError("Hardware decoder failure")
        assertEquals(PlaybackState.IDLE, controller.playbackState.value)
        assertFalse(controller.isPlaying.value)
    }
}
