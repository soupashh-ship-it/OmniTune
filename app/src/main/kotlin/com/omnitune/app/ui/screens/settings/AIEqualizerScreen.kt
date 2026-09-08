/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omnitune.app.constants.AIEqualizerAutoModeKey
import com.omnitune.app.constants.AIEqualizerPromptKey
import com.omnitune.app.constants.EqualizerBandLevelsMbKey
import com.omnitune.app.constants.EqualizerEnabledKey
import com.omnitune.app.constants.EqualizerSelectedProfileIdKey
import com.omnitune.app.playback.createAiEqualizerBands
import com.omnitune.app.playback.describeAiEqualizerProfile
import com.omnitune.app.playback.encodeEqualizerBands
import com.omnitune.app.ui.component.BetaBadge
import com.omnitune.app.ui.component.SettingsCard
import com.omnitune.app.ui.theme.SquircleShape
import com.omnitune.app.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIEqualizerScreen(
    onBack: () -> Unit,
) {
    var prompt by rememberPreference(AIEqualizerPromptKey, "")
    var autoEqEnabled by rememberPreference(AIEqualizerAutoModeKey, false)
    var equalizerEnabled by rememberPreference(EqualizerEnabledKey, false)
    var storedBands by rememberPreference(EqualizerBandLevelsMbKey, "")
    var selectedProfileId by rememberPreference(EqualizerSelectedProfileIdKey, "")
    var generatedStatus by rememberSaveable { mutableStateOf<String?>(null) }

    fun applyPromptProfile() {
        val profile = createAiEqualizerBands(prompt)
        storedBands = encodeEqualizerBands(profile)
        selectedProfileId = "AI_GENERATED"
        equalizerEnabled = true
        generatedStatus = "${describeAiEqualizerProfile(prompt)} profile applied"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Neural Equalizer", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        BetaBadge()
                    }
                },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto Mode (Track Reactive)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Re-applies this prompt profile against the current track metadata during playback",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoEqEnabled,
                        onCheckedChange = { enabled ->
                            autoEqEnabled = enabled
                            if (enabled && prompt.isNotBlank()) {
                                applyPromptProfile()
                            }
                        }
                    )
                }
            }

            Text(
                text = "DESCRIBE YOUR DESIRED SOUND",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("e.g., 'warm vintage tube sound with punchy sub-bass'") },
                minLines = 3,
                maxLines = 5,
                shape = SquircleShape,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { applyPromptProfile() },
                shape = SquircleShape,
                modifier = Modifier.fillMaxWidth(),
                enabled = prompt.isNotBlank()
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate AI EQ Profile", fontWeight = FontWeight.Bold)
            }

            if (generatedStatus != null || selectedProfileId == "AI_GENERATED" || storedBands.isNotBlank()) {
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = generatedStatus ?: "Generated profile active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (autoEqEnabled) {
                                "Auto Mode is updating the 10-band equalizer from this prompt as tracks change."
                            } else {
                                "The generated 10-band profile is saved and enabled for playback."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
