/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnitune.app.constants.AudioQuality
import com.omnitune.app.viewmodels.SettingsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onEqualizerClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showWifiQualitySheet by remember { mutableStateOf(false) }
    var showMobileQualitySheet by remember { mutableStateOf(false) }
    var showDownloadQualitySheet by remember { mutableStateOf(false) }
    var showDoubleTapSeekSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playback & Audio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Audio Quality
            item {
                Text(
                    text = "AUDIO QUALITY",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                SettingsCard {
                    SettingsNavRow(
                        title = "Wi-Fi Audio Quality",
                        subtitle = uiState.wifiAudioQuality.name.lowercase().replaceFirstChar { it.uppercase() },
                        icon = Icons.Default.Wifi,
                        onClick = { showWifiQualitySheet = true }
                    )
                    SettingsNavRow(
                        title = "Mobile Data Quality",
                        subtitle = uiState.mobileAudioQuality.name.lowercase().replaceFirstChar { it.uppercase() },
                        icon = Icons.Default.SignalCellularAlt,
                        onClick = { showMobileQualitySheet = true }
                    )
                    SettingsNavRow(
                        title = "Download Quality",
                        subtitle = uiState.downloadQuality.name.lowercase().replaceFirstChar { it.uppercase() },
                        icon = Icons.Default.Download,
                        onClick = { showDownloadQualitySheet = true }
                    )
                }
            }

            // Audio Engine & Transitions
            item {
                Text(
                    text = "ENGINE & TRANSITIONS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                SettingsCard {
                    SettingsSwitchRow(
                        title = "Gapless Playback",
                        subtitle = "Continuous transition without silent gaps between tracks",
                        icon = Icons.Default.GraphicEq,
                        checked = uiState.gaplessPlaybackEnabled,
                        onCheckedChange = viewModel::setGaplessPlaybackEnabled
                    )
                    SettingsSwitchRow(
                        title = "Automix",
                        subtitle = "Smoothly blend transitions between queue songs",
                        icon = Icons.Default.Shuffle,
                        checked = uiState.automixEnabled,
                        onCheckedChange = viewModel::setAutomixEnabled
                    )
                    SettingsSwitchRow(
                        title = "Audio Normalization",
                        subtitle = "Maintain consistent loudness across all tracks",
                        icon = Icons.Default.VolumeUp,
                        checked = uiState.volumeNormalizationEnabled,
                        onCheckedChange = viewModel::setVolumeNormalizationEnabled
                    )
                    SettingsSwitchRow(
                        title = "Next Song Preloading",
                        subtitle = "Preload incoming track for instant instant gapless start",
                        icon = Icons.Default.Speed,
                        checked = uiState.nextSongPreloadingEnabled,
                        onCheckedChange = viewModel::setNextSongPreloadingEnabled
                    )
                    SettingsNavRow(
                        title = "Equalizer",
                        subtitle = "Tune frequencies with graphic 10-band EQ",
                        icon = Icons.Default.Tune,
                        onClick = onEqualizerClick
                    )
                }
            }

            // Crossfade Slider
            item {
                Text(
                    text = "CROSSFADE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                SettingsCard {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Crossfade Duration", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = if (uiState.crossfadeMs == 0) "Off" else "${uiState.crossfadeMs / 1000}s",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = (uiState.crossfadeMs / 1000).toFloat(),
                            onValueChange = { viewModel.setCrossfadeMs((it * 1000).toInt()) },
                            valueRange = 0f..12f,
                            steps = 11,
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            // Gestures & Seek
            item {
                Text(
                    text = "GESTURES & CONTROLS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                SettingsCard {
                    SettingsNavRow(
                        title = "Double Tap Seek Duration",
                        subtitle = "${uiState.doubleTapSeekSeconds} seconds",
                        icon = Icons.Default.FastForward,
                        onClick = { showDoubleTapSeekSheet = true }
                    )
                    SettingsSwitchRow(
                        title = "Audio Offload",
                        subtitle = "Offload audio processing to DSP to save battery",
                        icon = Icons.Default.BatteryChargingFull,
                        checked = uiState.audioOffloadEnabled,
                        onCheckedChange = viewModel::setAudioOffloadEnabled
                    )
                }
            }
        }
    }

    if (showWifiQualitySheet) {
        ModalBottomSheet(onDismissRequest = { showWifiQualitySheet = false }) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Wi-Fi Audio Quality", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                AudioQuality.entries.forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setWifiAudioQuality(quality)
                                showWifiQualitySheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = uiState.wifiAudioQuality == quality, onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(quality.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }
        }
    }

    if (showMobileQualitySheet) {
        ModalBottomSheet(onDismissRequest = { showMobileQualitySheet = false }) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Mobile Data Audio Quality", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                AudioQuality.entries.forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setMobileAudioQuality(quality)
                                showMobileQualitySheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = uiState.mobileAudioQuality == quality, onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(quality.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }
        }
    }

    if (showDownloadQualitySheet) {
        ModalBottomSheet(onDismissRequest = { showDownloadQualitySheet = false }) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Download Audio Quality", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                AudioQuality.entries.forEach { quality ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setDownloadQuality(quality)
                                showDownloadQualitySheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = uiState.downloadQuality == quality, onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(quality.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }
        }
    }

    if (showDoubleTapSeekSheet) {
        ModalBottomSheet(onDismissRequest = { showDoubleTapSeekSheet = false }) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Double Tap Seek Duration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                listOf(5, 10, 15, 30, 60).forEach { seconds ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setDoubleTapSeekSeconds(seconds)
                                showDoubleTapSeekSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = uiState.doubleTapSeekSeconds == seconds, onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("$seconds seconds")
                    }
                }
            }
        }
    }
}
