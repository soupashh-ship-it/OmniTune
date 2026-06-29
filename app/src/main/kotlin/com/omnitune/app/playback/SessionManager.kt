/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import com.omnitune.app.MainActivity
import com.omnitune.app.R
import com.omnitune.app.constants.MediaSessionConstants.CommandToggleLike
import com.omnitune.app.constants.MediaSessionConstants.CommandToggleRepeatMode
import com.omnitune.app.constants.MediaSessionConstants.CommandToggleShuffle
import com.omnitune.app.constants.MediaSessionConstants.CommandToggleStartRadio

class SessionManager(
    private val context: Context,
    private val player: Player,
    private val sessionCallback: MusicSessionCallback,
) {
    val session: MediaLibrarySession = MediaLibrarySession.Builder(context, player, sessionCallback)
        .setSessionActivity(
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .setId("OmniTune")
        .build()

    @Suppress("DEPRECATION")
    fun updateCustomLayout() {
        val customLayout = listOf(
            CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                .setSessionCommand(CommandToggleLike)
                .setDisplayName("Like")
                .setIconResId(R.drawable.ic_add)
                .build(),
            CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                .setSessionCommand(CommandToggleRepeatMode)
                .setDisplayName("Repeat")
                .setIconResId(R.drawable.ic_history)
                .build(),
            CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                .setSessionCommand(CommandToggleShuffle)
                .setDisplayName("Shuffle")
                .setIconResId(R.drawable.ic_sort)
                .build(),
            CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                .setSessionCommand(CommandToggleStartRadio)
                .setDisplayName("Radio")
                .setIconResId(R.drawable.ic_share)
                .build(),
        )
        session.setCustomLayout(customLayout)
    }

    fun release() {
        sessionCallback.onDestroy()
        session.release()
    }
}
