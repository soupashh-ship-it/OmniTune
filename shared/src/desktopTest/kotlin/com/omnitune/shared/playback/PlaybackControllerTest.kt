package com.omnitune.shared.playback

import com.omnitune.shared.domain.models.SongItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackControllerTest {

    private fun createSong(id: String, title: String, durationSec: Int = 180): SongItem {
        return SongItem(
            id = id,
            title = title,
            artist = "Artist $id",
            album = "Album $id",
            duration = durationSec,
            thumbnailUrl = "https://thumb/$id.jpg"
        )
    }

    @Test
    fun testInitialState() {
        val testDispatcher = StandardTestDispatcher()
        val testScope = TestScope(testDispatcher)
        val player = DesktopAudioPlayer()
        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            scope = testScope
        )

        assertEquals(PlaybackState.IDLE, controller.playbackState.value)
        assertNull(controller.currentItem.value)
        assertEquals(0L, controller.currentPositionMs.value)
        assertEquals(0L, controller.durationMs.value)
        assertTrue(controller.queue.value.isEmpty())
        assertEquals(-1, controller.currentIndex.value)
        assertFalse(controller.isPlaying.value)
        assertFalse(controller.shuffleMode.value)
        assertEquals(RepeatMode.OFF, controller.repeatMode.value)
    }

    @Test
    fun testPlaySingleTrack() = runTest {
        val player = DesktopAudioPlayer()
        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            scope = this
        )
        val song = createSong("s1", "Song 1", durationSec = 200)

        controller.play(song)
        advanceUntilIdle()

        assertEquals("s1", controller.currentItem.value?.id)
        assertEquals(1, controller.queue.value.size)
        assertEquals(0, controller.currentIndex.value)
        assertTrue(controller.isPlaying.value)
        assertEquals(PlaybackState.READY, controller.playbackState.value)
        assertEquals(200000L, controller.durationMs.value)
        assertTrue(player.isPlaying)
    }

    @Test
    fun testPlayQueueWithStartIndex() = runTest {
        val player = DesktopAudioPlayer()
        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            scope = this
        )
        val songs = listOf(
            createSong("s1", "Song 1", 100),
            createSong("s2", "Song 2", 150),
            createSong("s3", "Song 3", 200)
        )

        controller.playQueue(songs, 1)
        advanceUntilIdle()

        assertEquals("s2", controller.currentItem.value?.id)
        assertEquals(3, controller.queue.value.size)
        assertEquals(1, controller.currentIndex.value)
        assertTrue(controller.isPlaying.value)
        assertEquals(PlaybackState.READY, controller.playbackState.value)
        assertEquals(150000L, controller.durationMs.value)
    }

    @Test
    fun testPauseAndResume() = runTest {
        val player = DesktopAudioPlayer()
        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            scope = this
        )
        controller.play(createSong("s1", "Song 1", 100))
        advanceUntilIdle()

        assertTrue(controller.isPlaying.value)

        controller.pause()
        assertFalse(controller.isPlaying.value)
        assertFalse(player.isPlaying)

        controller.resume()
        assertTrue(controller.isPlaying.value)
        assertTrue(player.isPlaying)
    }

    @Test
    fun testSeekClamping() = runTest {
        val player = DesktopAudioPlayer()
        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            scope = this
        )
        controller.play(createSong("s1", "Song 1", 100)) // 100_000ms duration
        advanceUntilIdle()

        controller.seekTo(50000L)
        assertEquals(50000L, controller.currentPositionMs.value)
        assertEquals(50000L, player.currentPositionMs)

        // Negative clamp to 0
        controller.seekTo(-500L)
        assertEquals(0L, controller.currentPositionMs.value)
        assertEquals(0L, player.currentPositionMs)

        // Over-duration clamp to durationMs
        controller.seekTo(150000L)
        assertEquals(100000L, controller.currentPositionMs.value)
        assertEquals(100000L, player.currentPositionMs)
    }

    @Test
    fun testSeekOnEmptyQueueClampsToZero() {
        val player = DesktopAudioPlayer()
        val controller = PlaybackControllerImpl(audioPlayer = player)

        controller.seekTo(10000L)
        assertEquals(0L, controller.currentPositionMs.value)
    }

    @Test
    fun testSkipNextAndQueueProgression() = runTest {
        val player = DesktopAudioPlayer()
        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            scope = this
        )
        val songs = listOf(
            createSong("s1", "Song 1", 100),
            createSong("s2", "Song 2", 120)
        )
        controller.playQueue(songs, 0)
        advanceUntilIdle()

        assertEquals("s1", controller.currentItem.value?.id)

        controller.skipNext()
        advanceUntilIdle()

        assertEquals("s2", controller.currentItem.value?.id)
        assertEquals(1, controller.currentIndex.value)
        assertTrue(controller.isPlaying.value)

        // At end of queue with Repeat OFF
        controller.skipNext()
        advanceUntilIdle()

        assertFalse(controller.isPlaying.value)
        assertEquals(PlaybackState.ENDED, controller.playbackState.value)
    }

    @Test
    fun testAutomaticProgressionOnTrackCompletion() = runTest {
        val player = DesktopAudioPlayer()
        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            scope = this
        )
        val songs = listOf(
            createSong("s1", "Song 1", 100),
            createSong("s2", "Song 2", 120)
        )
        controller.playQueue(songs, 0)
        advanceUntilIdle()

        assertEquals("s1", controller.currentItem.value?.id)

        // Player completes track
        player.simulateTrackCompletion()
        advanceUntilIdle()

        assertEquals("s2", controller.currentItem.value?.id)
        assertEquals(1, controller.currentIndex.value)
        assertTrue(controller.isPlaying.value)
    }

    @Test
    fun testRepeatOneReplaysCurrentTrack() = runTest {
        val player = DesktopAudioPlayer()
        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            scope = this
        )
        controller.playQueue(listOf(createSong("s1", "Song 1", 100)), 0)
        advanceUntilIdle()

        controller.setRepeat(RepeatMode.ONE)
        assertEquals(RepeatMode.ONE, controller.repeatMode.value)

        controller.seekTo(30000L)
        assertEquals(30000L, controller.currentPositionMs.value)

        controller.skipNext()
        advanceUntilIdle()

        assertEquals("s1", controller.currentItem.value?.id)
        assertEquals(0L, controller.currentPositionMs.value)
        assertTrue(controller.isPlaying.value)
    }

    @Test
    fun testShuffleModeStateFlow() = runTest {
        val player = DesktopAudioPlayer()
        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            scope = this
        )
        val songs = (1..5).map { createSong("s$it", "Song $it") }
        controller.playQueue(songs, 2)
        advanceUntilIdle()

        assertEquals("s3", controller.currentItem.value?.id)

        controller.setShuffle(true)
        assertTrue(controller.shuffleMode.value)
        assertEquals("s3", controller.currentItem.value?.id)

        controller.setShuffle(false)
        assertFalse(controller.shuffleMode.value)
        assertEquals("s3", controller.currentItem.value?.id)
        assertEquals(2, controller.currentIndex.value)
    }

    @Test
    fun testReorderQueueUpdatesStateFlow() = runTest {
        val player = DesktopAudioPlayer()
        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            scope = this
        )
        val songs = listOf(
            createSong("s1", "Song 1"),
            createSong("s2", "Song 2"),
            createSong("s3", "Song 3")
        )
        controller.playQueue(songs, 0)
        advanceUntilIdle()

        controller.reorderQueue(0, 2)
        assertEquals("s1", controller.queue.value[2].id)
        assertEquals(2, controller.currentIndex.value)
        assertEquals("s1", controller.currentItem.value?.id)
    }

    @Test
    fun testRemoveFromQueueUpdatesStateFlow() = runTest {
        val player = DesktopAudioPlayer()
        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            scope = this
        )
        val songs = listOf(
            createSong("s1", "Song 1"),
            createSong("s2", "Song 2")
        )
        controller.playQueue(songs, 0)
        advanceUntilIdle()

        controller.removeFromQueue(1)
        assertEquals(1, controller.queue.value.size)
        assertEquals("s1", controller.currentItem.value?.id)

        controller.removeFromQueue(0)
        assertTrue(controller.queue.value.isEmpty())
        assertNull(controller.currentItem.value)
        assertEquals(-1, controller.currentIndex.value)
        assertFalse(controller.isPlaying.value)
        assertEquals(PlaybackState.IDLE, controller.playbackState.value)
    }

    @Test
    fun testSkipPreviousQueueBehaviorAndStateSynchronization() = runTest {
        val player = DesktopAudioPlayer()
        val qm = QueueManager()
        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            queueManager = qm,
            scope = this
        )
        val songs = listOf(
            createSong("s0", "Song 0", 100),
            createSong("s1", "Song 1", 100),
            createSong("s2", "Song 2", 100)
        )

        // 1. Play starting at index 1 (s1)
        controller.playQueue(songs, 1)
        advanceUntilIdle()
        assertEquals(1, controller.currentIndex.value)
        assertEquals("s1", controller.currentItem.value?.id)

        // 2. Position > 3000ms: restarts s1 at 0ms, does not decrement index
        controller.seekTo(4500L)
        assertEquals(4500L, controller.currentPositionMs.value)
        controller.skipPrevious()
        advanceUntilIdle()

        assertEquals(1, controller.currentIndex.value)
        assertEquals(1, qm.currentIndex)
        assertEquals("s1", controller.currentItem.value?.id)
        assertEquals(0L, controller.currentPositionMs.value)

        // 3. Position <= 3000ms: navigates to previous track (s0 at index 0)
        controller.seekTo(1200L)
        assertEquals(1200L, controller.currentPositionMs.value)
        controller.skipPrevious()
        advanceUntilIdle()

        assertEquals(0, controller.currentIndex.value)
        assertEquals(0, qm.currentIndex)
        assertEquals("s0", controller.currentItem.value?.id)
        assertEquals(0L, controller.currentPositionMs.value)
        assertTrue(controller.isPlaying.value)

        // 4. At index 0 with RepeatMode.OFF: restarts s0 at 0ms
        controller.skipPrevious()
        advanceUntilIdle()

        assertEquals(0, controller.currentIndex.value)
        assertEquals(0, qm.currentIndex)
        assertEquals("s0", controller.currentItem.value?.id)
        assertEquals(0L, controller.currentPositionMs.value)

        // 5. At index 0 with RepeatMode.ALL: wraps to index 2 (s2)
        controller.setRepeat(RepeatMode.ALL)
        controller.skipPrevious()
        advanceUntilIdle()

        assertEquals(2, controller.currentIndex.value)
        assertEquals(2, qm.currentIndex)
        assertEquals("s2", controller.currentItem.value?.id)
        assertEquals(0L, controller.currentPositionMs.value)
        assertTrue(controller.isPlaying.value)

        // 6. At index 2 with RepeatMode.ONE: stays at s2 and restarts at 0ms
        controller.setRepeat(RepeatMode.ONE)
        controller.seekTo(1000L)
        controller.skipPrevious()
        advanceUntilIdle()

        assertEquals(2, controller.currentIndex.value)
        assertEquals(2, qm.currentIndex)
        assertEquals("s2", controller.currentItem.value?.id)
        assertEquals(0L, controller.currentPositionMs.value)
    }

    @Test
    fun testRapidSkipsCancelInFlightStreamResolution() = runTest {
        val player = DesktopAudioPlayer()
        var cancelledCount = 0
        var completedCount = 0

        val slowStreamResolver: suspend (String) -> Pair<String, Map<String, String>> = { videoId ->
            try {
                delay(200L)
                completedCount++
                Pair("https://stream.mock/$videoId.aac", emptyMap())
            } catch (e: CancellationException) {
                cancelledCount++
                throw e
            }
        }

        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            scope = this,
            streamResolverFn = slowStreamResolver
        )

        val songs = listOf(
            createSong("s0", "Song 0"),
            createSong("s1", "Song 1"),
            createSong("s2", "Song 2")
        )

        // Rapidly enqueue and skip while jobs are in-flight
        controller.playQueue(songs, 0)
        testScheduler.runCurrent()
        controller.skipNext()
        testScheduler.runCurrent()
        controller.skipNext()
        testScheduler.runCurrent()

        // Fast-forward coroutines
        advanceUntilIdle()

        // The first 2 resolutions (s0 and s1) must be cancelled, only s2 completes
        assertEquals(2, cancelledCount, "Previous in-flight stream resolutions must be cancelled")
        assertEquals(1, completedCount, "Only the final target track should complete stream resolution")
        assertEquals("s2", controller.currentItem.value?.id)
        assertEquals(2, controller.currentIndex.value)
        assertTrue(controller.isPlaying.value)
        assertEquals(PlaybackState.READY, controller.playbackState.value)
    }

    @Test
    fun testSeekEventsFlowEmission() = runTest {
        val player = DesktopAudioPlayer()
        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            scope = this
        )
        val song = createSong("s1", "Song 1", 200)
        controller.play(song)
        advanceUntilIdle()

        val emittedSeeks = mutableListOf<Long>()
        val job = launch {
            controller.seekEvents.collect { emittedSeeks.add(it) }
        }
        testScheduler.runCurrent()

        controller.seekTo(30000L)
        controller.seekTo(60000L)
        advanceUntilIdle()

        job.cancel()
        assertEquals(listOf(30000L, 60000L), emittedSeeks)
    }
}
