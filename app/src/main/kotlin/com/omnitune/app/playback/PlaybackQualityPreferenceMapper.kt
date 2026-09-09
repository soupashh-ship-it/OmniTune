/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import androidx.datastore.preferences.core.Preferences
import com.omnitune.app.constants.AudioQuality
import com.omnitune.app.constants.AudioQualityKey
import com.omnitune.app.constants.MobileAudioQualityKey
import com.omnitune.app.constants.PlaybackQualityModeKey
import com.omnitune.app.constants.WifiAudioQualityKey
import com.omnitune.app.models.PlaybackQualityMode

internal object PlaybackQualityPreferenceMapper {
    fun fromPreferences(
        preferences: Preferences,
        onWifi: Boolean,
    ): PlaybackQualityMode {
        val qualityName = if (onWifi) {
            preferences[WifiAudioQualityKey] ?: preferences[AudioQualityKey]
        } else {
            preferences[MobileAudioQualityKey] ?: preferences[AudioQualityKey]
        }
        val selectedQuality = qualityName?.let(::parseAudioQuality)
        if (selectedQuality != null) {
            return selectedQuality.toPlaybackQualityMode()
        }

        val legacyMode = preferences[PlaybackQualityModeKey]?.let(::parsePlaybackQualityMode)
        if (legacyMode != null) return legacyMode

        return if (onWifi) {
            AudioQuality.HIGH.toPlaybackQualityMode()
        } else {
            AudioQuality.MEDIUM.toPlaybackQualityMode()
        }
    }

    private fun parseAudioQuality(name: String): AudioQuality? =
        runCatching { AudioQuality.valueOf(name) }.getOrNull()

    private fun parsePlaybackQualityMode(name: String): PlaybackQualityMode? =
        runCatching { PlaybackQualityMode.valueOf(name) }.getOrNull()

    private fun AudioQuality.toPlaybackQualityMode(): PlaybackQualityMode =
        when (this) {
            AudioQuality.LOW -> PlaybackQualityMode.DATA_SAVER
            AudioQuality.MEDIUM -> PlaybackQualityMode.BALANCED
            AudioQuality.HIGH,
            AudioQuality.HIGHEST -> PlaybackQualityMode.HIGH
            AudioQuality.AUTO -> PlaybackQualityMode.AUTO
        }
}
