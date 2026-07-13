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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.omnitune.app.R
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.component.OmniSectionHeader
import com.omnitune.app.ui.component.shimmer.ShimmerHost
import com.omnitune.app.ui.component.shimmer.ShimmerShape
import com.omnitune.app.ui.theme.LocalOmniAccents
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniMotion
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.OmniTextStyles
import com.omnitune.innertube.pages.MoodAndGenres

@Composable
fun MoodAndGenresScreen(
    onBack: () -> Unit,
    onBrowse: (String, String?) -> Unit,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
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
                .background(OmniColors.BackgroundGradient),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = OmniSpacing.large,
                end = OmniSpacing.large,
                bottom = OmniChrome.BottomContentPaddingWithPlayer,
            ),
            verticalArrangement = Arrangement.spacedBy(OmniSpacing.large),
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
            .padding(top = OmniSpacing.compact),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Back",
                tint = OmniColors.TextPrimary,
            )
        }
        Spacer(modifier = Modifier.width(OmniSpacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Mood and Genres",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Browse real YouTube Music collections",
                style = OmniTextStyles.metadata,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MoodGenresHero(totalCategories: Int) {
    val accent = LocalOmniAccents.current.secondary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Large)
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = 0.20f),
                        OmniColors.SurfacePanel,
                        OmniColors.SurfaceQuiet,
                    ),
                ),
            )
            .border(1.dp, accent.copy(alpha = 0.15f), OmniShapes.Large)
            .padding(OmniSpacing.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(OmniShapes.Pill)
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
        Spacer(modifier = Modifier.width(OmniSpacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Find a mood, open a full collection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (totalCategories > 0) "$totalCategories playable categories" else "Loading categories",
                style = OmniTextStyles.metadata,
                color = OmniColors.TextSecondary,
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
    onBrowse: (String, String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
        OmniSectionHeader(title = title)
        chips.chunked(2).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small),
            ) {
                row.forEachIndexed { columnIndex, chip ->
                    val index = rowIndex * 2 + columnIndex
                    val metadata = HomeDefaultCatalog.findCollection(chip.id)
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(delayMillis = OmniMotion.listItemDelayMs(index, 18, 12))) +
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
                                    onBrowse(providerId, metadata.browseParams)
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
        LocalOmniAccents.current.secondary,
        LocalOmniAccents.current.primary,
        OmniColors.OmniAccentTertiary,
        OmniColors.Hot,
        OmniColors.Warning,
    )
    val accent = fallbackAccents[index % fallbackAccents.size]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(OmniShapes.Medium)
            .background(
                Brush.linearGradient(
                    listOf(
                        OmniColors.SurfaceSubtle.copy(alpha = 0.88f),
                        accent.copy(alpha = 0.12f),
                    ),
                ),
            )
            .border(1.dp, accent.copy(alpha = 0.12f), OmniShapes.Medium)
            .clickable(onClick = onClick)
            .padding(horizontal = OmniSpacing.medium, vertical = OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(OmniShapes.Pill)
                .background(accent),
        )
        Spacer(modifier = Modifier.width(OmniSpacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chip.label,
                style = OmniTextStyles.songTitle,
                fontWeight = FontWeight.SemiBold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Browse",
                style = OmniTextStyles.caption,
                color = OmniColors.TextTertiary,
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
    onBrowse: (String, String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium)) {
        OmniSectionHeader(title = group.title)
        group.items.chunked(2).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small),
            ) {
                row.forEachIndexed { columnIndex, item ->
                    val index = groupIndex * 12 + rowIndex * 2 + columnIndex
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(delayMillis = OmniMotion.listItemDelayMs(index, 18, 12))) +
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
                            onClick = { onBrowse(item.endpoint.browseId, item.endpoint.params) },
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
        LocalOmniAccents.current.secondary,
        LocalOmniAccents.current.primary,
        OmniColors.OmniAccentTertiary,
        OmniColors.Hot,
        OmniColors.Warning,
    )
    val providerAccent = item.stripeColor.toProviderColor()
    val accent = providerAccent.takeUnless { it == Color.Unspecified } ?: fallbackAccents[index % fallbackAccents.size]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(OmniShapes.Medium)
            .background(
                Brush.linearGradient(
                    listOf(
                        OmniColors.SurfaceSubtle.copy(alpha = 0.88f),
                        accent.copy(alpha = 0.12f),
                    ),
                ),
            )
            .border(1.dp, accent.copy(alpha = 0.12f), OmniShapes.Medium)
            .clickable(onClick = onClick)
            .padding(horizontal = OmniSpacing.medium, vertical = OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(OmniShapes.Pill)
                .background(accent),
        )
        Spacer(modifier = Modifier.width(OmniSpacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = OmniTextStyles.songTitle,
                fontWeight = FontWeight.SemiBold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = sectionTitle,
                style = OmniTextStyles.caption,
                color = OmniColors.TextTertiary,
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
            Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.small)) {
                ShimmerShape(
                    modifier = Modifier
                        .fillMaxWidth(0.42f)
                        .height(18.dp),
                )
                repeat(3) {
                    Row(horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small)) {
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
                Spacer(modifier = Modifier.height(OmniSpacing.small))
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
            .clip(OmniShapes.Medium)
            .background(OmniColors.Warning.copy(alpha = 0.10f))
            .clickable(onClick = onRetry)
            .padding(OmniSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = null,
            tint = OmniColors.Warning,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(OmniSpacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Couldn't load moods",
                style = OmniTextStyles.songTitle,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$message Tap to retry.",
                style = OmniTextStyles.metadata,
                color = OmniColors.TextSecondary,
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
