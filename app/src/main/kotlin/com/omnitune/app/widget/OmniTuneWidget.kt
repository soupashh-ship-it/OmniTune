package com.omnitune.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import kotlinx.coroutines.guava.await
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.omnitune.app.R
import androidx.glance.Image
import androidx.glance.ImageProvider
import com.omnitune.app.playback.MusicService
import androidx.media3.common.Player

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object WidgetState {
    val titleKey = stringPreferencesKey("title")
    val artistKey = stringPreferencesKey("artist")
    val isPlayingKey = booleanPreferencesKey("isPlaying")
}

fun updateWidgetState(context: Context, title: String, artist: String, isPlaying: Boolean) {
    CoroutineScope(Dispatchers.IO).launch {
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(OmniTuneWidget::class.java).forEach { glanceId ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[WidgetState.titleKey] = title
                    this[WidgetState.artistKey] = artist
                    this[WidgetState.isPlayingKey] = isPlaying
                }
            }
            OmniTuneWidget().update(context, glanceId)
        }
    }
}

class OmniTuneWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OmniTuneWidget()
}

class OmniTuneWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent(context)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context) {
        val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
        val title = prefs[WidgetState.titleKey] ?: "OmniTune"
        val artist = prefs[WidgetState.artistKey] ?: "Ready to play"
        val isPlaying = prefs[WidgetState.isPlayingKey] ?: false

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_launcher_foreground),
                    contentDescription = "Album Art",
                    modifier = GlanceModifier.size(64.dp)
                )

                Spacer(modifier = GlanceModifier.width(16.dp))

                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = title,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = artist,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 14.sp
                        ),
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_skip_previous),
                        contentDescription = "Previous",
                        modifier = GlanceModifier.size(32.dp).clickable(actionRunCallback<PrevAction>())
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Image(
                        provider = ImageProvider(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                        contentDescription = "Play/Pause",
                        modifier = GlanceModifier.size(48.dp).clickable(actionRunCallback<PlayPauseAction>())
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Image(
                        provider = ImageProvider(R.drawable.ic_skip_next),
                        contentDescription = "Next",
                        modifier = GlanceModifier.size(32.dp).clickable(actionRunCallback<NextAction>())
                    )
                }
            }
        }
    }
}

private suspend fun withMediaController(context: Context, action: suspend (androidx.media3.session.MediaController) -> Unit) {
    val sessionToken = androidx.media3.session.SessionToken(context, android.content.ComponentName(context, com.omnitune.app.playback.MusicService::class.java))
    val controllerFuture = androidx.media3.session.MediaController.Builder(context, sessionToken).buildAsync()
    try {
        val controller = controllerFuture.await()
        action(controller)
        controller.release()
    } catch (e: Exception) {
        // Ignored
    }
}

class PlayPauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withMediaController(context) { player ->
            if (player.playbackState == androidx.media3.common.Player.STATE_ENDED) {
                player.seekTo(0, 0)
                player.playWhenReady = true
            } else if (player.playWhenReady) {
                player.pause()
            } else {
                player.play()
            }
        }
    }
}

class NextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withMediaController(context) { player ->
            player.seekToNextMediaItem()
        }
    }
}

class PrevAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withMediaController(context) { player ->
            player.seekToPreviousMediaItem()
        }
    }
}
