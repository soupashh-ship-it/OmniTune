package com.omnitune.app.pip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.omnitune.app.playback.PlaybackActions
import com.omnitune.app.playback.MusicService
import dagger.hilt.android.AndroidEntryPoint

/**
 * Handles PiP remote action intents (play/pause, next, previous).
 * Registered in AndroidManifest and receives broadcasts from PiP action buttons.
 */
@AndroidEntryPoint
class PipActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, MusicService::class.java).apply {
            action = when (intent.action) {
                PlaybackActions.ACTION_PIP_PLAY_PAUSE -> PlaybackActions.ACTION_PLAY_PAUSE
                PlaybackActions.ACTION_PIP_NEXT -> PlaybackActions.ACTION_NEXT
                PlaybackActions.ACTION_PIP_PREVIOUS -> PlaybackActions.ACTION_PREVIOUS
                else -> return
            }
        }
        try {
            context.startService(serviceIntent)
        } catch (_: Exception) {}
    }

}
