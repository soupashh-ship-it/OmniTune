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
import com.omnitune.app.ui.component.OmniFloatingSurface
import com.omnitune.app.ui.component.OmniLoadingPulse
import com.omnitune.app.ui.component.OmniSectionHeader
import com.omnitune.app.ui.component.OmniTrackLoadingRow
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
fun SearchLoadingState() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
    ) {
        item {
            OmniSectionHeader(title = "Searching", action = "Working")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = OmniSpacing.medium),
                contentAlignment = Alignment.Center,
            ) {
                OmniLoadingPulse(size = 42.dp)
            }
        }
        items(5, key = { "search-loading-$it" }, contentType = { "loading" }) {
            OmniFloatingSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp),
                shape = OmniShapes.Large,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(OmniSpacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OmniTrackLoadingRow(artworkSize = 54.dp)
                }
            }
        }
    }
}


@Composable
fun SearchStartState(
    history: List<SearchHistory>,
    onHistoryClick: (String) -> Unit,
    onClearHistory: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
    ) {
        item {
            OmniFloatingSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = OmniShapes.ExtraLarge,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    OmniColors.OmniAccentPrimary.copy(alpha = 0.18f),
                                    OmniColors.OmniAccentSecondary.copy(alpha = 0.08f),
                                    Color.Transparent,
                                )
                            )
                        )
                        .padding(OmniSpacing.large),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.compact)) {
                        Text(
                            text = "Start with a song",
                            style = OmniTextStyles.sectionTitle,
                        )
                        Text(
                            text = "Search for real tracks and play them through the existing OmniTune player.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OmniColors.TextSecondary,
                        )
                    }
                }
            }
        }

        if (history.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(OmniSpacing.compact))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OmniSectionHeader(
                        title = "Recent searches",
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onClearHistory) {
                        Text(
                            text = "Clear",
                            color = OmniColors.OmniAccentSecondary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            items(
                items = history,
                key = { "history-${it.query}" },
                contentType = { "history" },
            ) { item ->
                SearchHistoryRow(
                    query = item.query,
                    onClick = { onHistoryClick(item.query) },
                )
            }
        } else {
            item {
                SearchMessageCard(
                    icon = R.drawable.ic_search,
                    title = "Search for a song to start listening",
                    message = "Results will appear here as songs, artists, albums, and playlists when real data is available.",
                )
            }
        }
    }
}


@Composable
fun SearchErrorState(
    message: String,
    status: SearchStatus,
    onRetry: () -> Unit,
) {
    val title = when (status) {
        SearchStatus.NetworkError -> "Search needs a connection"
        SearchStatus.ParserChanged -> "Search could not read results"
        else -> "Search failed"
    }

    SearchMessageCard(
        icon = R.drawable.ic_search,
        title = title,
        message = message.ifBlank { "Try again in a moment." },
        actionLabel = if (status == SearchStatus.NetworkError) "Retry when online" else "Retry",
        onAction = onRetry,
    )
}


@Composable
fun SearchEmptyResults(query: String) {
    SearchMessageCard(
        icon = R.drawable.ic_search,
        title = "No results found",
        message = "No songs, artists, albums, or playlists matched \"$query\".",
    )
}


@Composable
fun SearchHistoryRow(
    query: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Medium)
            .background(OmniColors.OmniGlassSubtle)
            .omniPressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(
                    bounded = true,
                    color = OmniColors.OmniAccentSecondary.copy(alpha = 0.14f),
                ),
                onClick = onClick,
            )
            .padding(OmniSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_history),
            contentDescription = null,
            tint = OmniColors.TextTertiary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(OmniSpacing.small))
        Text(
            text = query,
            style = MaterialTheme.typography.bodyLarge,
            color = OmniColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}


@Composable
fun SearchStatusPill(message: String) {
    OmniFloatingSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = OmniShapes.Medium,
    ) {
        Row(
            modifier = Modifier.padding(OmniSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = null,
                tint = OmniColors.Warning,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(OmniSpacing.compact))
            Text(
                text = message,
                style = OmniTextStyles.metadata,
                color = OmniColors.TextSecondary,
            )
        }
    }
}


@Composable
fun SearchMessageCard(
    icon: Int,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    OmniFloatingSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = OmniShapes.ExtraLarge,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OmniSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(OmniShapes.Large)
                    .background(OmniColors.OmniAccentPrimary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = OmniColors.OmniAccentSecondary,
                    modifier = Modifier.size(26.dp),
                )
            }
            Text(
                text = title,
                style = OmniTextStyles.sectionTitle,
                color = OmniColors.TextPrimary,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = OmniColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) {
                    Text(
                        text = actionLabel,
                        color = OmniColors.OmniAccentSecondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
