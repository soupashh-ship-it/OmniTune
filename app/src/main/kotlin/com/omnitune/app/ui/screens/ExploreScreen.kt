/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.omnitune.app.R
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.innertube.pages.MoodAndGenres
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Provider-backed discovery.  No editorial labels, listener counts, or stations are
 * invented here: every visible category comes from the active music provider.
 */
@Composable
fun ExploreScreen(
    onOpenAllCategories: () -> Unit,
    onBrowse: (String, String?) -> Unit,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedGroup by rememberSaveable { mutableStateOf<String?>(null) }
    val visibleGroups = remember(uiState.groups, selectedGroup) {
        selectedGroup?.let { selected ->
            uiState.groups.filter { it.title == selected }
        }.orEmpty().ifEmpty { uiState.groups }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase),
        contentPadding = PaddingValues(
            start = OmniSpacing.section,
            end = OmniSpacing.section,
            bottom = OmniChrome.BottomContentPaddingWithPlayer,
        ),
        verticalArrangement = Arrangement.spacedBy(OmniSpacing.medium),
    ) {
        item(contentType = "top-inset") {
            Spacer(Modifier.statusBarsPadding())
            Spacer(Modifier.height(4.dp))
        }
        item(contentType = "header") {
            Column {
                Text(
                    text = "Explore",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = OmniColors.TextPrimary,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Find music for every moment.",
                    fontSize = 12.sp,
                    color = OmniColors.TextSecondary,
                )
            }
        }
        if (uiState.groups.isNotEmpty()) {
            item(contentType = "tabs") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "all") {
                        ExploreTab(
                            label = "All",
                            isSelected = selectedGroup == null,
                            onClick = { selectedGroup = null },
                        )
                    }
                    items(uiState.groups, key = { it.title }) { group ->
                        ExploreTab(
                            label = group.title,
                            isSelected = selectedGroup == group.title,
                            onClick = { selectedGroup = group.title },
                        )
                    }
                }
            }
        }
        when {
            uiState.isLoading -> item(contentType = "loading") { ExploreLoading() }
            uiState.error != null && uiState.groups.isEmpty() -> item(contentType = "error") {
                ExploreError(message = uiState.error.orEmpty(), onRetry = viewModel::retry)
            }
            visibleGroups.isEmpty() -> item(contentType = "empty") {
                ExploreEmpty(onOpenAllCategories = onOpenAllCategories)
            }
            else -> visibleGroups.forEach { group ->
                item(key = "section_${group.title}", contentType = "section") {
                    ExploreGroup(
                        group = group,
                        onSeeAll = onOpenAllCategories,
                        onBrowse = onBrowse,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreTab(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val background = if (isSelected) OmniColors.OmniAccentPrimary else OmniColors.SurfaceRaised
    val foreground = if (isSelected) OmniColors.TextOnAccent else OmniColors.TextSecondary
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(OmniShapes.Pill)
            .background(background)
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else OmniColors.SurfaceHairline,
                shape = OmniShapes.Pill,
            )
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Tab
                selected = isSelected
            }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ExploreGroup(
    group: MoodAndGenres,
    onSeeAll: () -> Unit,
    onBrowse: (String, String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = group.title,
                modifier = Modifier.weight(1f),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "See all",
                modifier = Modifier
                    .clip(OmniShapes.Pill)
                    .clickable(onClick = onSeeAll)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = OmniColors.OmniAccentPrimary,
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(group.items.take(8), key = { it.endpoint.browseId }) { item ->
                ExploreCategoryCard(item = item, groupTitle = group.title, onBrowse = onBrowse)
            }
        }
    }
}

@Composable
private fun ExploreCategoryCard(
    item: MoodAndGenres.Item,
    groupTitle: String,
    onBrowse: (String, String?) -> Unit,
) {
    val providerAccent = item.stripeColor.toExploreColor()
    val accent = providerAccent.takeUnless { it == Color.Unspecified } ?: OmniColors.OmniAccentPrimary
    Column(
        modifier = Modifier
            .width(116.dp)
            .clip(OmniShapes.Medium)
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.38f), OmniColors.SurfaceRaised),
                ),
            )
            .border(1.dp, accent.copy(alpha = 0.22f), OmniShapes.Medium)
            .clickable { onBrowse(item.endpoint.browseId, item.endpoint.params) }
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(OmniShapes.Pill)
                .background(Color.Black.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_compass),
                contentDescription = null,
                tint = OmniColors.TextPrimary.copy(alpha = 0.9f),
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = item.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = OmniColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = groupTitle,
            fontSize = 10.sp,
            color = OmniColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ExploreLoading() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp)
                    .clip(OmniShapes.Medium)
                    .background(OmniColors.SurfaceRaised),
            )
        }
    }
}

@Composable
private fun ExploreError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Medium)
            .background(OmniColors.SurfaceRaised)
            .border(1.dp, OmniColors.Error.copy(alpha = 0.28f), OmniShapes.Medium)
            .clickable(onClick = onRetry)
            .padding(OmniSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Explore is unavailable", color = OmniColors.TextPrimary, fontWeight = FontWeight.SemiBold)
        Text(message, color = OmniColors.TextSecondary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text("Tap to retry", color = OmniColors.OmniAccentPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ExploreEmpty(onOpenAllCategories: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Medium)
            .background(OmniColors.SurfaceRaised)
            .padding(OmniSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("No categories are available yet", color = OmniColors.TextPrimary, fontWeight = FontWeight.SemiBold)
        Text("Reconnect and try again to load provider-backed discovery.", color = OmniColors.TextSecondary, fontSize = 12.sp)
        Text(
            text = "Browse categories",
            modifier = Modifier
                .clip(OmniShapes.Pill)
                .clickable(onClick = onOpenAllCategories)
                .padding(vertical = 6.dp),
            color = OmniColors.OmniAccentPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun Long.toExploreColor(): Color {
    if (this == 0L) return Color.Unspecified
    val argb = if ((this and 0xFF000000L) == 0L) this or 0xFF000000L else this
    return Color(argb)
}
