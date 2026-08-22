package com.omnitune.app.playback

import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.SessionResult
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.omnitune.app.constants.MediaSessionConstants
import com.google.common.util.concurrent.ListenableFuture
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the real Media3 session/controller command path without starting MusicService. */
@RunWith(AndroidJUnit4::class)
class MusicSessionCallbackInstrumentedTest {

    @Test
    fun callbackDetachesFromTheRealPlayerWhenTheServiceIsDestroyed() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val callback = MusicSessionCallback()
        lateinit var player: ExoPlayer

        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                player = ExoPlayer.Builder(context).build()
                callback.onPlayerReady(player)
                player.setMediaItem(mediaItem("before-destroy"))
            }
            assertEquals("before-destroy", callback.currentMediaItem.value?.mediaId)

            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                callback.onDestroy()
                player.setMediaItem(mediaItem("after-destroy"))
            }
            assertNull(callback.currentMediaItem.value)
            assertEquals(Player.STATE_IDLE, callback.playbackState.value)
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                callback.onDestroy()
                player.release()
            }
        }
    }

    @Test
    fun customSessionCommandsReachTheRealPlayerAndCallbacks() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val callback = MusicSessionCallback()
        val likes = AtomicInteger(0)
        callback.onToggleLike = { likes.incrementAndGet() }
        lateinit var player: ExoPlayer
        lateinit var session: MediaLibrarySession

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            player = ExoPlayer.Builder(context).build()
            callback.onPlayerReady(player)
            session = MediaLibrarySession.Builder(context, player, callback)
                .setId("test-session-${UUID.randomUUID()}")
                .build()
        }

        val controller = createController(context, session)

        try {
            assertEquals(
                SessionResult.RESULT_SUCCESS,
                sendCustomCommand(controller, MediaSessionConstants.CommandToggleLike).resultCode,
            )
            assertEquals(1, likes.get())

            assertEquals(
                SessionResult.RESULT_SUCCESS,
                sendCustomCommand(controller, MediaSessionConstants.CommandToggleShuffle).resultCode,
            )
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                assertTrue(player.shuffleModeEnabled)
            }

            val expectedRepeatModes = listOf(
                Player.REPEAT_MODE_ALL,
                Player.REPEAT_MODE_ONE,
                Player.REPEAT_MODE_OFF,
            )
            expectedRepeatModes.forEach { expected ->
                assertEquals(
                    SessionResult.RESULT_SUCCESS,
                    sendCustomCommand(controller, MediaSessionConstants.CommandToggleRepeatMode).resultCode,
                )
                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    assertEquals(expected, player.repeatMode)
                }
            }
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                controller.release()
                session.release()
                player.release()
                callback.onDestroy()
            }
        }
    }

    @Test
    fun transportCommandsSynchronizeControllerAndPlayerState() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val callback = MusicSessionCallback()
        lateinit var player: ExoPlayer
        lateinit var session: MediaLibrarySession

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            player = ExoPlayer.Builder(context).build()
            player.setMediaItems(
                listOf(
                    mediaItem("transport-first"),
                    mediaItem("transport-second"),
                ),
            )
            callback.onPlayerReady(player)
            session = MediaLibrarySession.Builder(context, player, callback)
                .setId("transport-session-${UUID.randomUUID()}")
                .build()
        }
        val controller = createController(context, session)

        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { controller.play() }
            awaitPlayer(player) { it.playWhenReady }

            InstrumentationRegistry.getInstrumentation().runOnMainSync { controller.pause() }
            awaitPlayer(player) { !it.playWhenReady }

            InstrumentationRegistry.getInstrumentation().runOnMainSync { controller.seekToNextMediaItem() }
            awaitPlayer(player) { it.currentMediaItemIndex == 1 }

            InstrumentationRegistry.getInstrumentation().runOnMainSync { controller.seekToPreviousMediaItem() }
            awaitPlayer(player) { it.currentMediaItemIndex == 0 }
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                controller.release()
                session.release()
                player.release()
                callback.onDestroy()
            }
        }
    }

    private suspend fun awaitPlayer(player: Player, predicate: (Player) -> Boolean) {
        val reached = CompletableDeferred<Unit>()
        lateinit var listener: Player.Listener
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            listener = object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    if (predicate(player)) reached.complete(Unit)
                }
            }
            player.addListener(listener)
            if (predicate(player)) reached.complete(Unit)
        }
        try {
            withTimeout(5_000) { reached.await() }
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                player.removeListener(listener)
            }
        }
    }

    private fun createController(context: Context, session: MediaLibrarySession): MediaController {
        lateinit var future: ListenableFuture<MediaController>
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            future = MediaController.Builder(context, session.token).buildAsync()
        }
        return future.get(5, TimeUnit.SECONDS)
    }

    private fun sendCustomCommand(
        controller: MediaController,
        command: androidx.media3.session.SessionCommand,
    ): SessionResult {
        lateinit var future: ListenableFuture<SessionResult>
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            future = controller.sendCustomCommand(command, Bundle.EMPTY)
        }
        return future.get(5, TimeUnit.SECONDS)
    }

    private fun mediaItem(id: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setUri("https://example.invalid/$id.mp3")
            .build()
}
