package com.omnitune.app.playback

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.omnitune.app.constants.AudioCrossfadeDurationKey
import com.omnitune.app.constants.AudioNormalizationKey
import com.omnitune.app.constants.AudioOffload
import com.omnitune.app.constants.AudioOffloadEnabledKey
import com.omnitune.app.constants.CrossfadeMsKey
import com.omnitune.app.constants.NextSongPreloadingKey
import com.omnitune.app.constants.SkipSilenceKey
import com.omnitune.app.constants.VolumeNormalizationKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackEnginePreferenceMapperTest {
    @Test
    fun `new playback keys drive runtime engine preferences`() {
        val preferences = mutablePreferencesOf(
            AudioOffload to false,
            AudioOffloadEnabledKey to true,
            AudioCrossfadeDurationKey to 3,
            CrossfadeMsKey to 4_500,
            AudioNormalizationKey to false,
            VolumeNormalizationKey to true,
            NextSongPreloadingKey to false,
        )

        val mapped = PlaybackEnginePreferenceMapper.fromPreferences(preferences)

        assertTrue(mapped.audioOffloadRequested)
        assertEquals(4_500, mapped.crossfadeDurationMs)
        assertTrue(mapped.volumeNormalizationEnabled)
        assertFalse(mapped.nextSongPreloadingEnabled)
        assertFalse(mapped.shouldEnableAudioOffload)
    }

    @Test
    fun `legacy playback keys remain supported`() {
        val preferences = mutablePreferencesOf(
            AudioOffload to true,
            AudioCrossfadeDurationKey to 2,
            AudioNormalizationKey to false,
        )

        val mapped = PlaybackEnginePreferenceMapper.fromPreferences(preferences)

        assertTrue(mapped.audioOffloadRequested)
        assertEquals(2_000, mapped.crossfadeDurationMs)
        assertFalse(mapped.volumeNormalizationEnabled)
        assertTrue(mapped.nextSongPreloadingEnabled)
    }

    @Test
    fun `offload is disabled while skip silence is active`() {
        val preferences = mutablePreferencesOf(
            AudioOffloadEnabledKey to true,
            CrossfadeMsKey to 0,
            SkipSilenceKey to true,
        )

        val mapped = PlaybackEnginePreferenceMapper.fromPreferences(preferences)

        assertFalse(mapped.shouldEnableAudioOffload)
    }
}
