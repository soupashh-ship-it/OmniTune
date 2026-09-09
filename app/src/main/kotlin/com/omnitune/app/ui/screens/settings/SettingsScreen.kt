/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 *
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.omnitune.app.ui.component.BetaBadge
import com.omnitune.app.ui.component.LeadingIconBox
import com.omnitune.app.ui.theme.SquircleShape
import com.omnitune.app.viewmodels.SettingsViewModel


private data class SettingsSearchEntry(
    val title: String,
    val subtitle: String,
    val keywords: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

/**
 * Settings screen with Material 3 Expressive design and organized categories.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onLoginClick: () -> Unit = {},
    onPlaybackClick: () -> Unit = {},
    onAppearanceClick: () -> Unit = {},
    onCustomizationClick: () -> Unit = {},
    onStorageClick: () -> Unit = {},
    onStatsClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onMiscClick: () -> Unit = {},
    onSponsorBlockClick: () -> Unit = {},
    onCreditsClick: () -> Unit = {},
    onLastFmClick: () -> Unit = {},
    onAISettingsClick: () -> Unit = {},
    onUpdaterClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var settingsQuery by remember { mutableStateOf("") }

    val searchIndex = remember {
        listOf(
            SettingsSearchEntry("Appearance", "Theme, dark mode, colors, liquid glass", "theme dark mode light colors dynamic material amoled gradient glass", Icons.Default.DarkMode, onAppearanceClick),
            SettingsSearchEntry("Playback", "Audio quality, equalizer, crossfade", "audio quality bitrate equalizer eq crossfade normalization loudness preloading offload", Icons.Default.GraphicEq, onPlaybackClick),
            SettingsSearchEntry("Customization", "Player UI, artwork shape/size, seekbar style", "player ui artwork shape size seekbar style mini player vinyl glass", Icons.Default.Tune, onCustomizationClick),
            SettingsSearchEntry("SponsorBlock", "Skip non-music segments", "sponsorblock skip segments intro outro sponsor", Icons.Default.FastForward, onSponsorBlockClick),
            SettingsSearchEntry("Scrobbling", "ListenBrainz & track history", "listenbrainz scrobble scrobbling history", Icons.Default.MusicNote, onLastFmClick),
            SettingsSearchEntry("Advanced", "Diagnostics, experimental & lyrics order", "advanced misc diagnostics experimental logs lyrics", Icons.Default.Settings, onMiscClick),
            SettingsSearchEntry("Storage Manager", "Manage downloads & cache", "storage downloads cache clear space data", Icons.Default.Storage, onStorageClick),
            SettingsSearchEntry("Listening Insights", "Your listening stats & habits", "stats statistics listening history wrapped activity", Icons.Default.Info, onStatsClick),
            SettingsSearchEntry("Support the project", "Donate & sponsor development", "support donate sponsor project", Icons.Default.Favorite, onSupportClick),
            SettingsSearchEntry("Credits", "Developers & open-source libraries", "credits developers libraries licenses", Icons.Default.Person, onCreditsClick),
            SettingsSearchEntry("About OmniTune", "Version & application details", "about version app info changelog", Icons.Default.Album, onAboutClick),
            SettingsSearchEntry("Check for Updates", "App updates and release notes", "update updates ota check changelog", Icons.Default.SystemUpdate, onUpdaterClick)
        )
    }

    val filteredEntries = remember(settingsQuery) {
        if (settingsQuery.isBlank()) emptyList()
        else {
            val q = settingsQuery.lowercase().trim()
            searchIndex.filter { it.title.lowercase().contains(q) || it.subtitle.lowercase().contains(q) || it.keywords.contains(q) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar
            item {
                OutlinedTextField(
                    value = settingsQuery,
                    onValueChange = { settingsQuery = it },
                    placeholder = { Text("Search settings, features...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (settingsQuery.isNotEmpty()) {
                            IconButton(onClick = { settingsQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = SquircleShape,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (settingsQuery.isNotBlank()) {
                if (filteredEntries.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No settings found matching \"$settingsQuery\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(filteredEntries) { entry ->
                        SettingsCard {
                            SettingsNavRow(
                                title = entry.title,
                                subtitle = entry.subtitle,
                                icon = entry.icon,
                                onClick = entry.onClick
                            )
                        }
                    }
                }
            } else {
                // Account / Auth Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onLoginClick),
                        shape = SquircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.isLoggedIn && uiState.userAvatarUrl != null) {
                                    AsyncImage(
                                        model = uiState.userAvatarUrl,
                                        contentDescription = "User Avatar",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (uiState.isLoggedIn) Icons.Default.Person else Icons.AutoMirrored.Filled.Login,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (uiState.isLoggedIn) (uiState.userName ?: "Logged In") else "Sign in to YouTube",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (uiState.isLoggedIn) "Sync playlists & library" else "Access your saved playlists & recommendations",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Audio & Playback Group
                item {
                    Text(
                        text = "AUDIO & PLAYBACK",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    SettingsCard {
                        SettingsNavRow(
                            title = "Playback Engine",
                            subtitle = "Bitrate quality, crossfade, normalization",
                            icon = Icons.Default.GraphicEq,
                            onClick = onPlaybackClick
                        )
                        SettingsNavRow(
                            title = "Player Customization",
                            subtitle = "Artwork shape, size, miniplayer & seekbar style",
                            icon = Icons.Default.Tune,
                            onClick = onCustomizationClick
                        )
                        SettingsNavRow(
                            title = "SponsorBlock",
                            subtitle = "Skip non-music segments & sponsored intros",
                            icon = Icons.Default.FastForward,
                            onClick = onSponsorBlockClick
                        )
                        SettingsNavRow(
                            title = "Scrobbling",
                            subtitle = "Sync listens with ListenBrainz",
                            icon = Icons.Default.MusicNote,
                            onClick = onLastFmClick
                        )
                    }
                }

                // Personalization & Appearance Group
                item {
                    Text(
                        text = "APPEARANCE & THEME",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    SettingsCard {
                        SettingsNavRow(
                            title = "Appearance",
                            subtitle = "Dark mode, palettes, liquid glass & app logos",
                            icon = Icons.Default.DarkMode,
                            onClick = onAppearanceClick
                        )
                        SettingsNavRow(
                            title = "Advanced & Miscellaneous",
                            subtitle = "Lyrics providers, background playback & power",
                            icon = Icons.Default.Settings,
                            onClick = onMiscClick
                        )
                    }
                }

                // Storage & Activity Group
                item {
                    Text(
                        text = "DATA & STORAGE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    SettingsCard {
                        SettingsNavRow(
                            title = "Storage Manager",
                            subtitle = "Manage audio cache and offline downloads",
                            icon = Icons.Default.Storage,
                            onClick = onStorageClick
                        )
                        SettingsNavRow(
                            title = "Listening Insights",
                            subtitle = "View listening stats, trends & personality",
                            icon = Icons.Default.Timeline,
                            onClick = onStatsClick
                        )
                    }
                }

                // About & Support Group
                item {
                    Text(
                        text = "ABOUT & PROJECT",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    SettingsCard {
                        SettingsNavRow(
                            title = "Support the Project",
                            subtitle = "Sponsor OmniTune and help ongoing development",
                            icon = Icons.Default.Favorite,
                            onClick = onSupportClick
                        )
                        SettingsNavRow(
                            title = "Credits & Open Source",
                            subtitle = "Developers, contributors and libraries",
                            icon = Icons.Default.Person,
                            onClick = onCreditsClick
                        )
                        SettingsNavRow(
                            title = "About OmniTune",
                            subtitle = "Version details and license information",
                            icon = Icons.Default.Album,
                            onClick = onAboutClick
                        )
                    }
                }
            }
        }
    }
}
