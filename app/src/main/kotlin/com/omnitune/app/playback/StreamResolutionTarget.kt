package com.omnitune.app.playback

/**
 * Captures the queue entry a background stream lookup is allowed to replace.
 * A completion for an old queue entry must never affect the current player item.
 */
internal data class StreamResolutionTarget(
    val mediaId: String,
    val mediaItemIndex: Int,
    val resumePositionMs: Long,
)

internal fun StreamResolutionTarget.isCurrent(
    currentMediaId: String?,
    currentMediaItemIndex: Int,
): Boolean = mediaId == currentMediaId && mediaItemIndex == currentMediaItemIndex
