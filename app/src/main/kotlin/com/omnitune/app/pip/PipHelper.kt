package com.omnitune.app.pip

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import com.omnitune.app.R
import com.omnitune.app.playback.PlaybackActions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encapsulates Picture-in-Picture logic for both audio and video modes.
 * 
 * - Video mode: 16:9 aspect ratio with play/pause, next, previous actions
 * - Audio-only playback remains in the normal background-media path.
 */
@Singleton
class PipHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    /**
     * Build PiP parameters with appropriate aspect ratio and remote actions.
     * Returns null if PiP is not supported on this API level.
     */
    fun buildPipParams(
        isVideoMode: Boolean,
        isPlaying: Boolean,
        isPipEnabled: Boolean = true,
        sourceRectHint: Rect? = null,
    ): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null

        val aspectRatio = if (isVideoMode) {
            Rational(16, 9) // Widescreen for video
        } else {
            Rational(1, 1) // Square for album art
        }

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .setActions(createRemoteActions(isPlaying))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isPipEnabled && isVideoMode) {
                builder.setAutoEnterEnabled(true)
            } else {
                builder.setAutoEnterEnabled(false)
            }
            builder.setSeamlessResizeEnabled(true)
            if (sourceRectHint != null) {
                builder.setSourceRectHint(sourceRectHint)
            }
        }

        return builder.build()
    }

    /**
     * Enter PiP mode if conditions are met.
     */
    fun enterPipIfEligible(
        activity: Activity,
        hasSong: Boolean,
        isPlaying: Boolean,
        isVideoMode: Boolean = false,
        isPipEnabled: Boolean = true
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (!hasSong || !isPipEnabled || !isVideoMode) return

        val params = buildPipParams(
            isVideoMode = isVideoMode,
            isPlaying = isPlaying,
            isPipEnabled = isPipEnabled,
            sourceRectHint = activity.pipSourceRect(),
        ) ?: return

        try {
            activity.enterPictureInPictureMode(params)
        } catch (_: Exception) {
            // PiP not supported or activity state doesn't allow it
        }
    }

    /**
     * Update PiP params dynamically (e.g., when play state changes in PiP).
     */
    fun updatePipParams(activity: Activity, isPlaying: Boolean = false, isVideoMode: Boolean = false, isPipEnabled: Boolean = true) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val params = buildPipParams(
            isVideoMode = isVideoMode,
            isPlaying = isPlaying,
            isPipEnabled = isPipEnabled,
            sourceRectHint = activity.pipSourceRect(),
        ) ?: return

        try {
            activity.setPictureInPictureParams(params)
        } catch (_: Exception) {
            // Ignore
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createRemoteActions(isPlaying: Boolean): List<RemoteAction> {
        val actions = mutableListOf<RemoteAction>()

        // Previous
        actions.add(
            RemoteAction(
                Icon.createWithResource(context, R.drawable.ic_pip_previous),
                "Previous",
                "Skip to previous track",
                PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_PREVIOUS,
                    Intent(context, PipActionReceiver::class.java)
                        .setAction(PlaybackActions.ACTION_PIP_PREVIOUS),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        )

        // Play/Pause
        val playPauseIcon = if (isPlaying) {
            R.drawable.ic_pip_pause
        } else {
            R.drawable.ic_pip_play
        }
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        actions.add(
            RemoteAction(
                Icon.createWithResource(context, playPauseIcon),
                playPauseTitle,
                "Toggle playback",
                PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_PLAY_PAUSE,
                    Intent(context, PipActionReceiver::class.java)
                        .setAction(PlaybackActions.ACTION_PIP_PLAY_PAUSE),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        )

        // Next
        actions.add(
            RemoteAction(
                Icon.createWithResource(context, R.drawable.ic_pip_next),
                "Next",
                "Skip to next track",
                PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_NEXT,
                    Intent(context, PipActionReceiver::class.java)
                        .setAction(PlaybackActions.ACTION_PIP_NEXT),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        )

        return actions
    }

    companion object {
        private const val REQUEST_CODE_PLAY_PAUSE = 100
        private const val REQUEST_CODE_NEXT = 101
        private const val REQUEST_CODE_PREVIOUS = 102
    }
}

private fun Activity.pipSourceRect(): Rect {
    val rect = Rect()
    window.decorView.getGlobalVisibleRect(rect)
    return rect
}
