package com.omnitune.app.pip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
                ACTION_PLAY_PAUSE -> "com.omnitune.app.action.PLAY_PAUSE"
                ACTION_NEXT -> "com.omnitune.app.action.NEXT"
                ACTION_PREVIOUS -> "com.omnitune.app.action.PREVIOUS"
                else -> return
            }
        }
        try {
            context.startService(serviceIntent)
        } catch (_: Exception) {}
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.omnitune.app.pip.PLAY_PAUSE"
        const val ACTION_NEXT = "com.omnitune.app.pip.NEXT"
        const val ACTION_PREVIOUS = "com.omnitune.app.pip.PREVIOUS"
    }
}
