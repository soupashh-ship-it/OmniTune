package com.omnitune.app.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.omnitune.app.R
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.playback.PlayerConnection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    playerConnection: PlayerConnection?,
    onBack: () -> Unit = {},
) {
    val queueTitle by playerConnection?.queueTitle?.collectAsState() ?: remember { mutableStateOf(null) }
    val currentIndex by playerConnection?.currentMediaItemIndex?.collectAsState() ?: remember { mutableStateOf(-1) }
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(queueTitle ?: "Queue") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = "Back",
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )

        if (playerConnection == null || mediaMetadata == null) {
            com.omnitune.app.ui.component.EmptyPlaceholder(
                icon = com.omnitune.app.R.drawable.ic_sort,
                text = "No items in queue",
            )
        } else {
            val itemCount = playerConnection.mediaItemCount

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Now Playing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    NowPlayingRow(mediaMetadata = mediaMetadata!!)
                    HorizontalDivider()
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Up Next (${itemCount - 1})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (itemCount <= 1) {
                    item {
                        com.omnitune.app.ui.component.EmptyPlaceholder(
                            icon = com.omnitune.app.R.drawable.ic_sort,
                            text = "No upcoming items",
                        )
                    }
                } else {
                    itemsIndexed(
                        items = (0 until itemCount).filter { it != currentIndex },
                        key = { _, index -> index }
                    ) { _, index ->
                        val mediaItem = playerConnection.getMediaItemAt(index)
                        val meta = mediaItem.localConfiguration?.tag as? MediaMetadata
                        
                        val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart || dismissValue == androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd) {
                                    playerConnection.removeMediaItem(index)
                                    true
                                } else {
                                    false
                                }
                            }
                        )

                        androidx.compose.material3.SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color by androidx.compose.animation.animateColorAsState(
                                    when (dismissState.targetValue) {
                                        androidx.compose.material3.SwipeToDismissBoxValue.Settled -> androidx.compose.ui.graphics.Color.Transparent
                                        else -> MaterialTheme.colorScheme.errorContainer
                                    },
                                    label = "dismissColor"
                                )
                                Box(
                                    modifier = Modifier.fillMaxSize().background(color).padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        painterResource(com.omnitune.app.R.drawable.ic_close),
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            },
                            content = {
                                QueueItemRow(
                                    title = meta?.title ?: mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                                    artists = meta?.artists?.joinToString(", ") { it.name } ?: "",
                                    thumbnail = meta?.thumbnailUrl,
                                    isCurrent = false,
                                    onClick = {
                                        playerConnection.seekTo(index, 0)
                                        playerConnection.prepare()
                                        // The playback will auto-resume due to playWhenReady in playerConnection
                                    },
                                )
                            }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun NowPlayingRow(mediaMetadata: MediaMetadata) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = mediaMetadata.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mediaMetadata.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = mediaMetadata.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            painter = painterResource(com.omnitune.app.R.drawable.ic_play_arrow),
            contentDescription = "Now Playing",
            tint = MaterialTheme.colorScheme.primary,
        )
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
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (thumbnail != null) {
            AsyncImage(
                model = thumbnail,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artists,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
