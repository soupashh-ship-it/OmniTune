/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import androidx.datastore.preferences.core.Preferences
import com.omnitune.app.constants.AudioCrossfadeDurationKey
import com.omnitune.app.constants.AudioNormalizationKey
import com.omnitune.app.constants.AudioOffload
import com.omnitune.app.constants.AudioOffloadEnabledKey
import com.omnitune.app.constants.CrossfadeMsKey
import com.omnitune.app.constants.NextSongPreloadingKey
import com.omnitune.app.constants.VolumeNormalizationKey

internal data class PlaybackEnginePreferences(
    val audioOffloadRequested: Boolean,
    val crossfadeDurationMs: Int,
    val skipSilenceEnabled: Boolean,
    val volumeNormalizationEnabled: Boolean,
    val nextSongPreloadingEnabled: Boolean,
) {
    val shouldEnableAudioOffload: Boolean
        get() = audioOffloadRequested && crossfadeDurationMs == 0 && !skipSilenceEnabled
}

internal object PlaybackEnginePreferenceMapper {
    private const val MaxCrossfadeMs = 12_000

    fun fromPreferences(
        preferences: Preferences,
        skipSilenceEnabled: Boolean = preferences[com.omnitune.app.constants.SkipSilenceKey] ?: false,
    ): PlaybackEnginePreferences =
        PlaybackEnginePreferences(
            audioOffloadRequested = preferences[AudioOffloadEnabledKey]
                ?: preferences[AudioOffload]
                ?: false,
            crossfadeDurationMs = crossfadeDurationMs(preferences),
            skipSilenceEnabled = skipSilenceEnabled,
            volumeNormalizationEnabled = preferences[VolumeNormalizationKey]
                ?: preferences[AudioNormalizationKey]
                ?: true,
            nextSongPreloadingEnabled = preferences[NextSongPreloadingKey] ?: true,
        )

    private fun crossfadeDurationMs(preferences: Preferences): Int {
        val storedMs = preferences[CrossfadeMsKey]
        val legacySeconds = preferences[AudioCrossfadeDurationKey]
        val durationMs = storedMs ?: legacySeconds?.times(1_000) ?: 0
        return durationMs.coerceIn(0, MaxCrossfadeMs)
    }
}
