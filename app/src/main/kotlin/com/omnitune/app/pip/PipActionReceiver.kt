package com.omnitune.app.pip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.omnitune.app.playback.MusicService
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Handles PiP remote action intents (play/pause, next, previous).
 * Registered in AndroidManifest and receives broadcasts from PiP action buttons.
 */
@AndroidEntryPoint
class PipActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val playbackAction = PipPlaybackActionMapper.toPlaybackAction(intent.action) ?: return
        val serviceIntent = Intent(context, MusicService::class.java).apply {
            action = playbackAction
        }
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: IllegalStateException) {
            Timber.w(e, "Failed to dispatch PiP playback action")
        } catch (e: SecurityException) {
            Timber.w(e, "Failed to dispatch PiP playback action")
        }
    }

}
