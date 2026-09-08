/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 *
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.screens.settings.about

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun AboutTechStackSection() {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    AboutSectionTitle("Built With")
    AboutCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        TechListItem("Language", "Kotlin")
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = dividerColor)
        TechListItem("UI Framework", "Jetpack Compose")
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = dividerColor)
        TechListItem("Audio Engine", "Media3 ExoPlayer")
    }
    Spacer(modifier = Modifier.height(24.dp))
}
