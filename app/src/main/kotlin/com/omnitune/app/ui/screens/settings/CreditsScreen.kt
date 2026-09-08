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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Credits & Open Source", fontWeight = FontWeight.Bold) },
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
                    text = "PROJECT INSPIRATION & CODEBASES",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                SettingsCard {
                    AboutDestinations.inspiration.forEach { project ->
                        SettingsNavRow(
                            title = project.title,
                            subtitle = project.subtitle,
                            icon = if (project.title == "SuvMusic") Icons.Default.Favorite else Icons.Default.Code,
                            onClick = { context.openExternalUrl(project.url) }
                        )
                    }
                    SettingsNavRow(
                        title = "NewPipe Extractor",
                        subtitle = "Open source community media extraction library",
                        icon = Icons.Default.Code,
                        onClick = { context.openExternalUrl(NewPipeExtractorUrl) }
                    )
                }
            }

            item {
                Text(
                    text = "CORE OPEN SOURCE LIBRARIES",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                SettingsCard {
                    SettingsNavRow(
                        title = "Jetpack Compose & Material 3",
                        subtitle = "Modern declarative UI toolkit for Android & desktop",
                        icon = Icons.Default.Code,
                        onClick = { context.openExternalUrl("https://developer.android.com/compose") }
                    )
                    SettingsNavRow(
                        title = "Media3 ExoPlayer",
                        subtitle = "High performance media playback engine by Google",
                        icon = Icons.Default.Code,
                        onClick = { context.openExternalUrl("https://developer.android.com/media/media3") }
                    )
                    SettingsNavRow(
                        title = "Coil 3",
                        subtitle = "Asynchronous image loading for Compose",
                        icon = Icons.Default.Code,
                        onClick = { context.openExternalUrl("https://coil-kt.github.io/coil/") }
                    )
                    SettingsNavRow(
                        title = "Ktor Client & Kotlinx Serialization",
                        subtitle = "Asynchronous HTTP client and JSON parsing",
                        icon = Icons.Default.Code,
                        onClick = { context.openExternalUrl("https://ktor.io/") }
                    )
                }
            }
        }
    }
}
