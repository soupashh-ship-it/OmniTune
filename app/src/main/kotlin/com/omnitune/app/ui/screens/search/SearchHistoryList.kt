package com.omnitune.app.ui.screens.search

import com.omnitune.app.ui.screens.SearchViewModel
import com.omnitune.app.ui.screens.SearchStatus
import com.omnitune.app.ui.screens.SearchUiState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.omnitune.app.ui.component.OmniChrome
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
    onStartSearch: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
    ) {
        item { SearchDiscoveryHero(onStartSearch = onStartSearch) }

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
                            text = "Clear all",
                            color = OmniColors.OmniAccentSecondary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
            item(key = "search-history-chips") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small)) {
                    items(
                        items = history,
                        key = { "history-${it.query}" },
                        contentType = { "history" },
                    ) { item ->
                        SearchHistoryChip(
                            query = item.query,
                            onClick = { onHistoryClick(item.query) },
                        )
                    }
                }
            }
        }

        item(key = "search-discovery-queries") {
            Spacer(modifier = Modifier.height(OmniSpacing.medium))
            OmniSectionHeader(title = "Suggested searches")
            Spacer(modifier = Modifier.height(OmniSpacing.small))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small),
                verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
            ) {
                listOf("Chill", "Workout", "Focus", "New releases", "Instrumental").forEach { query ->
                    SearchDiscoveryQueryChip(query = query, onClick = { onHistoryClick(query) })
                }
            }
        }

        item(key = "search-discovery-moods") {
            Spacer(modifier = Modifier.height(OmniSpacing.large))
            OmniSectionHeader(title = "Discover moods")
            Spacer(modifier = Modifier.height(OmniSpacing.small))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
                items(
                    items = listOf(
                        SearchMood("Feel Good", "Uplifting hits", R.drawable.ic_sparkle),
                        SearchMood("Late Night Drive", "Smooth and mellow", R.drawable.ic_mood),
                        SearchMood("Rainy Days", "Calm your mind", R.drawable.ic_cloud),
                        SearchMood("Heartbreak", "Reflect and reset", R.drawable.ic_favorite_border),
                    ),
                    key = { it.title },
                ) { mood ->
                    SearchMoodCard(mood = mood, onClick = { onHistoryClick(mood.title) })
                }
            }
        }

        item(key = "search-start-bottom-spacer") {
            Spacer(modifier = Modifier.height(OmniChrome.BottomContentPaddingWithPlayer))
        }
    }
}

@Composable
private fun SearchDiscoveryHero(
    onStartSearch: () -> Unit,
) {
    OmniFloatingSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(158.dp),
        shape = OmniShapes.ExtraLarge,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF5C2034),
                            Color(0xFF2A1D27),
                            OmniColors.SurfaceRaised,
                        ),
                    ),
                ),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(116.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                OmniColors.OmniAccentPrimary.copy(alpha = 0.46f),
                                Color.Transparent,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(OmniShapes.Pill)
                        .border(2.dp, OmniColors.OmniAccentSecondary.copy(alpha = 0.72f), OmniShapes.Pill)
                        .background(Color(0xFF201B25).copy(alpha = 0.72f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        tint = OmniColors.OmniAccentSecondary,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(214.dp)
                    .padding(end = OmniSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
            ) {
                Text(
                    text = "Search real music.",
                    style = MaterialTheme.typography.titleLarge,
                    color = OmniColors.TextPrimary,
                )
                Text(
                    text = "Find songs, artists, albums and playlists from millions of tracks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmniColors.TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButton(
                    onClick = onStartSearch,
                    modifier = Modifier
                        .clip(OmniShapes.Pill)
                        .background(OmniColors.PrimaryGradient)
                        .border(1.dp, OmniColors.OmniAccentSecondary.copy(alpha = 0.64f), OmniShapes.Pill),
                ) {
                    Text(
                        text = "Start searching  ↗",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = OmniSpacing.small, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

private data class SearchMood(
    val title: String,
    val subtitle: String,
    val icon: Int,
)

@Composable
private fun SearchDiscoveryQueryChip(query: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(OmniShapes.Pill)
            .background(OmniColors.SurfaceRaised)
            .border(1.dp, OmniColors.SurfaceHairline, OmniShapes.Pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_bolt),
            contentDescription = null,
            tint = OmniColors.OmniAccentSecondary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = query,
            style = MaterialTheme.typography.bodyMedium,
            color = OmniColors.TextPrimary,
            maxLines = 1,
        )
    }
}

@Composable
private fun SearchHistoryChip(query: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(OmniShapes.Pill)
            .background(OmniColors.SurfaceRaised)
            .border(1.dp, OmniColors.OmniAccentPrimary.copy(alpha = 0.28f), OmniShapes.Pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_history),
            contentDescription = null,
            tint = OmniColors.TextSecondary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = query,
            style = MaterialTheme.typography.bodyMedium,
            color = OmniColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SearchMoodCard(mood: SearchMood, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(120.dp)
            .height(146.dp)
            .clip(OmniShapes.Large)
            .background(
                Brush.verticalGradient(
                    listOf(
                        OmniColors.OmniAccentPrimary.copy(alpha = 0.38f),
                        OmniColors.SurfaceRaised,
                    ),
                ),
            )
            .border(1.dp, OmniColors.OmniAccentPrimary.copy(alpha = 0.26f), OmniShapes.Large)
            .clickable(onClick = onClick)
            .padding(OmniSpacing.compact),
    ) {
        Icon(
            painter = painterResource(mood.icon),
            contentDescription = null,
            tint = OmniColors.OmniAccentSecondary,
            modifier = Modifier.size(28.dp),
        )
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(
                text = mood.title,
                style = MaterialTheme.typography.titleMedium,
                color = OmniColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = mood.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_play_arrow),
            contentDescription = "Search ${mood.title}",
            tint = OmniColors.TextOnAccent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(28.dp)
                .clip(OmniShapes.Pill)
                .background(OmniColors.OmniAccentPrimary)
                .padding(8.dp),
        )
    }
}


@Composable
fun SearchErrorState(
    message: String,
    status: SearchStatus,
    onRetry: () -> Unit,
) {
    // Parse the message to detect specific error types for better UI guidance
    val (title, actionLabel) = when {
        message.contains("403", ignoreCase = true) ||
            message.contains("blocked", ignoreCase = true) ||
            message.contains("geo-restriction", ignoreCase = true) ->
            "YouTube blocked this search" to "Try again"
        message.contains("404", ignoreCase = true) ||
            message.contains("not found", ignoreCase = true) ->
            "Content not available" to "Search again"
        message.contains("429", ignoreCase = true) ||
            message.contains("rate limit", ignoreCase = true) ||
            message.contains("too many requests", ignoreCase = true) ->
            "Too many requests" to "Retry after a moment"
        message.contains("timeout", ignoreCase = true) ||
            message.contains("timed out", ignoreCase = true) ->
            "Request timed out" to "Retry"
        status == SearchStatus.ParserChanged -> "Search could not read results" to "Retry"
        else -> "Search needs a connection" to "Retry when online"
    }

    SearchMessageCard(
        icon = R.drawable.ic_search,
        title = title,
        message = message.ifBlank { "Try again in a moment." },
        actionLabel = actionLabel,
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
                style = OmniTextStyles.sectionHeader,
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
