package com.omnitune.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.omnitune.app.R
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.playback.PlayerConnection
import com.omnitune.app.ui.component.EmptyPlaceholder
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import kotlin.math.max

private const val QUEUE_ARTWORK_SIZE = 160

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    playerConnection: PlayerConnection?,
    onBack: () -> Unit = {},
) {
    val queueTitle by playerConnection?.queueTitle?.collectAsState() ?: remember { mutableStateOf(null) }
    val currentIndex by playerConnection?.currentMediaItemIndex?.collectAsState() ?: remember { mutableStateOf(-1) }
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) }
    val itemCount = playerConnection?.mediaItemCount ?: 0
    val upcomingCount = max(itemCount - 1, 0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        OmniColors.OmniBackgroundGradientTop.copy(alpha = 0.82f),
                        OmniColors.OmniBackgroundBase,
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = OmniSpacing.section),
        ) {
            QueueHeader(
                title = queueTitle ?: "Queue",
                itemCount = itemCount,
                upcomingCount = upcomingCount,
                onBack = onBack,
            )

            if (playerConnection == null || mediaMetadata == null) {
                QueueEmptyState(text = "No items in queue")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
                ) {
                    item(contentType = "current") {
                        SectionLabel(
                            title = "Now playing",
                            subtitle = "Current track",
                        )
                        NowPlayingCard(mediaMetadata = mediaMetadata!!)
                    }

                    item(contentType = "upNextHeader") {
                        SectionLabel(
                            title = "Up next",
                            subtitle = if (upcomingCount == 1) "1 track queued" else "$upcomingCount tracks queued",
                        )
                    }

                    if (itemCount <= 1) {
                        item(contentType = "emptyUpcoming") {
                            QueueEmptyState(
                                text = "No upcoming items",
                                compact = true,
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = (0 until itemCount).filter { it != currentIndex },
                            key = { _, index -> queueItemKey(playerConnection, index) },
                            contentType = { _, _ -> "queueItem" },
                        ) { _, index ->
                            val mediaItem = playerConnection.getMediaItemAt(index)
                            val meta = mediaItem.localConfiguration?.tag as? MediaMetadata
                            val title = meta?.title
                                ?: mediaItem.mediaMetadata.title?.toString()
                                ?: "Unknown title"
                            val artists = meta?.artists?.joinToString(", ") { it.name }
                                ?.takeIf { it.isNotBlank() }
                                ?: mediaItem.mediaMetadata.artist?.toString()
                                ?: "Unknown artist"

                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissValue ->
                                    if (
                                        dismissValue == SwipeToDismissBoxValue.EndToStart ||
                                        dismissValue == SwipeToDismissBoxValue.StartToEnd
                                    ) {
                                        playerConnection.removeMediaItem(index)
                                        true
                                    } else {
                                        false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val color by animateColorAsState(
                                        targetValue = when (dismissState.targetValue) {
                                            SwipeToDismissBoxValue.Settled -> Color.Transparent
                                            else -> OmniColors.Error.copy(alpha = 0.28f)
                                        },
                                        label = "queueDismissColor",
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(OmniShapes.Large)
                                            .background(color)
                                            .padding(horizontal = OmniSpacing.large),
                                        contentAlignment = Alignment.CenterEnd,
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_close),
                                            contentDescription = "Remove from queue",
                                            tint = OmniColors.Error,
                                        )
                                    }
                                },
                                content = {
                                    QueueItemRow(
                                        title = title,
                                        artists = artists,
                                        thumbnail = meta?.thumbnailUrl,
                                        isCurrent = false,
                                        onClick = {
                                            playerConnection.seekTo(index, 0)
                                            playerConnection.prepare()
                                        },
                                    )
                                }
                            )
                        }
                    }

                    item(contentType = "bottomSpace") {
                        Spacer(modifier = Modifier.height(OmniSpacing.section))
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueHeader(
    title: String,
    itemCount: Int,
    upcomingCount: Int,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = OmniSpacing.medium, bottom = OmniSpacing.large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(OmniColors.OmniGlassMedium)
                .border(
                    BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle),
                    CircleShape,
                ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Back",
                tint = OmniColors.TextPrimary,
            )
        }

        Spacer(modifier = Modifier.width(OmniSpacing.medium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = queueCountLabel(itemCount, upcomingCount),
                style = MaterialTheme.typography.bodyMedium,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SectionLabel(
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = OmniSpacing.small, bottom = OmniSpacing.compact),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = OmniColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = OmniColors.TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NowPlayingCard(mediaMetadata: MediaMetadata) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.ExtraLarge)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        OmniColors.OmniGlassPlayer,
                        OmniColors.OmniGlassMedium,
                    )
                )
            )
            .border(
                BorderStroke(1.dp, OmniColors.OmniGlassBorderStrong),
                OmniShapes.ExtraLarge,
            )
            .padding(OmniSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QueueArtwork(
            thumbnail = mediaMetadata.thumbnailUrl,
            contentDescription = "Current track artwork",
            size = 72.dp,
            shapeLarge = true,
        )

        Spacer(modifier = Modifier.width(OmniSpacing.medium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Playing now",
                style = MaterialTheme.typography.labelMedium,
                color = OmniColors.ActivePlayback,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = mediaMetadata.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = mediaMetadata.artists.joinToString(", ") { it.name }
                    .ifBlank { "Unknown artist" },
                style = MaterialTheme.typography.bodyMedium,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(OmniSpacing.small))

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(OmniColors.OmniAccentPrimary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_play_arrow),
                contentDescription = "Now playing",
                tint = OmniColors.ActivePlayback,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun QueueItemRow(
    title: String,
    artists: String,
    thumbnail: String?,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Large)
            .background(
                if (isCurrent) {
                    OmniColors.OmniAccentPrimary.copy(alpha = 0.12f)
                } else {
                    OmniColors.OmniGlassSubtle
                }
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (isCurrent) OmniColors.OmniGlassBorderStrong else OmniColors.OmniGlassBorderSubtle,
                ),
                OmniShapes.Large,
            )
            .clickable(onClick = onClick)
            .padding(OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QueueArtwork(
            thumbnail = thumbnail,
            contentDescription = null,
            size = 58.dp,
        )

        Spacer(modifier = Modifier.width(OmniSpacing.small))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artists.ifBlank { "Unknown artist" },
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun QueueArtwork(
    thumbnail: String?,
    contentDescription: String?,
    size: androidx.compose.ui.unit.Dp,
    shapeLarge: Boolean = false,
) {
    val context = LocalContext.current
    val shape = if (shapeLarge) OmniShapes.ArtworkMedium else OmniShapes.ArtworkSmall

    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(OmniColors.OmniGlassStrong),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnail.isNullOrBlank()) {
            Icon(
                painter = painterResource(R.drawable.ic_album),
                contentDescription = contentDescription,
                tint = OmniColors.TextTertiary,
                modifier = Modifier.size(size * 0.48f),
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbnail)
                    .size(Size(QUEUE_ARTWORK_SIZE, QUEUE_ARTWORK_SIZE))
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun QueueEmptyState(
    text: String,
    compact: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (compact) Modifier else Modifier.fillMaxSize())
            .clip(OmniShapes.ExtraLarge)
            .background(OmniColors.OmniGlassSubtle)
            .border(
                BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle),
                OmniShapes.ExtraLarge,
            )
            .padding(
                horizontal = OmniSpacing.section,
                vertical = if (compact) OmniSpacing.hero else OmniSpacing.screen,
            ),
        contentAlignment = Alignment.Center,
    ) {
        EmptyPlaceholder(
            icon = R.drawable.ic_sort,
            text = text,
        )
    }
}

private fun queueCountLabel(itemCount: Int, upcomingCount: Int): String {
    if (itemCount <= 0) return "Queue is empty"
    val total = if (itemCount == 1) "1 track" else "$itemCount tracks"
    val upcoming = if (upcomingCount == 1) "1 up next" else "$upcomingCount up next"
    return "$total · $upcoming"
}

private fun queueItemKey(
    playerConnection: PlayerConnection,
    index: Int,
): String {
    val mediaItem = playerConnection.getMediaItemAt(index)
    val mediaId = mediaItem.mediaId.takeIf { it.isNotBlank() }
    val title = mediaItem.mediaMetadata.title?.toString()
    return mediaId ?: "$index-${title.orEmpty()}"
}
