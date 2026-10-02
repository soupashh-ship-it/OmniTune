package com.omnitune.shared.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omnitune.shared.domain.models.SongItem
import com.omnitune.shared.ui.components.BounceButton
import com.omnitune.shared.ui.components.OmniArtwork
import com.omnitune.shared.ui.components.SquircleShape

/**
 * Authentic SuvMusic docked bottom mini player bar (64dp) with spinning vinyl
 * artwork animation, marquee text, play/skip buttons, and progress line.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayer(
    song: SongItem,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onPlayPauseClick: () -> Unit,
    onSkipNextClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rotatingVinylEnabled: Boolean = true,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val progress = if (durationMs > 0L) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_spin"
    )

    val currentRotation = if (rotatingVinylEnabled && isPlaying) rotationAngle else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        shape = SquircleShape(cornerRadius = 16.dp, cornerSmoothing = 0.6f),
        tonalElevation = 6.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(64.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vinyl / Album Art
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .rotate(currentRotation),
                    contentAlignment = Alignment.Center
                ) {
                    OmniArtwork(
                        url = song.thumbnailUrl,
                        size = 48.dp,
                        shape = if (rotatingVinylEnabled) CircleShape else SquircleShape(cornerRadius = 10.dp, cornerSmoothing = 0.6f),
                        contentDescription = "Mini player artwork"
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Artist Marquee
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }

                // Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BounceButton(
                        onClick = onPlayPauseClick,
                        size = 40.dp,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    BounceButton(
                        onClick = onSkipNextClick,
                        size = 40.dp,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next track",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Progress indicator at the bottom edge
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.BottomCenter),
                color = accentColor,
                trackColor = Color.Transparent
            )
        }
    }
}
