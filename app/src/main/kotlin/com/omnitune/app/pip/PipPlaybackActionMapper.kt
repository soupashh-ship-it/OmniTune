package com.omnitune.app.pip

import com.omnitune.app.playback.PlaybackActions

object PipPlaybackActionMapper {
    fun toPlaybackAction(action: String?): String? = when (action) {
        PlaybackActions.ACTION_PIP_PLAY_PAUSE -> PlaybackActions.ACTION_PLAY_PAUSE
        PlaybackActions.ACTION_PIP_NEXT -> PlaybackActions.ACTION_NEXT
        PlaybackActions.ACTION_PIP_PREVIOUS -> PlaybackActions.ACTION_PREVIOUS
        else -> null
    }
}
