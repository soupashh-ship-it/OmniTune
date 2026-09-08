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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omnitune.app.ui.screens.settings.OmniTuneRepositoryUrl

@Composable
internal fun AboutInformationSection(
    onOpenUri: (String) -> Unit,
    onHowItWorksClick: () -> Unit,
    onCreditsClick: () -> Unit,
    onChangelogClick: () -> Unit,
    onSupportClick: () -> Unit,
) {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)

    AboutSectionTitle("Information")
    AboutCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        AboutLinkItem(
            icon = Icons.Default.Code,
            title = "Source Code",
            subtitle = "Open OmniTune on GitHub",
            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
            onClick = { onOpenUri(OmniTuneRepositoryUrl) },
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = dividerColor)

        AboutLinkItem(
            icon = Icons.Default.Person,
            title = "Credits & Open Source",
            subtitle = "Developers, contributors and libraries",
            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            onClick = onCreditsClick,
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = dividerColor)

        AboutLinkItem(
            icon = Icons.Default.History,
            title = "Changelog",
            subtitle = "See what's new in this release",
            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            onClick = onChangelogClick,
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = dividerColor)

        AboutLinkItem(
            icon = Icons.Default.Lightbulb,
            title = "How It Works",
            subtitle = "Learn how OmniTune works with YouTube Music",
            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            onClick = onHowItWorksClick,
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = dividerColor)

        AboutLinkItem(
            icon = Icons.Default.Favorite,
            title = "Support the Project",
            subtitle = "Help development and maintenance",
            trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            onClick = onSupportClick,
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
}
