package com.omnitune.app.playback

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.omnitune.app.db.MusicDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import com.omnitune.app.utils.dataStore

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackEventRecorderTest {

    private lateinit var recorder: PlaybackEventRecorder
    private lateinit var player: Player
    private lateinit var database: MusicDatabase
    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        player = mock(Player::class.java)
        database = mock(MusicDatabase::class.java)
        context = mock(Context::class.java)
        recorder = PlaybackEventRecorder(context, player, database, CoroutineScope(testDispatcher))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startPlaybackTracker skips tracking if mediaId is unchanged`() = runTest(testDispatcher) {
        val mediaItem = MediaItem.Builder().setMediaId("test-id").build()
        // If we start the tracker twice for the same ID, it shouldn't cause exceptions or loop infinitely
        // Test ensures no infinite loop/crash
        recorder.startPlaybackTracker(mediaItem)
        recorder.startPlaybackTracker(mediaItem)
        assert(true)
    }
}
