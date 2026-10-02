package com.omnitune.shared.playback

import com.omnitune.shared.domain.models.SongItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QueueManagerTest {

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
        val qm = QueueManager()
        assertTrue(qm.queue.isEmpty())
        assertTrue(qm.originalQueue.isEmpty())
        assertEquals(-1, qm.currentIndex)
        assertNull(qm.currentItem)
        assertFalse(qm.shuffleMode)
        assertEquals(RepeatMode.OFF, qm.repeatMode)
    }

    @Test
    fun testSetQueueWithValidStartIndex() {
        val qm = QueueManager()
        val songs = listOf(createSong("s1", "Song 1"), createSong("s2", "Song 2"), createSong("s3", "Song 3"))
        qm.setQueue(songs, 1)

        assertEquals(3, qm.queue.size)
        assertEquals(1, qm.currentIndex)
        assertEquals("s2", qm.currentItem?.id)
        assertEquals(3, qm.originalQueue.size)
    }

    @Test
    fun testSetQueueClampsStartIndex() {
        val qm = QueueManager()
        val songs = listOf(createSong("s1", "Song 1"), createSong("s2", "Song 2"))

        qm.setQueue(songs, 10)
        assertEquals(1, qm.currentIndex)
        assertEquals("s2", qm.currentItem?.id)

        qm.setQueue(songs, -5)
        assertEquals(0, qm.currentIndex)
        assertEquals("s1", qm.currentItem?.id)
    }

    @Test
    fun testSetEmptyQueueResetsIndex() {
        val qm = QueueManager()
        qm.setQueue(listOf(createSong("s1", "Song 1")), 0)
        assertEquals(0, qm.currentIndex)

        qm.setQueue(emptyList())
        assertEquals(-1, qm.currentIndex)
        assertNull(qm.currentItem)
        assertTrue(qm.queue.isEmpty())
    }

    @Test
    fun testEnqueueAppendsSongs() {
        val qm = QueueManager()
        val s1 = createSong("s1", "Song 1")
        val s2 = createSong("s2", "Song 2")

        qm.enqueue(s1)
        assertEquals(1, qm.queue.size)
        assertEquals(0, qm.currentIndex)
        assertEquals("s1", qm.currentItem?.id)

        qm.enqueue(s2)
        assertEquals(2, qm.queue.size)
        assertEquals(0, qm.currentIndex)
        assertEquals("s1", qm.currentItem?.id)
        assertEquals("s2", qm.queue[1].id)
    }

    @Test
    fun testEnqueueAllAppendsList() {
        val qm = QueueManager()
        val songs = listOf(createSong("s1", "Song 1"), createSong("s2", "Song 2"))
        qm.enqueueAll(songs)

        assertEquals(2, qm.queue.size)
        assertEquals(0, qm.currentIndex)
        assertEquals("s1", qm.currentItem?.id)
    }

    @Test
    fun testRemoveFromQueue() {
        val qm = QueueManager()
        val songs = listOf(
            createSong("s1", "Song 1"),
            createSong("s2", "Song 2"),
            createSong("s3", "Song 3"),
            createSong("s4", "Song 4")
        )
        qm.setQueue(songs, 2) // Active is s3 at index 2

        // Remove item after active item
        assertTrue(qm.remove(3))
        assertEquals(3, qm.queue.size)
        assertEquals(2, qm.currentIndex)
        assertEquals("s3", qm.currentItem?.id)

        // Remove item before active item
        assertTrue(qm.remove(0))
        assertEquals(2, qm.queue.size)
        assertEquals(1, qm.currentIndex)
        assertEquals("s3", qm.currentItem?.id)

        // Remove active item itself (now index 1)
        assertTrue(qm.remove(1))
        assertEquals(1, qm.queue.size)
        assertEquals(0, qm.currentIndex)
        assertEquals("s2", qm.currentItem?.id)

        // Remove the final item
        assertTrue(qm.remove(0))
        assertEquals(0, qm.queue.size)
        assertEquals(-1, qm.currentIndex)
        assertNull(qm.currentItem)

        // Remove from empty queue returns false
        assertFalse(qm.remove(0))
    }

    @Test
    fun testReorderQueuePreservesActiveTrack() {
        val qm = QueueManager()
        val songs = listOf(
            createSong("s1", "Song 1"),
            createSong("s2", "Song 2"),
            createSong("s3", "Song 3")
        )
        qm.setQueue(songs, 0) // Active is s1 at index 0

        // Move s1 from index 0 to index 2
        assertTrue(qm.reorder(0, 2))
        assertEquals("s1", qm.queue[2].id)
        assertEquals(2, qm.currentIndex)
        assertEquals("s1", qm.currentItem?.id)

        // Move s2 from index 0 to index 1
        assertTrue(qm.reorder(0, 1))
        assertEquals(2, qm.currentIndex)
        assertEquals("s1", qm.currentItem?.id)

        // Invalid indices return false
        assertFalse(qm.reorder(-1, 0))
        assertFalse(qm.reorder(0, 10))
    }

    @Test
    fun testShuffleTogglePreservesActiveTrackAndRestoresOriginal() {
        val qm = QueueManager()
        val songs = (1..10).map { createSong("s$it", "Song $it") }
        qm.setQueue(songs, 3) // Active is s4

        val activeId = qm.currentItem?.id
        assertEquals("s4", activeId)

        // Enable shuffle
        qm.setShuffle(true)
        assertTrue(qm.shuffleMode)
        assertEquals(10, qm.queue.size)
        assertEquals(activeId, qm.currentItem?.id)
        assertEquals(0, qm.currentIndex) // active item is positioned at head of shuffled list

        // Disable shuffle
        qm.setShuffle(false)
        assertFalse(qm.shuffleMode)
        assertEquals(10, qm.queue.size)
        assertEquals("s4", qm.queue[3].id)
        assertEquals(3, qm.currentIndex)
        assertEquals(activeId, qm.currentItem?.id)
    }

    @Test
    fun testRepeatOffProgression() {
        val qm = QueueManager()
        val songs = listOf(createSong("s1", "Song 1"), createSong("s2", "Song 2"))
        qm.setQueue(songs, 0)
        qm.setRepeat(RepeatMode.OFF)

        val next1 = qm.next()
        assertNotNull(next1)
        assertEquals("s2", next1.id)
        assertEquals(1, qm.currentIndex)

        val next2 = qm.next()
        assertNull(next2) // End of queue
        assertEquals(1, qm.currentIndex)
    }

    @Test
    fun testRepeatAllLoopsQueue() {
        val qm = QueueManager()
        val songs = listOf(createSong("s1", "Song 1"), createSong("s2", "Song 2"))
        qm.setQueue(songs, 1) // At last track
        qm.setRepeat(RepeatMode.ALL)

        val next = qm.next()
        assertNotNull(next)
        assertEquals("s1", next.id)
        assertEquals(0, qm.currentIndex)
    }

    @Test
    fun testRepeatOneReplaysSameTrack() {
        val qm = QueueManager()
        val songs = listOf(createSong("s1", "Song 1"), createSong("s2", "Song 2"))
        qm.setQueue(songs, 0)
        qm.setRepeat(RepeatMode.ONE)

        val next = qm.next()
        assertNotNull(next)
        assertEquals("s1", next.id)
        assertEquals(0, qm.currentIndex)
    }

    @Test
    fun testPreviousTrackBehavior() {
        val qm = QueueManager()
        val songs = listOf(createSong("s1", "Song 1"), createSong("s2", "Song 2"), createSong("s3", "Song 3"))
        qm.setQueue(songs, 2) // At s3

        // Elapsed > 3000ms restarts current track
        val restarted = qm.previous(currentPositionMs = 5000L)
        assertNotNull(restarted)
        assertEquals("s3", restarted.id)
        assertEquals(2, qm.currentIndex)

        // Elapsed <= 3000ms goes to previous track
        val prev1 = qm.previous(currentPositionMs = 1500L)
        assertNotNull(prev1)
        assertEquals("s2", prev1.id)
        assertEquals(1, qm.currentIndex)

        val prev2 = qm.previous(currentPositionMs = 500L)
        assertNotNull(prev2)
        assertEquals("s1", prev2.id)
        assertEquals(0, qm.currentIndex)

        // Previous on first track when Repeat OFF restarts first track
        val prevAtStart = qm.previous(currentPositionMs = 0L)
        assertNotNull(prevAtStart)
        assertEquals("s1", prevAtStart.id)
        assertEquals(0, qm.currentIndex)

        // Previous on first track when Repeat ALL wraps to last track
        qm.setRepeat(RepeatMode.ALL)
        val wrappedPrev = qm.previous(currentPositionMs = 0L)
        assertNotNull(wrappedPrev)
        assertEquals("s3", wrappedPrev.id)
        assertEquals(2, qm.currentIndex)
    }

    @Test
    fun testSeekToIndex() {
        val qm = QueueManager()
        val songs = listOf(createSong("s1", "Song 1"), createSong("s2", "Song 2"), createSong("s3", "Song 3"))
        qm.setQueue(songs, 0)

        val item = qm.seekToIndex(2)
        assertNotNull(item)
        assertEquals("s3", item.id)
        assertEquals(2, qm.currentIndex)

        assertNull(qm.seekToIndex(10))
        assertNull(qm.seekToIndex(-1))
    }

    @Test
    fun testClearQueue() {
        val qm = QueueManager()
        qm.setQueue(listOf(createSong("s1", "Song 1")), 0)
        qm.clear()

        assertTrue(qm.queue.isEmpty())
        assertTrue(qm.originalQueue.isEmpty())
        assertEquals(-1, qm.currentIndex)
        assertNull(qm.currentItem)
    }
}
