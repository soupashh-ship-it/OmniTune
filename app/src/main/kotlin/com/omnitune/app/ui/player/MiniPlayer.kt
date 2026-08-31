/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import com.omnitune.app.models.MiniPlayerStyle
import com.omnitune.app.models.toSuvSong
import com.omnitune.app.playback.PlayerConnection
import com.omnitune.app.ui.component.rememberDominantColors
import com.omnitune.app.ui.player.miniplayer.LiquidGlassMiniPlayer
import com.omnitune.app.ui.player.miniplayer.PillMiniPlayer
import com.omnitune.app.ui.player.miniplayer.StandardMiniPlayer
import com.omnitune.app.ui.player.miniplayer.YTMusicMiniPlayer
import kotlinx.coroutines.flow.flowOf

@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    playerConnection: PlayerConnection? = null,
    onClick: () -> Unit = {},
    onNavigateToAlbum: ((String) -> Unit)? = null,
    onNavigateToArtist: ((String) -> Unit)? = null,
    onShare: ((String, String?) -> Unit)? = null,
    onOpenQueue: (() -> Unit)? = null,
    style: MiniPlayerStyle = MiniPlayerStyle.YT_MUSIC,
    artworkShape: String = "ROUNDED_SQUARE",
) {
    val mediaMetadata by (playerConnection?.mediaMetadata ?: flowOf(null)).collectAsStateWithLifecycle(initialValue = null)
    val isPlaying by (playerConnection?.isPlaying ?: flowOf(false)).collectAsStateWithLifecycle(initialValue = false)
    var currentPosition by remember { mutableLongStateOf(0L) }
    LaunchedEffect(playerConnection, isPlaying) {
        while (true) {
            val pc = playerConnection
            if (pc != null) {
                currentPosition = pc.player.currentPosition
            }
            delay(250)
        }
    }
    val duration = (mediaMetadata?.duration ?: 0) * 1000L

    val song = remember(mediaMetadata) { mediaMetadata?.toSuvSong() } ?: return
    val dominantColors = rememberDominantColors(song.thumbnailUrl)

    val progressProvider: () -> Float = {
        if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        when (style) {
            MiniPlayerStyle.LIQUID_GLASS -> {
                LiquidGlassMiniPlayer(
                    song = song,
                    isPlaying = isPlaying,
                    dominantColors = dominantColors,
                    progressProvider = progressProvider,
                    onPlayPause = { playerConnection?.player?.let { if (it.isPlaying) it.pause() else it.play() } },
                    onNext = { playerConnection?.seekToNext() },
                    onClose = { playerConnection?.player?.stop() },
                    onTap = onClick,
                    artworkShape = artworkShape
                )
            }
            MiniPlayerStyle.FLOATING_PILL -> {
                PillMiniPlayer(
                    song = song,
                    isPlaying = isPlaying,
                    dominantColors = dominantColors,
                    progressProvider = progressProvider,
                    onPlayPause = { playerConnection?.player?.let { if (it.isPlaying) it.pause() else it.play() } },
                    onNext = { playerConnection?.seekToNext() },
                    onClose = { playerConnection?.player?.stop() },
                    onTap = onClick,
                    artworkShape = artworkShape
                )
            }
            MiniPlayerStyle.YT_MUSIC -> {
                YTMusicMiniPlayer(
                    song = song,
                    isPlaying = isPlaying,
                    dominantColors = dominantColors,
                    progressProvider = progressProvider,
                    onPlayPause = { playerConnection?.player?.let { if (it.isPlaying) it.pause() else it.play() } },
                    onNext = { playerConnection?.seekToNext() },
                    onClose = { playerConnection?.player?.stop() },
                    onTap = onClick,
                    artworkShape = artworkShape
                )
            }
            else -> {
                StandardMiniPlayer(
                    song = song,
                    isPlaying = isPlaying,
                    dominantColors = dominantColors,
                    progressProvider = progressProvider,
                    onPlayPause = { playerConnection?.player?.let { if (it.isPlaying) it.pause() else it.play() } },
                    onNext = { playerConnection?.seekToNext() },
                    onClose = { playerConnection?.player?.stop() },
                    onTap = onClick,
                    artworkShape = artworkShape
                )
            }
        }
    }
}
