@file:OptIn(ExperimentalMaterial3Api::class)

package com.omnitune.app.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(40)
        contentVisible = true
    }

    val categories = listOf(
        SettingsCategory(
            title = "Account and Social",
            items = listOf(
                SettingsCategoryItem(
                    icon = R.drawable.ic_settings,
                    title = "OmniTune Account",
                    subtitle = "Manage your OmniTune account",
                    accentColor = OmniColors.OmniAccentPrimary,
                    onClick = { onNavigateToCategory("account_settings") },
                ),
                SettingsCategoryItem(
                    icon = R.drawable.ic_share,
                    title = "Listen Together",
                    subtitle = "Sync playback with friends",
                    accentColor = OmniColors.OmniAccentSecondary,
                    onClick = { onNavigateToCategory("music_together") },
                ),
            ),
        ),
        SettingsCategory(
            title = "UI and Display",
            items = listOf(
                SettingsCategoryItem(
                    icon = R.drawable.ic_settings,
                    title = "Appearance",
                    subtitle = "Dynamic colors, OLED mode, font",
                    accentColor = OmniColors.OmniAccentPrimary,
                    onClick = { onNavigateToCategory("appearance") },
                ),
            ),
        ),
        SettingsCategory(
            title = "Playback and Audio",
            items = listOf(
                SettingsCategoryItem(
                    icon = R.drawable.ic_play_arrow,
                    title = "Playback & Audio",
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
            title = "Content and Privacy",
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
            title = "Storage",
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
            title = "Integrations",
            items = listOf(
                SettingsCategoryItem(
                    icon = R.drawable.ic_favorite,
                    title = "Scrobbling",
                    subtitle = "Last.fm, ListenBrainz",
                    accentColor = OmniColors.HotLight,
                    onClick = { onNavigateToCategory("scrobbling") },
                ),
                SettingsCategoryItem(
                    icon = R.drawable.ic_favorite,
                    title = "Discord RPC",
                    subtitle = "Rich Presence integration",
                    accentColor = OmniColors.Hot,
                    onClick = { onNavigateToCategory("discord") },
                ),
            ),
        ),
        SettingsCategory(
            title = "System",
            items = listOf(
                SettingsCategoryItem(
                    icon = R.drawable.ic_download,
                    title = "Updates",
                    subtitle = "Check for new versions",
                    accentColor = OmniColors.OmniAccentSecondary,
                    onClick = { onNavigateToCategory("updates") },
                ),
                SettingsCategoryItem(
                    icon = R.drawable.ic_download,
                    title = "Backup & Restore",
                    subtitle = "Export and import your data",
                    accentColor = OmniColors.Downloaded,
                    onClick = { onNavigateToCategory("backup_restore") },
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
        contentPadding = PaddingValues(bottom = 112.dp),
    ) {
        item { Spacer(Modifier.statusBarsPadding()) }
        item { Spacer(Modifier.height(OmniSpacing.compact)) }

        item {
            SettingsTopBar(
                onBack = onBack,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        item {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) +
                    slideInVertically(
                        initialOffsetY = { it / 6 },
                        animationSpec = spring(
                            stiffness = Spring.StiffnessLow,
                            dampingRatio = 0.88f,
                        ),
                    ),
            ) {
                SettingsIdentityRow(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                )
            }
        }

        categories.forEachIndexed { index, category ->
            item {
                AnimatedVisibility(
                    visible = contentVisible,
                    enter = fadeIn(
                        spring(
                            stiffness = Spring.StiffnessLow,
                            dampingRatio = 0.9f,
                        ),
                    ) + slideInVertically(
                        initialOffsetY = { it / 8 },
                        animationSpec = spring(
                            stiffness = Spring.StiffnessLow,
                            dampingRatio = 0.9f,
                        ),
                    ),
                ) {
                    SettingsCategorySection(
                        category = category,
                        modifier = Modifier.padding(
                            start = 24.dp,
                            end = 24.dp,
                            top = if (index == 0) 2.dp else 16.dp,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                modifier = Modifier.size(24.dp),
            )
        }

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            color = OmniColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SettingsIdentityRow(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(OmniColors.SurfaceRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = null,
                tint = OmniColors.OmniAccentPrimary,
                modifier = Modifier.size(34.dp),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "OmniTune",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Normal,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = OmniColors.TextTertiary,
                maxLines = 1,
            )
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
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = OmniColors.TextTertiary.copy(alpha = 0.62f),
            modifier = Modifier.padding(bottom = 4.dp),
        )

        category.items.forEachIndexed { index, item ->
            SettingsCategoryRow(
                item = item,
                showDivider = index < category.items.size - 1,
            )
        }
    }
}

@Composable
private fun SettingsCategoryRow(
    item: SettingsCategoryItem,
    showDivider: Boolean,
) {
    Column {
        Surface(
            onClick = item.onClick,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp)),
            color = Color.Transparent,
            contentColor = OmniColors.TextPrimary,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = null,
                        tint = item.accentColor.copy(alpha = 0.9f),
                        modifier = Modifier.size(23.dp),
                    )
                }

                Spacer(Modifier.width(22.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Normal,
                        color = OmniColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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

                Spacer(Modifier.width(12.dp))

                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null,
                    tint = OmniColors.TextTertiary.copy(alpha = 0.55f),
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = 180f },
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp, end = 4.dp),
                thickness = 0.5.dp,
                color = OmniColors.SurfaceHairline.copy(alpha = 0.42f),
            )
        }
    }
}
