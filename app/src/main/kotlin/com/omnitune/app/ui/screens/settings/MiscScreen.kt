/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnitune.app.viewmodels.SettingsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiscScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onLyricsProvidersClick: () -> Unit = {},
    onPoTokenClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced & Miscellaneous", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "LYRICS ENGINES",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                SettingsCard {
                    SettingsNavRow(
                        title = "Lyrics Providers",
                        subtitle = "Configure LRCLIB, BetterLyrics, and KuGou order",
                        icon = Icons.Default.Lyrics,
                        onClick = onLyricsProvidersClick
                    )
                }
            }

            item {
                Text(
                    text = "YOUTUBE ACCESS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                SettingsCard {
                    SettingsNavRow(
                        title = "YouTube PO Token",
                        subtitle = "Manage visitor data and Web playback tokens",
                        icon = Icons.Default.Key,
                        onClick = onPoTokenClick
                    )
                }
            }

            item {
                Text(
                    text = "SYSTEM & POWER",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                SettingsCard {
                    SettingsSwitchRow(
                        title = "Keep Screen On",
                        subtitle = "Prevents device display from sleeping while in player",
                        icon = Icons.Default.Smartphone,
                        checked = uiState.keepScreenOn,
                        onCheckedChange = viewModel::setKeepScreenOn
                    )
                    SettingsSwitchRow(
                        title = "Stop Music on Task Clear",
                        subtitle = "Terminate audio playback service when app is swiped away",
                        icon = Icons.Default.Close,
                        checked = uiState.stopMusicOnTaskClear,
                        onCheckedChange = viewModel::setStopMusicOnTaskClear
                    )
                }
            }
        }
    }
}
