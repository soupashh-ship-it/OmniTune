@file:OptIn(ExperimentalMaterial3Api::class)

package com.omnitune.app.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.sp
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

private fun referenceSettingsCategories(
    onNavigateToCategory: (String) -> Unit,
): List<SettingsCategory> = listOf(
    SettingsCategory(
        title = "PLAYBACK",
        items = listOf(
            SettingsCategoryItem(R.drawable.ic_play_arrow, "Playback", "Audio quality, crossfade, equalizer", OmniColors.OmniAccentPrimary) { onNavigateToCategory("playback") },
            SettingsCategoryItem(R.drawable.ic_settings, "Player Appearance", "Now playing screen, themes, animations", OmniColors.OmniAccentPrimary) { onNavigateToCategory("appearance") },
            SettingsCategoryItem(R.drawable.ic_moon, "Behavior", "Autoplay, resume, headset & car", OmniColors.OmniAccentPrimary) { onNavigateToCategory("behavior") },
        ),
    ),
    SettingsCategory(
        title = "CONTENT",
        items = listOf(
            SettingsCategoryItem(R.drawable.ic_download, "Downloads", "Offline library and automatic downloads", OmniColors.OmniAccentPrimary) { onNavigateToCategory("downloads") },
            SettingsCategoryItem(R.drawable.ic_storage, "Library", "Shortcuts, layout, and discovery", OmniColors.OmniAccentPrimary) { onNavigateToCategory("library") },
            SettingsCategoryItem(R.drawable.ic_verified, "Parental Controls", "Limit explicit content & manage access", OmniColors.OmniAccentPrimary) { onNavigateToCategory("parental_controls") },
        ),
    ),
    SettingsCategory(
        title = "NOTIFICATIONS",
        items = listOf(
            SettingsCategoryItem(R.drawable.ic_notification_play, "Notifications", "Manage alerts and in-app messages", OmniColors.OmniAccentPrimary) { onNavigateToCategory("notifications") },
        ),
    ),
    SettingsCategory(
        title = "STORAGE",
        items = listOf(
            SettingsCategoryItem(R.drawable.ic_storage, "Storage", "Cache, downloads, and device storage", OmniColors.OmniAccentPrimary) { onNavigateToCategory("storage") },
        ),
    ),
    SettingsCategory(
        title = "SCROBBLING & INTEGRATIONS",
        items = listOf(
            SettingsCategoryItem(R.drawable.ic_sync, "Scrobbling & Integrations", "Last.fm and ListenBrainz services", OmniColors.OmniAccentPrimary) { onNavigateToCategory("scrobbling") },
        ),
    ),
    SettingsCategory(
        title = "UPDATES",
        items = listOf(
            SettingsCategoryItem(R.drawable.ic_download, "Updates", "Check for updates and release notes", OmniColors.OmniAccentPrimary) { onNavigateToCategory("updates") },
        ),
    ),
    SettingsCategory(
        title = "BACKUP & RESTORE",
        items = listOf(
            SettingsCategoryItem(R.drawable.ic_storage, "Backup & Restore", "Backup your library and preferences", OmniColors.OmniAccentPrimary) { onNavigateToCategory("backup_restore") },
        ),
    ),
    SettingsCategory(
        title = "DIAGNOSTICS",
        items = listOf(
            SettingsCategoryItem(R.drawable.ic_insights, "Diagnostics", "Logs, crash reports, and advanced info", OmniColors.OmniAccentPrimary) { onNavigateToCategory("diagnostics") },
        ),
    ),
    SettingsCategory(
        title = "ABOUT",
        items = listOf(
            SettingsCategoryItem(R.drawable.ic_info, "About OmniTune", "Version, terms, privacy & licenses", OmniColors.OmniAccentPrimary) { onNavigateToCategory("about") },
        ),
    ),
)

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToEqualizer: () -> Unit = {},
    onNavigateToCategory: (String) -> Unit = {},
) {
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(40)
        contentVisible = true
    }

    val categories = referenceSettingsCategories(onNavigateToCategory)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
            .background(OmniColors.BackgroundGradient),
        contentPadding = PaddingValues(bottom = 112.dp),
    ) {
        item { Spacer(Modifier.statusBarsPadding()) }
        item {
            SettingsTopBar(
                onSearch = onNavigateToSearch,
                modifier = Modifier.padding(horizontal = OmniSpacing.screenHorizontalCompact),
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
                    modifier = Modifier.padding(horizontal = OmniSpacing.screenHorizontalCompact, vertical = 0.dp),
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
                            start = OmniSpacing.screenHorizontalCompact,
                            end = OmniSpacing.screenHorizontalCompact,
                            top = if (index == 0) 8.dp else 10.dp,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsTopBar(
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_omnitune_logo),
            contentDescription = null,
            tint = OmniColors.OmniAccentPrimary,
            modifier = Modifier.size(32.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = "OmniTune",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
            )
            Text(
                text = "Your music, your vibe.",
                style = MaterialTheme.typography.bodyMedium,
                color = OmniColors.TextSecondary,
            )
        }
        IconButton(
            onClick = onSearch,
            modifier = Modifier
                .size(36.dp)
                .clip(OmniShapes.Pill)
                .border(1.dp, OmniColors.OmniAccentPrimary.copy(alpha = 0.24f), OmniShapes.Pill),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = "Search",
                tint = OmniColors.TextPrimary,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun SettingsIdentityRow(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = OmniColors.TextPrimary,
        )
        Text(
            text = "Personalize your experience",
            style = MaterialTheme.typography.bodyMedium,
            color = OmniColors.TextSecondary,
        )
    }
}

@Composable
private fun SettingsCategorySection(
    category: SettingsCategory,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = category.title,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = OmniColors.OmniAccentSecondary,
            )
            Spacer(modifier = Modifier.width(OmniSpacing.compact))
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                thickness = 0.5.dp,
                color = OmniColors.OmniAccentPrimary.copy(alpha = 0.30f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(OmniShapes.Small)
                .background(OmniColors.SurfaceRaised)
                .border(1.dp, OmniColors.OmniAccentPrimary.copy(alpha = 0.25f), OmniShapes.Small),
        ) {
            category.items.forEachIndexed { index, item ->
                SettingsCategoryRow(
                    item = item,
                    showDivider = index < category.items.size - 1,
                )
            }
        }
    }
}

@Composable
private fun SettingsCategoryRow(
    item: SettingsCategoryItem,
    showDivider: Boolean,
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = item.onClick)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(25.dp)
                        .clip(OmniShapes.Pill)
                        .background(item.accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = null,
                        tint = item.accentColor.copy(alpha = 0.9f),
                        modifier = Modifier.size(14.dp),
                    )
                }

                Spacer(Modifier.width(OmniSpacing.compact))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = OmniColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.subtitle,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        color = OmniColors.TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.width(OmniSpacing.compact))

                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null,
                    tint = OmniColors.TextTertiary.copy(alpha = 0.55f),
                    modifier = Modifier
                        .size(14.dp)
                        .graphicsLayer { rotationZ = 180f },
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 10.dp, end = 10.dp),
                thickness = 0.5.dp,
                color = OmniColors.SurfaceHairline.copy(alpha = 0.42f),
            )
        }
    }
}
