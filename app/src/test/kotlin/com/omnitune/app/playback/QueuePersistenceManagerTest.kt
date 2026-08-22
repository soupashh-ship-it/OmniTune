package com.omnitune.app.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.playback.queues.Queue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.doAnswer
import org.mockito.Answers
import org.mockito.stubbing.Answer

@OptIn(ExperimentalCoroutinesApi::class)
class QueuePersistenceManagerTest {

    private lateinit var manager: QueuePersistenceManager
    private lateinit var player: Player
    private lateinit var database: MusicDatabase
    private var savedEntity: com.omnitune.app.db.entities.QueueEntity? = null
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        player = mock(Player::class.java)
        savedEntity = null
        database = mock(
            MusicDatabase::class.java,
            Answer { invocation ->
                if (invocation.method.name == "saveQueue") {
                    savedEntity = invocation.arguments[0] as com.omnitune.app.db.entities.QueueEntity
                }
                Answers.RETURNS_DEFAULTS.answer(invocation)
            },
        )
        manager = QueuePersistenceManager(
            player = player,
            database = database,
            scope = CoroutineScope(testDispatcher),
            ioDispatcher = testDispatcher,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `restoreQueueMetadataOnly preserves item order and clamps invalid saved index`() = runTest(testDispatcher) {
        val queue = mock(Queue::class.java)
        val originalVideoId = "dQw4w9WgXcQ"
        val restoredItems = mutableListOf<MediaItem>()
        doAnswer { invocation ->
            restoredItems += invocation.getArgument<List<MediaItem>>(0)
            null
        }.`when`(player).setMediaItems(anyList(), anyInt(), anyLong())
        val initialStatus = Queue.Status(
            title = "Test Queue",
            items = listOf(
                MediaItem.Builder().setMediaId(originalVideoId).setUri("https://expired.example/one").build(),
                MediaItem.Builder().setMediaId("second-video").build(),
            ),
            mediaItemIndex = 99,
            position = 250L,
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
        verify(player).setMediaItems(anyList(), eq(1), eq(250L))
        assertEquals(listOf(originalVideoId, "second-video"), restoredItems.map(MediaItem::mediaId))
        assertTrue(titleRestored)
        assertTrue(metadataRestored)
    }

    @Test
    fun `saveQueueState persists exact queue ordering index and position`() = runTest(testDispatcher) {
        val first = MediaItem.Builder().setMediaId("first").build()
        val second = MediaItem.Builder().setMediaId("second").build()
        val third = MediaItem.Builder().setMediaId("third").build()
        `when`(player.mediaItemCount).thenReturn(3)
        `when`(player.currentMediaItemIndex).thenReturn(1)
        `when`(player.currentPosition).thenReturn(12_345L)
        `when`(player.getMediaItemAt(0)).thenReturn(first)
        `when`(player.getMediaItemAt(1)).thenReturn(second)
        `when`(player.getMediaItemAt(2)).thenReturn(third)

        manager.saveQueueState("Runtime queue")
        advanceTimeBy(1_000L)
        advanceUntilIdle()

        val entity = requireNotNull(savedEntity)
        assertEquals("first,second,third", entity.mediaIdList)
        assertEquals(1, entity.startIndex)
        assertEquals(12_345L, entity.position)
    }
}
