package com.omnitune.app.playback

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.omnitune.app.constants.AudioQuality
import com.omnitune.app.constants.AudioQualityKey
import com.omnitune.app.constants.MobileAudioQualityKey
import com.omnitune.app.constants.PlaybackQualityModeKey
import com.omnitune.app.constants.WifiAudioQualityKey
import com.omnitune.app.models.PlaybackQualityMode
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackQualityPreferenceMapperTest {
    @Test
    fun `wifi quality preference drives playback quality`() {
        val preferences = mutablePreferencesOf(
            WifiAudioQualityKey to AudioQuality.LOW.name,
            MobileAudioQualityKey to AudioQuality.HIGHEST.name,
        )

        assertEquals(
            PlaybackQualityMode.DATA_SAVER,
            PlaybackQualityPreferenceMapper.fromPreferences(preferences, onWifi = true),
        )
    }

    @Test
    fun `mobile quality preference drives playback quality`() {
        val preferences = mutablePreferencesOf(
            WifiAudioQualityKey to AudioQuality.HIGHEST.name,
            MobileAudioQualityKey to AudioQuality.MEDIUM.name,
        )

        assertEquals(
            PlaybackQualityMode.BALANCED,
            PlaybackQualityPreferenceMapper.fromPreferences(preferences, onWifi = false),
        )
    }

    @Test
    fun `legacy audio quality beats legacy playback mode`() {
        val preferences = mutablePreferencesOf(
            AudioQualityKey to AudioQuality.AUTO.name,
            PlaybackQualityModeKey to PlaybackQualityMode.DATA_SAVER.name,
        )

        assertEquals(
            PlaybackQualityMode.AUTO,
            PlaybackQualityPreferenceMapper.fromPreferences(preferences, onWifi = true),
        )
    }

    @Test
    fun `legacy playback mode is still honored when audio quality is absent`() {
        val preferences = mutablePreferencesOf(
            PlaybackQualityModeKey to PlaybackQualityMode.DATA_SAVER.name,
        )

        assertEquals(
            PlaybackQualityMode.DATA_SAVER,
            PlaybackQualityPreferenceMapper.fromPreferences(preferences, onWifi = true),
        )
    }
}
