package com.omnitune.app.playback

import androidx.media3.common.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackHistoryTrackerTest {

    private fun item(id: String): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .build()

    @Test
    fun `transition records previous entry when item changes`() {
        val tracker = PlaybackHistoryTracker()
        tracker.onTransition(item("a"), 0)
        tracker.onTransition(item("b"), 1)

        assertTrue(tracker.hasPrevious())
        val entry = tracker.popPrevious()!!
        assertEquals("a", entry.mediaId)
        assertEquals(0, entry.index)
        assertFalse(tracker.hasPrevious())
    }

    @Test
    fun `same-item transition does not record duplicate entry`() {
        val tracker = PlaybackHistoryTracker()
        tracker.onTransition(item("a"), 0)
        tracker.onTransition(item("a"), 0)

        assertFalse(tracker.hasPrevious())
    }

    @Test
    fun `blank media id is not tracked as current`() {
        val tracker = PlaybackHistoryTracker()
        tracker.onTransition(item(""), 0)
        tracker.onTransition(item("b"), 1)

        // Current was blank -> nothing recorded for it.
        assertFalse(tracker.hasPrevious())
    }

    @Test
    fun `null transition records outgoing current like any other change`() {
        val tracker = PlaybackHistoryTracker()
        tracker.onTransition(item("a"), 0)
        tracker.onTransition(null, -1)

        // Faithful to original MusicService semantics: any current-entry change (including to
        // null) records the outgoing entry.
        assertTrue(tracker.hasPrevious())
        assertEquals("a", tracker.popPrevious()!!.mediaId)
    }

    @Test
    fun `history is capped at maxEntries and evicts oldest first`() {
        val tracker = PlaybackHistoryTracker(maxEntries = 3)
        tracker.onTransition(item("a"), 0)
        tracker.onTransition(item("b"), 1)
        tracker.onTransition(item("c"), 2)
        tracker.onTransition(item("d"), 3)
        // This transition pushes "d" into a full history, evicting "a".
        tracker.onTransition(item("e"), 4)

        // popPrevious returns the most recent previous entry first.
        assertEquals("d", tracker.popPrevious()!!.mediaId)
        assertEquals("c", tracker.popPrevious()!!.mediaId)
        assertEquals("b", tracker.popPrevious()!!.mediaId)
        assertFalse(tracker.hasPrevious())
        assertNull(tracker.popPrevious())
    }

    @Test
    fun `suppressNextRecord drops the next outgoing current instead of recording it`() {
        val tracker = PlaybackHistoryTracker()
        tracker.onTransition(item("a"), 0)

        tracker.suppressNextRecord()
        tracker.onTransition(item("b"), 1)

        // "a" was consumed by the suppression flag, not recorded.
        assertFalse(tracker.hasPrevious())

        // Suppression is one-shot: subsequent transitions record again.
        tracker.onTransition(item("c"), 2)
        assertTrue(tracker.hasPrevious())
        assertEquals("b", tracker.popPrevious()!!.mediaId)
    }

    @Test
    fun `clearSuppressNextRecord restores normal recording`() {
        val tracker = PlaybackHistoryTracker()
        tracker.onTransition(item("a"), 0)

        tracker.suppressNextRecord()
        tracker.clearSuppressNextRecord()
        tracker.onTransition(item("b"), 1)

        assertTrue(tracker.hasPrevious())
        assertEquals("a", tracker.popPrevious()!!.mediaId)
    }

    @Test
    fun `reset clears history current and suppression flag`() {
        val tracker = PlaybackHistoryTracker()
        tracker.onTransition(item("a"), 0)
        tracker.suppressNextRecord()
        tracker.reset()

        tracker.onTransition(item("b"), 5)
        assertFalse(tracker.hasPrevious())

        tracker.onTransition(item("c"), 6)
        assertTrue(tracker.hasPrevious())
        assertEquals("b", tracker.popPrevious()!!.mediaId)
    }
}
