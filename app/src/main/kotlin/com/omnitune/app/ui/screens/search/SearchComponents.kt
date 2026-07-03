package com.omnitune.app.ui.screens.search

import com.omnitune.app.ui.screens.SearchViewModel
import com.omnitune.app.ui.screens.SearchStatus
import com.omnitune.app.ui.screens.SearchUiState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.omnitune.app.R
import com.omnitune.app.db.entities.SearchHistory
import com.omnitune.app.ui.component.GlassCard
import com.omnitune.app.ui.component.GlassSurface
import com.omnitune.app.ui.component.GlassTone
import com.omnitune.app.ui.component.OmniSectionHeader
import com.omnitune.app.ui.component.OmniThumbnailPlaceholder
import com.omnitune.app.ui.component.OmniTuneLoader
import com.omnitune.app.ui.component.ShimmerBar
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.ui.component.TrackMenuProvider
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.OmniTextStyles
import com.omnitune.app.ui.theme.omniPressScale
import com.omnitune.innertube.models.AlbumItem
import com.omnitune.innertube.models.ArtistItem
import com.omnitune.innertube.models.PlaylistItem
import com.omnitune.innertube.models.SongItem
import timber.log.Timber

@Composable
fun SectionLabel(
    title: String,
    count: Int,
) {
    OmniSectionHeader(
        title = title,
        action = "$count",
        modifier = Modifier.padding(top = OmniSpacing.compact),
    )
}


@Composable
fun SearchResultRow(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    fallbackRes: Int,
    onClick: (() -> Unit)?,
    circular: Boolean = false,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    statusText: String = "Info",
    mediaMetadata: com.omnitune.app.models.MediaMetadata? = null,
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    var menuExpanded by remember { mutableStateOf(false) }
    val artworkShape = if (circular) RoundedCornerShape(999.dp) else OmniShapes.ArtworkSmall
    val thumbnailModel = remember(thumbnailUrl) {
        thumbnailUrl?.let {
            ImageRequest.Builder(context)
                .data(it)
                .size(144, 144)
                .memoryCacheKey(it)
                .build()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(OmniShapes.Medium)
            .background(OmniColors.SurfaceSubtle.copy(alpha = 0.42f))
            .then(
                if (onClick != null) {
                    Modifier
                        .omniPressScale(interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = androidx.compose.material3.ripple(
                                bounded = true,
                                color = OmniColors.OmniAccentSecondary.copy(alpha = 0.12f),
                            ),
                        ) {
                            focusManager.clearFocus(force = true)
                            onClick()
                        }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(artworkShape)
                .background(OmniColors.SurfaceQuiet),
            contentAlignment = Alignment.Center,
        ) {
            OmniThumbnailPlaceholder(modifier = Modifier.fillMaxSize())
            if (thumbnailModel != null) {
                AsyncImage(
                    model = thumbnailModel,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    painter = painterResource(fallbackRes),
                    contentDescription = null,
                    tint = OmniColors.TextTertiary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(OmniSpacing.small))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                style = OmniTextStyles.songTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = OmniTextStyles.metadata,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (onPlayNext != null || onAddToQueue != null) {
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = "More options",
                        tint = OmniColors.TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (mediaMetadata != null) {
                    TrackMenuProvider(
                        showMenu = menuExpanded,
                        onDismissMenu = { menuExpanded = false },
                        mediaMetadata = mediaMetadata,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue
                    )
                } else {
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(OmniColors.OmniBackgroundElevated),
                    ) {
                        if (onPlayNext != null) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_skip_next),
                                        contentDescription = null,
                                    )
                                },
                                text = { Text("Play next") },
                                onClick = {
                                    menuExpanded = false
                                    onPlayNext()
                                },
                            )
                        }
                        if (onAddToQueue != null) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_list),
                                        contentDescription = null,
                                    )
                                },
                                text = { Text("Add to queue") },
                                onClick = {
                                    menuExpanded = false
                                    onAddToQueue()
                                },
                            )
                        }
                    }
                }
            }
        } else if (onClick == null) {
            Text(
                text = statusText,
                style = OmniTextStyles.caption,
                color = OmniColors.TextTertiary,
            )
        }
    }
}
