package com.omnitune.app.ui.player

internal data class VideoModeSession(
    val enabled: Boolean = false,
    val mediaId: String? = null,
)

internal object VideoModeStateReducer {
    fun setEnabled(
        current: VideoModeSession,
        enabled: Boolean,
        mediaId: String?,
    ): VideoModeSession =
        if (enabled) {
            current.copy(enabled = true, mediaId = mediaId)
        } else {
            VideoModeSession()
        }

    fun onMediaItemChanged(
        current: VideoModeSession,
        mediaId: String?,
        isVideo: Boolean = false,
    ): VideoModeSession =
        when {
            isVideo && !mediaId.isNullOrBlank() -> VideoModeSession(enabled = true, mediaId = mediaId)
            !current.enabled -> current
            current.mediaId == null -> current.copy(mediaId = mediaId)
            current.mediaId != mediaId -> VideoModeSession()
            else -> current
        }
}
