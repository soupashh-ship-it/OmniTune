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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
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
import com.omnitune.app.ui.theme.OmniTextStyles
import kotlinx.coroutines.delay
private data class SettingsSection(
    val sectionTitle: String,
    val items: List<SettingsItem>,
)

private data class SettingsItem(
    val id: String,
    val icon: Int,
    val title: String,
    val subtitle: String,
    val keywords: String = "",
    val onClick: () -> Unit,
)

private fun buildSettingsSections(
    onNavigateToCategory: (String) -> Unit,
): List<SettingsSection> = listOf(
    SettingsSection(
        sectionTitle = "AUDIO & PLAYBACK",
        items = listOf(
            SettingsItem(
                id = "playback",
                icon = R.drawable.ic_play_arrow,
                title = "Playback Engine",
                subtitle = "Audio quality, crossfade, replay gain, equalizer",
                keywords = "quality sound bit depth volume normalizer gapless equalizer eq",
                onClick = { onNavigateToCategory("playback") },
            ),
            SettingsItem(
                id = "appearance",
                icon = R.drawable.ic_settings,
                title = "Player & Appearance",
                subtitle = "Now playing theme, font styles, dynamic palette",
                keywords = "theme dark mode oled pure black colors styling artwork",
                onClick = { onNavigateToCategory("appearance") },
            ),
            SettingsItem(
                id = "behavior",
                icon = R.drawable.ic_moon,
                title = "Listening Behavior",
                subtitle = "Autoplay continuation, headset auto-resume, sleep timer defaults",
                keywords = "autoplay resume pause disconnect bluetooth car audio",
                onClick = { onNavigateToCategory("behavior") },
            ),
        ),
    ),
    SettingsSection(
        sectionTitle = "LIBRARY & STORAGE",
        items = listOf(
            SettingsItem(
                id = "downloads",
                icon = R.drawable.ic_download,
                title = "Downloads & Offline",
                subtitle = "Offline library management and automatic downloads",
                keywords = "offline download cache storage songs save tracks",
                onClick = { onNavigateToCategory("downloads") },
            ),
            SettingsItem(
                id = "storage",
                icon = R.drawable.ic_storage,
                title = "Storage & Cache",
                subtitle = "Cached streams, disk footprint, database cleanup",
                keywords = "clear cache storage memory space disk",
                onClick = { onNavigateToCategory("storage") },
            ),
            SettingsItem(
                id = "library",
                icon = R.drawable.ic_list,
                title = "Library Navigation",
                subtitle = "Custom tabs, display density, and default sorting",
                keywords = "tabs playlists favorites artists albums sorting",
                onClick = { onNavigateToCategory("library") },
            ),
            SettingsItem(
                id = "parental_controls",
                icon = R.drawable.ic_verified,
                title = "Content & Restrictions",
                subtitle = "Explicit content filters and restriction rules",
                keywords = "explicit filter parental clean mature restriction",
                onClick = { onNavigateToCategory("parental_controls") },
            ),
        ),
    ),
    SettingsSection(
        sectionTitle = "SERVICES & NOTIFICATIONS",
        items = listOf(
            SettingsItem(
                id = "scrobbling",
                icon = R.drawable.ic_sync,
                title = "Scrobbling & Sync",
                subtitle = "ListenBrainz and Last.fm scrobble integration",
                keywords = "listenbrainz lastfm scrobble track history sync token",
                onClick = { onNavigateToCategory("scrobbling") },
            ),
            SettingsItem(
                id = "notifications",
                icon = R.drawable.ic_notification_play,
                title = "Media Notifications",
                subtitle = "System status bar controls and lock-screen display",
                keywords = "notification lockscreen controls status bar media playback",
                onClick = { onNavigateToCategory("notifications") },
            ),
            SettingsItem(
                id = "backup_restore",
                icon = R.drawable.ic_storage,
                title = "Backup & Restore",
                subtitle = "Export or restore library data and playlists archive",
                keywords = "export import backup restore json archive migrate",
                onClick = { onNavigateToCategory("backup_restore") },
            ),
        ),
    ),
    SettingsSection(
        sectionTitle = "APPLICATION & SYSTEM",
        items = listOf(
            SettingsItem(
                id = "updates",
                icon = R.drawable.ic_download,
                title = "Software Updates",
                subtitle = "Check for new releases, changelogs, and features",
                keywords = "update release changelog version check upgrade",
                onClick = { onNavigateToCategory("updates") },
            ),
            SettingsItem(
                id = "diagnostics",
                icon = R.drawable.ic_insights,
                title = "Diagnostics & Logs",
                subtitle = "Crash logs, network traces, and engine status",
                keywords = "debug logs crash report diagnostics engine errors",
                onClick = { onNavigateToCategory("diagnostics") },
            ),
            SettingsItem(
                id = "about",
                icon = R.drawable.ic_info,
                title = "About OmniTune",
                subtitle = "Version, contributors, open-source licenses",
                keywords = "about version licenses credits github source terms",
                onClick = { onNavigateToCategory("about") },
            ),
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
    var searchQuery by remember { mutableStateOf("") }
    val allSections = remember(onNavigateToCategory) { buildSettingsSections(onNavigateToCategory) }

    val filteredSections = remember(searchQuery, allSections) {
        if (searchQuery.isBlank()) {
            allSections
        } else {
            val q = searchQuery.trim().lowercase()
            allSections.mapNotNull { section ->
                val matchingItems = section.items.filter { item ->
                    item.title.lowercase().contains(q) ||
                        item.subtitle.lowercase().contains(q) ||
                        item.keywords.lowercase().contains(q)
                }
                if (matchingItems.isNotEmpty()) {
                    section.copy(items = matchingItems)
                } else null
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase),
        contentPadding = PaddingValues(
            start = OmniSpacing.screenHorizontalCompact,
            end = OmniSpacing.screenHorizontalCompact,
            bottom = 120.dp,
        ),
    ) {
        item { Spacer(Modifier.statusBarsPadding()) }

        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = OmniSpacing.small, bottom = OmniSpacing.compact),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = OmniColors.TextPrimary,
                    )
                    Text(
                        text = "Audio engine & application preferences",
                        style = OmniTextStyles.metadata,
                        color = OmniColors.TextSecondary,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(OmniShapes.Pill)
                        .background(OmniColors.SurfacePanel)
                        .border(1.dp, OmniColors.BorderSubtle, OmniShapes.Pill),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = null,
                        tint = OmniColors.OmniAccentPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // Search settings field
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = OmniSpacing.small)
                    .height(44.dp)
                    .clip(OmniShapes.Medium)
                    .background(OmniColors.SurfacePanel)
                    .border(1.dp, OmniColors.BorderSubtle, OmniShapes.Medium),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        tint = OmniColors.TextTertiary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        textStyle = OmniTextStyles.songTitle.copy(
                            fontSize = 14.sp,
                            color = OmniColors.TextPrimary,
                        ),
                        cursorBrush = SolidColor(OmniColors.OmniAccentPrimary),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search settings (quality, crossfade, cache...)",
                                    style = OmniTextStyles.metadata.copy(fontSize = 13.sp),
                                    color = OmniColors.TextTertiary,
                                )
                            }
                            innerTextField()
                        },
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Clear",
                            tint = OmniColors.TextTertiary,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(OmniShapes.Pill)
                                .clickable { searchQuery = "" },
                        )
                    }
                }
            }
        }

        // Sections
        filteredSections.forEach { section ->
            item {
                Text(
                    text = section.sectionTitle,
                    style = OmniTextStyles.eyebrow.copy(
                        fontSize = 11.sp,
                        letterSpacing = 1.3.sp,
                        color = OmniColors.OmniAccentPrimary,
                    ),
                    modifier = Modifier.padding(top = 18.dp, bottom = 8.dp, start = 4.dp),
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(OmniShapes.Medium)
                        .background(OmniColors.SurfacePanel)
                        .border(1.dp, OmniColors.BorderSubtle, OmniShapes.Medium),
                ) {
                    section.items.forEachIndexed { index, item ->
                        SettingsRowItem(
                            item = item,
                            showDivider = index < section.items.size - 1,
                        )
                    }
                }
            }
        }

        if (filteredSections.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No matching settings found",
                        style = OmniTextStyles.metadata,
                        color = OmniColors.TextTertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRowItem(
    item: SettingsItem,
    showDivider: Boolean,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = item.onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(OmniShapes.Small)
                    .background(OmniColors.SurfaceRaised),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(item.icon),
                    contentDescription = null,
                    tint = OmniColors.OmniAccentPrimary,
                    modifier = Modifier.size(17.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = OmniTextStyles.songTitle.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = OmniColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = item.subtitle,
                    style = OmniTextStyles.metadata.copy(
                        fontSize = 11.sp,
                        color = OmniColors.TextSecondary,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = null,
                tint = OmniColors.TextTertiary.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(15.dp)
                    .graphicsLayer { rotationZ = 180f },
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 58.dp, end = 14.dp),
                thickness = 0.5.dp,
                color = OmniColors.BorderSubtle.copy(alpha = 0.5f),
            )
        }
    }
}
