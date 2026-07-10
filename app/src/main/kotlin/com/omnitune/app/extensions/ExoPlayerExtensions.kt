/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */



package com.omnitune.app.extensions

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi

@UnstableApi
fun ExoPlayer.setOffloadEnabled(enabled: Boolean) {
    val mode = if (enabled) {
        TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED
    } else {
        TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED
    }
    trackSelectionParameters = trackSelectionParameters.buildUpon()
        .setAudioOffloadPreferences(
            TrackSelectionParameters.AudioOffloadPreferences.Builder()
                .setAudioOffloadMode(mode)
                .build()
        )
        .build()
}
