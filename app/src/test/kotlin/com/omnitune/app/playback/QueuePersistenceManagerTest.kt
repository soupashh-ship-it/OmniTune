package com.omnitune.app.playback

import androidx.datastore.preferences.core.preferencesOf
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.omnitune.app.constants.PersistentQueueKey
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.playback.continuation.PlaybackContext
import com.omnitune.app.playback.continuation.PlaybackSourceType
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
import org.junit.Assert.assertFalse
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
            preferences = kotlinx.coroutines.flow.MutableStateFlow(preferencesOf()),
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
    fun `restoreQueueMetadataOnly preserves duplicate media ids and selected duplicate index`() = runTest(testDispatcher) {
        val queue = mock(Queue::class.java)
        val restoredItems = mutableListOf<MediaItem>()
        doAnswer { invocation ->
            restoredItems += invocation.getArgument<List<MediaItem>>(0)
            null
        }.`when`(player).setMediaItems(anyList(), anyInt(), anyLong())
        val initialStatus = Queue.Status(
            title = "Duplicate queue",
            items = listOf(
                MediaItem.Builder().setMediaId("duplicate").build(),
                MediaItem.Builder().setMediaId("middle").build(),
                MediaItem.Builder().setMediaId("duplicate").build(),
            ),
            mediaItemIndex = 2,
            position = -50L,
        )
        `when`(queue.getInitialStatus()).thenReturn(initialStatus)

        manager.restoreQueueMetadataOnly(
            queue,
            onMetadataRestored = {},
            onQueueTitleRestored = {},
        )

        verify(player).setMediaItems(anyList(), eq(2), eq(0L))
        assertEquals(listOf("duplicate", "middle", "duplicate"), restoredItems.map(MediaItem::mediaId))
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

        manager.saveQueueState(
            "Runtime queue",
            com.omnitune.app.playback.continuation.PlaybackContext(
                sourceType = com.omnitune.app.playback.continuation.PlaybackSourceType.PLAYLIST,
                sourceId = "pl-42",
                sourceTitle = "Road trip",
            ),
        )
        advanceTimeBy(1_000L)
        advanceUntilIdle()

        val entity = requireNotNull(savedEntity)
        assertEquals("first,second,third", entity.mediaIdList)
        assertEquals(1, entity.startIndex)
        assertEquals(12_345L, entity.position)
        assertEquals("PLAYLIST", entity.playbackSourceType)
        assertEquals("pl-42", entity.playbackSourceId)
        assertEquals("Road trip", entity.playbackSourceTitle)
        assertTrue(entity.playbackAllowAutoplay)
    }

    @Test
    fun `saveQueueState persists duplicate queue and selected duplicate index`() = runTest(testDispatcher) {
        val first = MediaItem.Builder().setMediaId("duplicate").build()
        val second = MediaItem.Builder().setMediaId("middle").build()
        val third = MediaItem.Builder().setMediaId("duplicate").build()
        `when`(player.mediaItemCount).thenReturn(3)
        `when`(player.currentMediaItemIndex).thenReturn(2)
        `when`(player.currentPosition).thenReturn(7_000L)
        `when`(player.getMediaItemAt(0)).thenReturn(first)
        `when`(player.getMediaItemAt(1)).thenReturn(second)
        `when`(player.getMediaItemAt(2)).thenReturn(third)

        manager.saveQueueState(
            "Duplicates",
            PlaybackContext(
                sourceType = PlaybackSourceType.PLAYLIST,
                sourceId = "playlist-with-dupes",
                sourceTitle = "Duplicates",
                shuffledCollection = true,
                allowAutoplay = false,
            ),
            debounceMillis = 0L,
        )
        advanceUntilIdle()

        val entity = requireNotNull(savedEntity)
        assertEquals("duplicate,middle,duplicate", entity.mediaIdList)
        assertEquals(2, entity.startIndex)
        assertEquals(7_000L, entity.position)
        assertEquals("PLAYLIST", entity.playbackSourceType)
        assertEquals("playlist-with-dupes", entity.playbackSourceId)
        assertEquals("Duplicates", entity.playbackSourceTitle)
        assertTrue(entity.playbackShuffledCollection)
        assertFalse(entity.playbackAllowAutoplay)
    }

    @Test
    fun `saveQueueState with zero debounce flushes immediately`() = runTest(testDispatcher) {
        val only = MediaItem.Builder().setMediaId("only").build()
        `when`(player.mediaItemCount).thenReturn(1)
        `when`(player.currentMediaItemIndex).thenReturn(0)
        `when`(player.currentPosition).thenReturn(500L)
        `when`(player.getMediaItemAt(0)).thenReturn(only)

        manager.saveQueueState(null, com.omnitune.app.playback.continuation.PlaybackContext.Unknown, debounceMillis = 0L)
        advanceUntilIdle()

        val entity = requireNotNull(savedEntity)
        assertEquals("only", entity.mediaIdList)
        assertEquals(500L, entity.position)
    }

    @Test
    fun `saveQueueState skips database writes when persistent queue is disabled`() = runTest(testDispatcher) {
        var cleared = false
        doAnswer { cleared = true; null }.`when`(database).clearQueue()
        `when`(player.mediaItemCount).thenReturn(1)
        `when`(player.currentMediaItemIndex).thenReturn(0)
        `when`(player.currentPosition).thenReturn(500L)
        `when`(player.getMediaItemAt(0)).thenReturn(MediaItem.Builder().setMediaId("only").build())
        val localManager = QueuePersistenceManager(
            player = player,
            database = database,
            scope = CoroutineScope(testDispatcher),
            preferences = kotlinx.coroutines.flow.MutableStateFlow(preferencesOf(PersistentQueueKey to false)),
            ioDispatcher = testDispatcher,
        )

        localManager.saveQueueState("Disabled", PlaybackContext.Unknown, debounceMillis = 0L)
        advanceUntilIdle()

        assertEquals(null, savedEntity)
        assertFalse(cleared)
    }

    @Test
    fun `saveQueueState clears persisted queue when player is empty`() = runTest(testDispatcher) {
        var cleared = false
        doAnswer { cleared = true; null }.`when`(database).clearQueue()
        `when`(player.mediaItemCount).thenReturn(0)

        manager.saveQueueState("Empty", com.omnitune.app.playback.continuation.PlaybackContext.Unknown, debounceMillis = 0L)
        advanceUntilIdle()

        assertTrue(cleared)
        assertEquals(null, savedEntity)
    }
}
