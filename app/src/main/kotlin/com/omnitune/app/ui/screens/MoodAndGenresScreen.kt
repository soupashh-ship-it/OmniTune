/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnitune.app.R
import com.omnitune.app.ui.component.shimmer.ShimmerHost
import com.omnitune.app.ui.component.shimmer.ShimmerShape
import com.omnitune.app.ui.navigation.LocalRouteChromeInsets
import com.omnitune.app.ui.theme.SquircleShape
import com.omnitune.innertube.pages.MoodAndGenres

@Composable
fun MoodAndGenresScreen(
    onBack: () -> Unit,
    onBrowse: (String, String?, String) -> Unit,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chromeInsets = LocalRouteChromeInsets.current
    val fallbackChips = GenreChipsHolder.chips.filter { chip ->
        val metadata = HomeDefaultCatalog.findCollection(chip.id)
        metadata?.source == HomeCatalogSource.ProviderBrowse &&
            metadata.actionType == HomeActionType.OPEN_BROWSE &&
            !metadata.providerId.isNullOrBlank()
    }
    val showFallbackChips = uiState.groups.isEmpty() && fallbackChips.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = chromeInsets.contentBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(contentType = "header") {
                MoodGenresHeader(onBack = onBack)
            }

            item(contentType = "intro") {
                MoodGenresHero(totalCategories = uiState.totalCategories)
            }

            if (uiState.isLoading && !showFallbackChips) {
                item(contentType = "loading") {
                    MoodGenresLoading()
                }
            }

            if (showFallbackChips) {
                item(key = "cached_categories", contentType = "cached-group") {
                    MoodChipBrowseGroup(
                        title = if (uiState.isLoading) "Mood and Genres" else "Recently loaded categories",
                        chips = fallbackChips,
                        onBrowse = onBrowse,
                    )
                }
            }

            uiState.error?.takeUnless { showFallbackChips }?.let { error ->
                item(contentType = "error") {
                    MoodGenresError(message = error, onRetry = viewModel::retry)
                }
            }

            uiState.groups.forEachIndexed { groupIndex, group ->
                item(key = "group_${group.title}", contentType = "group") {
                    MoodGenresGroup(
                        group = group,
                        groupIndex = groupIndex,
                        onBrowse = onBrowse,
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodGenresHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Mood and Genres",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Browse real YouTube Music collections",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MoodGenresHero(totalCategories: Int) {
    val accent = MaterialTheme.colorScheme.secondary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SquircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = 0.20f),
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ),
            )
            .border(1.dp, accent.copy(alpha = 0.15f), SquircleShape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_album),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Find a mood, open a full collection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (totalCategories > 0) "$totalCategories playable categories" else "Loading categories",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MoodChipBrowseGroup(
    title: String,
    chips: List<MoodChip>,
    onBrowse: (String, String?, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        chips.chunked(2).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEachIndexed { columnIndex, chip ->
                    val index = rowIndex * 2 + columnIndex
                    val metadata = HomeDefaultCatalog.findCollection(chip.id)
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(delayMillis = (index * 12).coerceAtMost(100))) +
                            slideInVertically(
                                animationSpec = spring(dampingRatio = 0.82f),
                                initialOffsetY = { it / 5 },
                            ),
                        modifier = Modifier.weight(1f),
                    ) {
                        MoodChipBrowseButton(
                            chip = chip,
                            index = index,
                            onClick = {
                                val providerId = metadata?.providerId
                                if (!providerId.isNullOrBlank()) {
                                    onBrowse(providerId, metadata.browseParams, chip.label)
                                }
                            },
                        )
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MoodChipBrowseButton(
    chip: MoodChip,
    index: Int,
    onClick: () -> Unit,
) {
    val fallbackAccents = listOf(
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
    )
    val accent = fallbackAccents[index % fallbackAccents.size]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(SquircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f),
                        accent.copy(alpha = 0.12f),
                    ),
                ),
            )
            .border(1.dp, accent.copy(alpha = 0.12f), SquircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chip.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Browse",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MoodGenresGroup(
    group: MoodAndGenres,
    groupIndex: Int,
    onBrowse: (String, String?, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = group.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        group.items.chunked(2).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEachIndexed { columnIndex, item ->
                    val index = groupIndex * 12 + rowIndex * 2 + columnIndex
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(delayMillis = (index * 12).coerceAtMost(100))) +
                            slideInVertically(
                                animationSpec = spring(dampingRatio = 0.82f),
                                initialOffsetY = { it / 5 },
                            ),
                        modifier = Modifier.weight(1f),
                    ) {
                        MoodGenresButton(
                            item = item,
                            sectionTitle = group.title,
                            index = index,
                            onClick = { onBrowse(item.endpoint.browseId, item.endpoint.params, item.title) },
                        )
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MoodGenresButton(
    item: MoodAndGenres.Item,
    sectionTitle: String,
    index: Int,
    onClick: () -> Unit,
) {
    val fallbackAccents = listOf(
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
    )
    val providerAccent = item.stripeColor.toProviderColor()
    val accent = providerAccent.takeUnless { it == Color.Unspecified } ?: fallbackAccents[index % fallbackAccents.size]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(SquircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f),
                        accent.copy(alpha = 0.12f),
                    ),
                ),
            )
            .border(1.dp, accent.copy(alpha = 0.12f), SquircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = sectionTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MoodGenresLoading() {
    ShimmerHost {
        repeat(4) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerShape(
                    modifier = Modifier
                        .fillMaxWidth(0.42f)
                        .height(18.dp),
                )
                repeat(3) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShimmerShape(
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp),
                        )
                        ShimmerShape(
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MoodGenresError(
    message: String,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SquircleShape)
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f))
            .clickable(onClick = onRetry)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Couldn't load moods",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$message Tap to retry.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun Long.toProviderColor(): Color {
    if (this == 0L) return Color.Unspecified
    val argb = if ((this and 0xFF000000L) == 0L) this or 0xFF000000L else this
    return Color(argb)
}
