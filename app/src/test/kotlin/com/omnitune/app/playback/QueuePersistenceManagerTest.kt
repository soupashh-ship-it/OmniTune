package com.omnitune.app.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.playback.queues.Queue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class QueuePersistenceManagerTest {

    private lateinit var manager: QueuePersistenceManager
    private lateinit var player: Player
    private lateinit var database: MusicDatabase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        player = mock(Player::class.java)
        database = mock(MusicDatabase::class.java)
        manager = QueuePersistenceManager(player, database, CoroutineScope(testDispatcher))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `restoreQueueMetadataOnly restores queue items and sets player items`() = runTest(testDispatcher) {
        val queue = mock(Queue::class.java)
        val initialStatus = Queue.Status(
            title = "Test Queue",
            items = listOf(MediaItem.Builder().setMediaId("test").build()),
            mediaItemIndex = 0,
            position = 0L
        )
        `when`(queue.getInitialStatus()).thenReturn(initialStatus)

        var titleRestored = false
        var metadataRestored = false

        manager.restoreQueueMetadataOnly(
            queue,
            onMetadataRestored = { metadataRestored = true },
            onQueueTitleRestored = { titleRestored = true }
        )

        verify(player).stop()
        verify(player).clearMediaItems()
        assert(titleRestored)
        assert(metadataRestored)
    }
}
