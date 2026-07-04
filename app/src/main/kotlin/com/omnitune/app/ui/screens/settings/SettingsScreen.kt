@file:OptIn(ExperimentalMaterial3Api::class)

package com.omnitune.app.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omnitune.app.BuildConfig
import com.omnitune.app.R
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import kotlinx.coroutines.delay

private data class SettingsQuickAction(
    val icon: Int,
    val label: String,
    val onClick: () -> Unit,
    val accentColor: Color,
)

private data class SettingsCategory(
    val title: String,
    val items: List<SettingsCategoryItem>,
)

private data class SettingsCategoryItem(
    val icon: Int,
    val title: String,
    val subtitle: String,
    val accentColor: Color,
    val onClick: () -> Unit,
)

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onNavigateToEqualizer: () -> Unit = {},
    onNavigateToCategory: (String) -> Unit = {},
) {
    var heroVisible by remember { mutableStateOf(false) }
    var actionsVisible by remember { mutableStateOf(false) }
    var categoriesVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50); heroVisible = true
        delay(150); actionsVisible = true
        delay(200); categoriesVisible = true
    }

    val quickActions = listOf(
        SettingsQuickAction(
            icon = R.drawable.ic_settings,
            label = "Appearance",
            onClick = { onNavigateToCategory("appearance") },
            accentColor = OmniColors.OmniAccentPrimary,
        ),
        SettingsQuickAction(
            icon = R.drawable.ic_play_arrow,
            label = "Playback & Audio",
            onClick = { onNavigateToCategory("playback") },
            accentColor = OmniColors.ActivePlayback,
        ),
        SettingsQuickAction(
            icon = R.drawable.ic_download,
            label = "Storage",
            onClick = { onNavigateToCategory("storage") },
            accentColor = OmniColors.Downloaded,
        ),
        SettingsQuickAction(
            icon = R.drawable.ic_notification_play,
            label = "Content & Privacy",
            onClick = { onNavigateToCategory("content") },
            accentColor = OmniColors.Warning,
        ),
    )

    val categories = listOf(
        SettingsCategory(
            title = "UI & DISPLAY",
            items = listOf(
                SettingsCategoryItem(
                    icon = R.drawable.ic_settings,
                    title = "Appearance",
                    subtitle = "Theme, layout, player design",
                    accentColor = OmniColors.OmniAccentPrimary,
                    onClick = { onNavigateToCategory("appearance") },
                ),
            ),
        ),
        SettingsCategory(
            title = "PLAYBACK & AUDIO",
            items = listOf(
                SettingsCategoryItem(
                    icon = R.drawable.ic_play_arrow,
                    title = "Playback",
                    subtitle = "Quality, crossfade, equalizer",
                    accentColor = OmniColors.ActivePlayback,
                    onClick = { onNavigateToCategory("playback") },
                ),
                SettingsCategoryItem(
                    icon = R.drawable.ic_list,
                    title = "Lyrics",
                    subtitle = "Providers, animation, display",
                    accentColor = OmniColors.Hot,
                    onClick = { onNavigateToCategory("lyrics") },
                ),
            ),
        ),
        SettingsCategory(
            title = "CONTENT & PRIVACY",
            items = listOf(
                SettingsCategoryItem(
                    icon = R.drawable.ic_insights,
                    title = "Content",
                    subtitle = "Quick Picks, history",
                    accentColor = OmniColors.OmniAccentSecondary,
                    onClick = { onNavigateToCategory("content") },
                ),
                SettingsCategoryItem(
                    icon = R.drawable.ic_notification_play,
                    title = "Notifications",
                    subtitle = "Media controls, battery",
                    accentColor = OmniColors.Warning,
                    onClick = { onNavigateToCategory("notifications") },
                ),
            ),
        ),
        SettingsCategory(
            title = "STORAGE",
            items = listOf(
                SettingsCategoryItem(
                    icon = R.drawable.ic_download,
                    title = "Storage",
                    subtitle = "Cache, downloads, trimmer",
                    accentColor = OmniColors.Downloaded,
                    onClick = { onNavigateToCategory("storage") },
                ),
            ),
        ),
        SettingsCategory(
            title = "INTEGRATIONS",
            items = listOf(
                SettingsCategoryItem(
                    icon = R.drawable.ic_favorite,
                    title = "Scrobbling",
                    subtitle = "Last.fm, ListenBrainz",
                    accentColor = OmniColors.HotLight,
                    onClick = { onNavigateToCategory("scrobbling") },
                ),
            ),
        ),
        SettingsCategory(
            title = "SYSTEM",
            items = listOf(
                SettingsCategoryItem(
                    icon = R.drawable.ic_download,
                    title = "Updates",
                    subtitle = "Check for new versions",
                    accentColor = OmniColors.OmniAccentSecondary,
                    onClick = { onNavigateToCategory("updates") },
                ),
                SettingsCategoryItem(
                    icon = R.drawable.ic_share,
                    title = "Diagnostics",
                    subtitle = "Share diagnostic report",
                    accentColor = OmniColors.Hot,
                    onClick = { onNavigateToCategory("diagnostics") },
                ),
                SettingsCategoryItem(
                    icon = R.drawable.ic_info,
                    title = "About",
                    subtitle = "Version, license, credits",
                    accentColor = OmniColors.TextSecondary,
                    onClick = { onNavigateToCategory("about") },
                ),
            ),
        ),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item { Spacer(Modifier.statusBarsPadding()) }
        item { Spacer(Modifier.height(OmniSpacing.compact)) }

        item {
            AnimatedVisibility(
                visible = heroVisible,
                enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) +
                        slideInVertically(initialOffsetY = { it / 5 }, animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.85f)),
            ) {
                SettingsHeroHeader(
                    onBack = onBack,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        item {
            AnimatedVisibility(
                visible = actionsVisible,
                enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) +
                        slideInVertically(initialOffsetY = { it / 6 }, animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.85f)),
            ) {
                SettingsQuickActionsGrid(
                    actions = quickActions,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }

        items(categories.size) { index ->
            val category = categories[index]
            AnimatedVisibility(
                visible = categoriesVisible,
                enter = fadeIn(tween(360, delayMillis = index * 80)) +
                        slideInVertically(initialOffsetY = { it / 5 }, animationSpec = tween(360, delayMillis = index * 80)),
            ) {
                SettingsCategorySection(
                    category = category,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SettingsHeroHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .clip(OmniShapes.Pill),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Back",
                tint = OmniColors.TextPrimary,
                modifier = Modifier.size(22.dp),
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(OmniColors.OmniAccentPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = null,
                tint = OmniColors.OmniAccentPrimary,
                modifier = Modifier.size(26.dp),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
            )
            Text(
                text = "OmniTune v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextTertiary,
            )
        }
    }
}

@Composable
private fun SettingsQuickActionsGrid(
    actions: List<SettingsQuickAction>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = OmniColors.SurfaceQuiet),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(OmniColors.OmniAccentPrimary.copy(alpha = 0.18f), OmniColors.OmniAccentSecondary.copy(alpha = 0.12f)))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_play_arrow),
                        contentDescription = null,
                        tint = OmniColors.OmniAccentPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = "Quick actions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OmniColors.TextPrimary,
                )
            }
            val rows = actions.chunked(2)
            rows.forEach { rowActions ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    rowActions.forEach { action ->
                        SettingsQuickActionTile(
                            action = action,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowActions.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SettingsQuickActionTile(
    action: SettingsQuickAction,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "tileScale",
    )

    Surface(
        onClick = action.onClick,
        modifier = modifier
            .aspectRatio(1.45f),
        shape = RoundedCornerShape(20.dp),
        color = OmniColors.SurfaceQuiet,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(action.accentColor.copy(alpha = 0.12f), Color.Transparent)))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(action.accentColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(action.icon),
                        contentDescription = null,
                        tint = action.accentColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = action.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = OmniColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SettingsCategorySection(
    category: SettingsCategory,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = category.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = OmniColors.TextTertiary.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = OmniColors.SurfaceQuiet),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                category.items.forEachIndexed { index, item ->
                    SettingsCategoryRow(
                        item = item,
                        showDivider = index < category.items.size - 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryRow(
    item: SettingsCategoryItem,
    showDivider: Boolean,
) {
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "rowAlpha",
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .graphicsLayer { this.alpha = alpha }
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(item.accentColor.copy(alpha = 0.14f), item.accentColor.copy(alpha = 0.08f)))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(item.icon),
                    contentDescription = null,
                    tint = item.accentColor,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(16.dp))

            Surface(
                onClick = item.onClick,
                modifier = Modifier.weight(1f),
                color = Color.Transparent,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = OmniColors.TextPrimary,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = OmniColors.TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = null,
                        tint = OmniColors.TextTertiary.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = 180f },
                    )
                }
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 76.dp, end = 18.dp),
                thickness = 0.4.dp,
                color = OmniColors.SurfaceHairline.copy(alpha = 0.3f),
            )
        }
    }
}
