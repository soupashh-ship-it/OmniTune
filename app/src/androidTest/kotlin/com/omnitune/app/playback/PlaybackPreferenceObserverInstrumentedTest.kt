package com.omnitune.app.playback

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.omnitune.app.constants.AudioCrossfadeDurationKey
import com.omnitune.app.constants.SkipSilenceKey
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Uses a temporary preferences file and a real Media3 player, so it does not
 * mutate the application's debug profile when it is eventually executed.
 */
@RunWith(AndroidJUnit4::class)
class PlaybackPreferenceObserverInstrumentedTest {

    @Test
    fun persistedSkipSilenceAndCrossfadeSettingsAreConsumedByPlayer() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferencesFile = File(
            context.cacheDir,
            "playback-preferences-${UUID.randomUUID()}.preferences_pb",
        )
        val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val observerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val preferences = PreferenceDataStoreFactory.create(
            scope = storeScope,
            produceFile = { preferencesFile },
        )
        val crossfadeDurationMs = MutableStateFlow(0)
        val skipSilenceApplied = CompletableDeferred<Unit>()
        lateinit var player: ExoPlayer
        lateinit var observer: PlaybackPreferenceObserver

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            player = ExoPlayer.Builder(context).build().also { realPlayer ->
                realPlayer.addListener(object : Player.Listener {
                    override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) {
                        if (skipSilenceEnabled) skipSilenceApplied.complete(Unit)
                    }
                })
            }
            observer = PlaybackPreferenceObserver(
                preferences = preferences.data,
                player = player,
                scope = observerScope,
                playerVolume = MutableStateFlow(1f),
                playbackFadeFactor = MutableStateFlow(1f),
                normalizationFactor = MutableStateFlow(1f),
                crossfadeDurationMs = crossfadeDurationMs,
                audioNormalizationEnabled = MutableStateFlow(true),
                onAutoSkipNextOnErrorChanged = {},
            )
            observer.start()
        }

        try {
            preferences.edit {
                it[SkipSilenceKey] = true
                it[AudioCrossfadeDurationKey] = 3
            }

            val persisted = preferences.data.first()
            assertTrue(persisted[SkipSilenceKey] == true)
            assertEquals(3, persisted[AudioCrossfadeDurationKey])
            withTimeout(5_000) { skipSilenceApplied.await() }
            assertEquals(3_000, withTimeout(5_000) { crossfadeDurationMs.first { it == 3_000 } })
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                observer.stop()
                player.release()
            }
            observerScope.cancel()
            storeScope.cancel()
            preferencesFile.delete()
        }
    }
}
