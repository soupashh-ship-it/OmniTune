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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnitune.app.ui.component.BetaBadge
import com.omnitune.app.ui.component.SettingsCard
import com.omnitune.app.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var geminiValue by remember(uiState.geminiSecret) { mutableStateOf(uiState.geminiSecret) }
    var openaiValue by remember(uiState.openaiSecret) { mutableStateOf(uiState.openaiSecret) }
    var anthropicValue by remember(uiState.anthropicSecret) { mutableStateOf(uiState.anthropicSecret) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("AI Assistant", fontWeight = FontWeight.Bold)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Provider Selection
            item {
                Text(
                    text = "ACTIVE AI PROVIDER",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                SettingsCard {
                    listOf("gemini" to "Google Gemini (Recommended)", "openai" to "OpenAI (GPT-4o)", "anthropic" to "Anthropic (Claude 3.5)").forEach { (provider, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setSelectedAiProvider(provider) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = uiState.selectedAiProvider == provider, onClick = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Google Gemini API Config
            item {
                Text(
                    text = "GOOGLE GEMINI CONFIGURATION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SecretSettingsField(
                            value = geminiValue,
                            persistedValue = uiState.geminiSecret,
                            label = "Gemini API Key",
                            onValueChange = { geminiValue = it },
                            onSave = viewModel::setGeminiSecret,
                            onClear = {
                                geminiValue = ""
                                viewModel.setGeminiSecret("")
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Model: ${uiState.geminiModel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // OpenAI API Config
            item {
                Text(
                    text = "OPENAI CONFIGURATION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SecretSettingsField(
                            value = openaiValue,
                            persistedValue = uiState.openaiSecret,
                            label = "OpenAI API Key",
                            onValueChange = { openaiValue = it },
                            onSave = viewModel::setOpenaiSecret,
                            onClear = {
                                openaiValue = ""
                                viewModel.setOpenaiSecret("")
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Model: ${uiState.openaiModel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Anthropic API Config
            item {
                Text(
                    text = "ANTHROPIC CONFIGURATION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SecretSettingsField(
                            value = anthropicValue,
                            persistedValue = uiState.anthropicSecret,
                            label = "Anthropic API Key",
                            onValueChange = { anthropicValue = it },
                            onSave = viewModel::setAnthropicSecret,
                            onClear = {
                                anthropicValue = ""
                                viewModel.setAnthropicSecret("")
                            },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Model: ${uiState.anthropicModel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
