package com.omnitune.app.db.entities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueueEntityTest {

    @Test
    fun testQueueEntity_defaultValues() {
        val entity = QueueEntity(
            title = "My Queue",
            mediaIdList = "id1,id2,id3",
            startIndex = 0,
            position = 1000L
        )

        assertEquals(0, entity.id)
        assertEquals("My Queue", entity.title)
        assertEquals("id1,id2,id3", entity.mediaIdList)
        assertEquals(0, entity.startIndex)
        assertEquals(1000L, entity.position)
        assertNull(entity.playbackSourceType)
        assertEquals(true, entity.playbackAllowAutoplay)
        assertEquals(false, entity.playbackShuffledCollection)
    }

    @Test
    fun testQueueEntity_nullTitle() {
        val entity = QueueEntity(
            id = 1,
            title = null,
            mediaIdList = "",
            startIndex = 5,
            position = 0L
        )

        assertEquals(1, entity.id)
        assertNull(entity.title)
        assertEquals("", entity.mediaIdList)
        assertEquals(5, entity.startIndex)
        assertEquals(0L, entity.position)
    }
}
