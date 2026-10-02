package com.omnitune.shared.playback

import com.omnitune.shared.domain.models.SongItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class M3ChallengerStressTest {

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
    // 1. QUEUE MANAGER ADVERSARIAL STRESS TESTS
    // =========================================================================

    @Test
    fun testQueueManagerMassiveScaleEnqueueAndIntegrity() {
        val qm = QueueManager()
        val totalCount = 1000

        // 1. Sequential Enqueue of 1,000 items
        for (i in 0 until totalCount) {
            qm.enqueue(createSong("song_$i"))
        }

        assertEquals(totalCount, qm.queue.size)
        assertEquals(totalCount, qm.originalQueue.size)
        assertEquals(0, qm.currentIndex)
        assertEquals("song_0", qm.currentItem?.id)

        // 2. Bulk EnqueueAll of another 1,000 items
        val bulkList = (totalCount until totalCount * 2).map { createSong("song_$it") }
        qm.enqueueAll(bulkList)

        assertEquals(totalCount * 2, qm.queue.size)
        assertEquals(totalCount * 2, qm.originalQueue.size)
        assertEquals(0, qm.currentIndex)
        assertEquals("song_0", qm.currentItem?.id)
        assertEquals("song_${totalCount * 2 - 1}", qm.queue.last().id)
    }

    @Test
    fun testQueueManagerExtreme10kScalePerformance() {
        val qm = QueueManager()
        val count = 10000
        val items = (0 until count).map { createSong("track_$it") }

        val start = System.currentTimeMillis()
        qm.setQueue(items, 5000)
        val elapsed = System.currentTimeMillis() - start

        assertEquals(count, qm.queue.size)
        assertEquals(5000, qm.currentIndex)
        assertEquals("track_5000", qm.currentItem?.id)
        assertTrue(elapsed < 1000, "10k items queue initialization took too long: ${elapsed}ms")

        // Seek to last item
        val lastItem = qm.seekToIndex(count - 1)
        assertNotNull(lastItem)
        assertEquals("track_${count - 1}", lastItem.id)
        assertEquals(count - 1, qm.currentIndex)

        // Shuffle toggle on 10k items
        val shuffleStart = System.currentTimeMillis()
        qm.setShuffle(true)
        val shuffleElapsed = System.currentTimeMillis() - shuffleStart
        assertTrue(qm.shuffleMode)
        assertEquals(count, qm.queue.size)
        assertEquals("track_${count - 1}", qm.currentItem?.id)
        assertEquals(0, qm.currentIndex)
        assertTrue(shuffleElapsed < 1000, "10k shuffle took too long: ${shuffleElapsed}ms")

        // Restore shuffle
        qm.setShuffle(false)
        assertFalse(qm.shuffleMode)
        assertEquals(count, qm.queue.size)
        assertEquals("track_${count - 1}", qm.currentItem?.id)
        assertEquals(count - 1, qm.currentIndex)
    }

    @Test
    fun testQueueManagerDuplicateItemsHandling() {
        val qm = QueueManager()
        val songA = createSong("dup_A")
        val songB = createSong("dup_B")
        val songA2 = createSong("dup_A") // Identical content
        val songC = createSong("dup_C")

        qm.setQueue(listOf(songA, songB, songA2, songC), startIndex = 0)
        assertEquals(4, qm.queue.size)

        // Remove the second dup_A at index 2
        assertTrue(qm.remove(2))
        assertEquals(3, qm.queue.size)
        assertEquals(0, qm.currentIndex)
        assertEquals("dup_A", qm.currentItem?.id)

        // Remove active item
        assertTrue(qm.remove(0))
        assertEquals(2, qm.queue.size)
        assertEquals(0, qm.currentIndex)
        assertEquals("dup_B", qm.currentItem?.id)
    }

    @Test
    fun testQueueManagerRapidRandomReordersStress() {
        val qm = QueueManager()
        val size = 500
        val songs = (0 until size).map { createSong("s_$it") }
        val activeInitialIndex = 250
        qm.setQueue(songs, activeInitialIndex)

        val activeSongId = "s_$activeInitialIndex"
        assertEquals(activeSongId, qm.currentItem?.id)

        val random = Random(42)
        // Perform 500 random reorders
        for (i in 0 until 500) {
            val from = random.nextInt(size)
            val to = random.nextInt(size)
            val success = qm.reorder(from, to)
            assertTrue(success, "Reorder from $from to $to failed")

            // Invariant: queue size never changes
            assertEquals(size, qm.queue.size)
            // Invariant: active track is always preserved and tracked
            assertEquals(activeSongId, qm.currentItem?.id)
            assertEquals(activeSongId, qm.queue[qm.currentIndex].id)
        }

        // Invariant: All original IDs are still present
        val originalIds = songs.map { it.id }.toSet()
        val currentIds = qm.queue.map { it.id }.toSet()
        assertEquals(originalIds, currentIds)
    }

    @Test
    fun testQueueManagerReorderBoundaryConditions() {
        val qm = QueueManager()
        val songs = (0 until 10).map { createSong("s_$it") }
        qm.setQueue(songs, 0)

        // Negative fromIndex
        assertFalse(qm.reorder(-1, 5))
        // Negative toIndex
        assertFalse(qm.reorder(5, -1))
        // Out of bounds fromIndex
        assertFalse(qm.reorder(10, 5))
        // Out of bounds toIndex
        assertFalse(qm.reorder(5, 10))
        // Identity reorder (same index)
        assertTrue(qm.reorder(3, 3))
        assertEquals("s_0", qm.currentItem?.id)
        assertEquals(0, qm.currentIndex)

        // Move 0 to end (9)
        assertTrue(qm.reorder(0, 9))
        assertEquals(9, qm.currentIndex)
        assertEquals("s_0", qm.currentItem?.id)
        assertEquals("s_0", qm.queue[9].id)
        assertEquals("s_1", qm.queue[0].id)

        // Move end (9) back to 0
        assertTrue(qm.reorder(9, 0))
        assertEquals(0, qm.currentIndex)
        assertEquals("s_0", qm.currentItem?.id)
        assertEquals("s_0", qm.queue[0].id)
    }

    @Test
    fun testQueueManagerActiveVsNonActiveRemovalMatrix() {
        val qm = QueueManager()
        val songs = (0 until 5).map { createSong("s_$it") }

        // Scenario A: Remove non-active item before active item
        qm.setQueue(songs, 3) // Active is s_3 at index 3
        assertTrue(qm.remove(1)) // Remove s_1
        assertEquals(4, qm.queue.size)
        assertEquals(2, qm.currentIndex) // Index shifted down from 3 to 2
        assertEquals("s_3", qm.currentItem?.id) // Item remains s_3

        // Scenario B: Remove non-active item after active item
        assertTrue(qm.remove(3)) // Remove item at index 3 (s_4)
        assertEquals(3, qm.queue.size)
        assertEquals(2, qm.currentIndex) // Index remains 2
        assertEquals("s_3", qm.currentItem?.id) // Item remains s_3

        // Scenario C: Remove active item at end of queue
        // Queue is [s_0, s_2, s_3], active is s_3 at index 2 (last item)
        assertTrue(qm.remove(2)) // Remove s_3
        assertEquals(2, qm.queue.size)
        // When active item at end is removed, currentIndex wraps to 0
        assertEquals(0, qm.currentIndex)
        assertEquals("s_0", qm.currentItem?.id)

        // Scenario D: Remove active item at index 0
        assertTrue(qm.remove(0)) // Remove s_0
        assertEquals(1, qm.queue.size)
        assertEquals(0, qm.currentIndex)
        assertEquals("s_2", qm.currentItem?.id)

        // Scenario E: Remove final item
        assertTrue(qm.remove(0))
        assertEquals(0, qm.queue.size)
        assertEquals(-1, qm.currentIndex)
        assertNull(qm.currentItem)

        // Scenario F: Remove from empty queue or invalid index
        assertFalse(qm.remove(0))
        assertFalse(qm.remove(-1))
        assertFalse(qm.remove(5))
    }

    @Test
    fun testQueueManagerRapidShuffleTogglesPreserveOriginalQueue() {
        val qm = QueueManager()
        val count = 200
        val originalSongs = (0 until count).map { createSong("track_$it") }
        val activeIndex = 77
        val activeSong = originalSongs[activeIndex]

        qm.setQueue(originalSongs, activeIndex)
        assertEquals(activeSong.id, qm.currentItem?.id)
        assertEquals(activeIndex, qm.currentIndex)

        // Perform 100 rapid shuffle on/off toggles
        for (i in 1..100) {
            // Toggle ON
            qm.setShuffle(true)
            assertTrue(qm.shuffleMode, "Shuffle should be ON on iteration $i")
            assertEquals(count, qm.queue.size)
            // Active item must remain playing and placed at index 0 of shuffled queue
            assertEquals(0, qm.currentIndex)
            assertEquals(activeSong.id, qm.currentItem?.id)
            // Original queue must remain completely pristine and intact
            assertEquals(originalSongs, qm.originalQueue)

            // Toggle OFF
            qm.setShuffle(false)
            assertFalse(qm.shuffleMode, "Shuffle should be OFF on iteration $i")
            assertEquals(count, qm.queue.size)
            // Queue must be exactly restored to original order
            assertEquals(originalSongs, qm.queue)
            assertEquals(activeIndex, qm.currentIndex)
            assertEquals(activeSong.id, qm.currentItem?.id)
        }
    }

    // =========================================================================
    // 2. PLAYBACK CONTROLLER STATE FLOW & TRANSITION STRESS TESTS
    // =========================================================================

    @Test
    fun testPlaybackControllerSeekStressAndClamping() = runTest {
        val player = DesktopAudioPlayer()
        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            scope = this
        )
        val songDurationSec = 150 // 150,000 ms
        val song = createSong("seek_test_song", durationSec = songDurationSec)

        controller.play(song)
        advanceUntilIdle()

        assertEquals(150000L, controller.durationMs.value)

        // 1. Seek to exactly 0
        controller.seekTo(0L)
        assertEquals(0L, controller.currentPositionMs.value)
        assertEquals(0L, player.currentPositionMs)

        // 2. Negative seek (-10,000 ms) clamped to 0
        controller.seekTo(-10000L)
        assertEquals(0L, controller.currentPositionMs.value)
        assertEquals(0L, player.currentPositionMs)

        // 3. Seek to exact middle (75,000 ms)
        controller.seekTo(75000L)
        assertEquals(75000L, controller.currentPositionMs.value)
        assertEquals(75000L, player.currentPositionMs)

        // 4. Seek past duration (300,000 ms) clamped to 150,000 ms
        controller.seekTo(300000L)
        assertEquals(150000L, controller.currentPositionMs.value)
        assertEquals(150000L, player.currentPositionMs)

        // 5. Rapid burst of 200 arbitrary seeks
        val random = Random(123)
        for (i in 0 until 200) {
            val arbitrarySeek = random.nextLong(-50000L, 500000L)
            controller.seekTo(arbitrarySeek)
            val expected = arbitrarySeek.coerceIn(0L, 150000L)
            assertEquals(expected, controller.currentPositionMs.value)
            assertEquals(expected, player.currentPositionMs)
        }
    }

    @Test
    fun testPlaybackControllerSkipNextEndTransitionsUnderRepeatModes() = runTest {
        val player = DesktopAudioPlayer()
        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            scope = this
        )
        val songs = listOf(
            createSong("song_A", 100),
            createSong("song_B", 100),
            createSong("song_C", 100)
        )

        // -------------------------------------------------------------
        // Mode 1: RepeatMode.OFF at End of Queue
        // -------------------------------------------------------------
        controller.playQueue(songs, startIndex = 2) // Start at last track (song_C)
        advanceUntilIdle()

        assertEquals("song_C", controller.currentItem.value?.id)
        assertEquals(2, controller.currentIndex.value)
        assertTrue(controller.isPlaying.value)

        // Call skipNext at end of queue
        controller.skipNext()
        advanceUntilIdle()

        // Should halt playback, set state to ENDED, isPlaying=false
        assertFalse(controller.isPlaying.value)
        assertFalse(player.isPlaying)
        assertEquals(PlaybackState.ENDED, controller.playbackState.value)
        assertEquals(2, controller.currentIndex.value) // Index stays at last item

        // -------------------------------------------------------------
        // Mode 2: RepeatMode.ALL at End of Queue
        // -------------------------------------------------------------
        controller.playQueue(songs, startIndex = 2) // Start at last track
        advanceUntilIdle()
        controller.setRepeat(RepeatMode.ALL)
        assertEquals(RepeatMode.ALL, controller.repeatMode.value)

        controller.skipNext()
        advanceUntilIdle()

        // Should loop back to song_A at index 0, isPlaying=true, state=READY
        assertEquals(0, controller.currentIndex.value)
        assertEquals("song_A", controller.currentItem.value?.id)
        assertTrue(controller.isPlaying.value)
        assertTrue(player.isPlaying)
        assertEquals(PlaybackState.READY, controller.playbackState.value)

        // -------------------------------------------------------------
        // Mode 3: RepeatMode.ONE at Any Position
        // -------------------------------------------------------------
        controller.setRepeat(RepeatMode.ONE)
        assertEquals(RepeatMode.ONE, controller.repeatMode.value)

        controller.seekTo(50000L)
        assertEquals(50000L, controller.currentPositionMs.value)

        controller.skipNext()
        advanceUntilIdle()

        // Should replay song_A, reset position to 0, isPlaying=true, state=READY
        assertEquals(0, controller.currentIndex.value)
        assertEquals("song_A", controller.currentItem.value?.id)
        assertEquals(0L, controller.currentPositionMs.value)
        assertTrue(controller.isPlaying.value)
        assertTrue(player.isPlaying)
        assertEquals(PlaybackState.READY, controller.playbackState.value)
    }

    @Test
    fun testPlaybackControllerSkipPreviousInvestigation() = runTest {
        val player = DesktopAudioPlayer()
        val qm = QueueManager()
        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            queueManager = qm,
            scope = this
        )
        val songs = listOf(
            createSong("song_0", 100),
            createSong("song_1", 100),
            createSong("song_2", 100)
        )

        // Case 1: At song_1, position is 5,000ms (> 3,000ms threshold)
        // Expected: skipPrevious restarts song_1 at 0ms
        controller.playQueue(songs, startIndex = 1)
        advanceUntilIdle()
        controller.seekTo(5000L)
        assertEquals(5000L, controller.currentPositionMs.value)

        controller.skipPrevious()
        advanceUntilIdle()

        assertEquals("song_1", controller.currentItem.value?.id)
        assertEquals(1, controller.currentIndex.value)
        assertEquals(1, qm.currentIndex)
        assertEquals(0L, controller.currentPositionMs.value)

        // Case 2: At song_1, position is 0ms (<= 3,000ms threshold)
        // Expected: skipPrevious navigates to song_0 at index 0
        controller.skipPrevious()
        advanceUntilIdle()

        assertEquals(0, controller.currentIndex.value)
        assertEquals(0, qm.currentIndex)
        assertEquals("song_0", controller.currentItem.value?.id)
        assertEquals(0L, controller.currentPositionMs.value)

        // Case 3: At song_1, position is 1,500ms (<= 3,000ms threshold)
        // Verified: Desynchronization defect fixed - both QueueManager and PlaybackController navigate to song_0
        controller.playQueue(songs, startIndex = 1)
        advanceUntilIdle()
        controller.seekTo(1500L)
        assertEquals(1500L, controller.currentPositionMs.value)
        assertEquals(1, controller.currentIndex.value)
        assertEquals(1, qm.currentIndex)

        controller.skipPrevious()
        advanceUntilIdle()

        // Inside QueueManager, currentIndex decremented to 0
        assertEquals(0, qm.currentIndex, "QueueManager correctly decremented index to 0")
        // PlaybackController is synchronized at index 0 playing song_0
        assertEquals(0, controller.currentIndex.value, "PlaybackController synchronized at index 0")
        assertEquals("song_0", controller.currentItem.value?.id, "PlaybackController correctly plays song_0")
        assertEquals(qm.currentIndex, controller.currentIndex.value, "Synchronization confirmed")
    }

    @Test
    fun testPlaybackControllerTrackCompletionAutoProgression() = runTest {
        val player = DesktopAudioPlayer()
        val controller = PlaybackControllerImpl(
            audioPlayer = player,
            scope = this
        )
        val songs = listOf(
            createSong("track_1", 100),
            createSong("track_2", 100),
            createSong("track_3", 100)
        )

        controller.playQueue(songs, startIndex = 0)
        advanceUntilIdle()

        assertEquals("track_1", controller.currentItem.value?.id)
        assertEquals(0, controller.currentIndex.value)

        // Simulate track 1 completes
        player.simulateTrackCompletion()
        advanceUntilIdle()

        assertEquals("track_2", controller.currentItem.value?.id)
        assertEquals(1, controller.currentIndex.value)
        assertTrue(controller.isPlaying.value)

        // Simulate track 2 completes
        player.simulateTrackCompletion()
        advanceUntilIdle()

        assertEquals("track_3", controller.currentItem.value?.id)
        assertEquals(2, controller.currentIndex.value)
        assertTrue(controller.isPlaying.value)

        // Simulate track 3 completes with Repeat OFF
        player.simulateTrackCompletion()
        advanceUntilIdle()

        assertFalse(controller.isPlaying.value)
        assertEquals(PlaybackState.ENDED, controller.playbackState.value)
    }

    @Test
    fun testPlaybackControllerEmptyQueueOperationsResilience() {
        val player = DesktopAudioPlayer()
        val controller = PlaybackControllerImpl(audioPlayer = player)

        // Operations on initial empty queue must never throw
        controller.pause()
        controller.resume()
        controller.seekTo(5000L)
        assertEquals(0L, controller.currentPositionMs.value)
        controller.skipNext()
        assertEquals(PlaybackState.ENDED, controller.playbackState.value)
        controller.skipPrevious()
        controller.reorderQueue(0, 1)
        controller.removeFromQueue(0)
        controller.setShuffle(true)
        controller.setRepeat(RepeatMode.ALL)

        assertEquals(0, controller.queue.value.size)
        assertEquals(-1, controller.currentIndex.value)
        assertNull(controller.currentItem.value)
        assertFalse(controller.isPlaying.value)
    }
}
