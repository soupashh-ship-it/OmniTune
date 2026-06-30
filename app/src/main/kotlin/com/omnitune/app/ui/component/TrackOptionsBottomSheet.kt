package com.omnitune.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.db.entities.Playlist

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
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = OmniColors.OmniBackgroundElevated,
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
                        color = OmniColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OmniColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = OmniSpacing.small),
                color = OmniColors.OmniGlassBorderSubtle
            )

            // Actions
            TrackOptionRow(
                icon = if (isLiked) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
                label = if (isLiked) "Unlike" else "Like",
                tint = if (isLiked) OmniColors.Hot else OmniColors.TextPrimary,
                onClick = {
                    onToggleLike()
                    onDismissRequest()
                }
            )

            if (onPlayNext != null) {
                TrackOptionRow(
                    icon = R.drawable.ic_skip_next,
                    label = "Play next",
                    onClick = {
                        onPlayNext()
                        onDismissRequest()
                    }
                )
            }

            if (onAddToQueue != null) {
                TrackOptionRow(
                    icon = R.drawable.ic_list,
                    label = "Add to queue",
                    onClick = {
                        onAddToQueue()
                        onDismissRequest()
                    }
                )
            }

            if (onMoreLikeThis != null) {
                TrackOptionRow(
                    icon = R.drawable.ic_album,
                    label = "More like this",
                    onClick = {
                        onMoreLikeThis()
                        onDismissRequest()
                    }
                )
            }

            if (onRemoveFromPlaylist != null) {
                TrackOptionRow(
                    icon = R.drawable.ic_close,
                    label = "Remove from this playlist",
                    onClick = {
                        onRemoveFromPlaylist()
                        onDismissRequest()
                    }
                )
            }

            TrackOptionRow(
                icon = R.drawable.ic_add,
                label = "Add to playlist",
                onClick = {
                    onDismissRequest()
                    onAddToPlaylist()
                }
            )
            
            Spacer(modifier = Modifier.height(OmniSpacing.large))
        }
    }
}

@Composable
private fun TrackOptionRow(
    icon: Int,
    label: String,
    tint: androidx.compose.ui.graphics.Color = OmniColors.TextPrimary,
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
            color = OmniColors.TextPrimary
        )
    }
}
