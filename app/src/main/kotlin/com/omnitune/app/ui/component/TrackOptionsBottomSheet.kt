package com.omnitune.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.omniColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOptionsBottomSheet(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    isLiked: Boolean,
    onDismissRequest: () -> Unit,
    onToggleLike: () -> Unit,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onMoreLikeThis: (() -> Unit)? = null,
    onAddToPlaylist: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onStartRadio: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    downloadLabel: String = "Download",
    onToggleLibrary: (() -> Unit)? = null,
    libraryLabel: String = "Add to library",
    onViewArtist: (() -> Unit)? = null,
    onViewAlbum: (() -> Unit)? = null,
    onDetails: (() -> Unit)? = null,
) {
    val colors = omniColors()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = colors.backgroundElevated,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = OmniSpacing.screen)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OmniSpacing.section, vertical = OmniSpacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(OmniShapes.ArtworkSmall),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.width(OmniSpacing.medium))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            NewActionGrid(
                actions = buildList {
                    onStartRadio?.let { action ->
                        add(menuAction(R.drawable.ic_insights, "Start radio", onDismiss = onDismissRequest) {
                            action()
                        })
                    }
                    onPlayNext?.let { action ->
                        add(menuAction(R.drawable.ic_skip_next, "Play next", onDismiss = onDismissRequest) {
                            action()
                        })
                    }
                    onAddToQueue?.let { action ->
                        add(menuAction(R.drawable.ic_list, "Add to queue", onDismiss = onDismissRequest) {
                            action()
                        })
                    }
                    add(NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = null,
                                tint = omniColors().textSecondary,
                                modifier = Modifier.size(28.dp),
                            )
                        },
                        text = "Add to playlist",
                        onClick = {
                            onDismissRequest()
                            onAddToPlaylist()
                        },
                    ))
                    onShare?.let { action ->
                        add(menuAction(R.drawable.ic_share, "Share", onDismiss = onDismissRequest) {
                            action()
                        })
                    }
                    add(menuAction(
                        iconRes = if (isLiked) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
                        label = if (isLiked) "Unlike" else "Like",
                        onClick = onToggleLike,
                        onDismiss = onDismissRequest,
                    ))
                },
                modifier = Modifier.padding(horizontal = OmniSpacing.small, vertical = OmniSpacing.small),
            )

            onToggleLibrary?.let {
                TrackOptionGroup {
                    TrackOptionRow(
                        icon = R.drawable.ic_add,
                        label = libraryLabel,
                        onClick = {
                            it()
                            onDismissRequest()
                        },
                    )
                }
            }

            TrackOptionGroup {
                onRemoveFromPlaylist?.let {
                    TrackOptionRow(
                        icon = R.drawable.ic_close,
                        label = "Remove from playlist",
                        tint = colors.error,
                        onClick = {
                            it()
                            onDismissRequest()
                        },
                    )
                    HorizontalDivider(color = colors.borderSubtle, modifier = Modifier.padding(start = 56.dp))
                }
                onDownload?.let {
                    TrackOptionRow(
                        icon = R.drawable.ic_download,
                        label = downloadLabel,
                        onClick = {
                            it()
                            onDismissRequest()
                        },
                    )
                }
            }

            if (onViewArtist != null || onViewAlbum != null) {
                TrackOptionGroup {
                    onViewArtist?.let {
                        TrackOptionRow(
                            icon = R.drawable.ic_artist,
                            label = "View artist",
                            onClick = {
                                it()
                                onDismissRequest()
                            },
                        )
                    }
                    if (onViewArtist != null && onViewAlbum != null) {
                        HorizontalDivider(color = colors.borderSubtle, modifier = Modifier.padding(start = 56.dp))
                    }
                    onViewAlbum?.let {
                        TrackOptionRow(
                            icon = R.drawable.ic_album,
                            label = "View album",
                            onClick = {
                                it()
                                onDismissRequest()
                            },
                        )
                    }
                }
            }

            onDetails?.let {
                TrackOptionGroup {
                    TrackOptionRow(
                        icon = R.drawable.ic_info,
                        label = "Details",
                        onClick = {
                            it()
                            onDismissRequest()
                        },
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(OmniSpacing.large))
        }
    }
}

@Composable
private fun TrackOptionGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OmniSpacing.section, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(omniColors().surface),
        content = content,
    )
}

@Composable
private fun TrackOptionRow(
    icon: Int,
    label: String,
    tint: androidx.compose.ui.graphics.Color = omniColors().textPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = OmniSpacing.section, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(OmniSpacing.medium))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = omniColors().textPrimary
        )
    }
}
